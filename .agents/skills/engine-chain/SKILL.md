---
name: engine-chain
description: Engine-side full-chain development guide for adding a new business command or gRPC API across client SDK, gateway, broker, engine processors, protocol records and repository storage. Use when adding any new engine-side command/API (new XxxCommand, new gRPC service, new Value API chain), or when onboarding to how a request travels from client-java through gateway-grpc and broker into the partition engine and RocksDB.
---

# 引擎侧全链路开发规范（engine-chain）

## 技能定位

- **核心定位**：引擎侧**新增功能**（新命令 / 新 gRPC API / 新 Value 域）的全链路开发地图——client → gateway → broker → 引擎 → 存储 六层各改什么、按什么顺序改、哪些注册点漏一个就跑不通
- **与 iteration 的边界**：`iteration` 管**改已有功能**（影响排查 + 顺序编排）；本技能管**加新链路**（分层实现清单 + 注册点核对）。两者共用 record/enum/repository-standards 与 structpack 的细则，本技能只引用不重复
- **活样例**：2026.9 新增的「表达式求值链」（`ExpressionService`）贯穿全链，[07-example-expression.md](./07-example-expression.md) 有逐层文件清单，可当模板抄

## 链路总览（一条命令的一生）

```
client-java                    gateway                        broker                          引擎(bpmn-engine)               存储
──────────                     ───────                        ──────                          ────────────────                ────
XxxCommand.send() ──gRPC──▶ GrpcXxxServiceImpl ──broker-client──▶ CommandApiHandleImpl
                              (proto→broker请求)    EvaluateExpressionRequest          │ 分配 requestId，挂 future
                                                                   │ valueMapper.getValue(lifeCycle) 反序列化 ★没注册直接挂
                                                                   │ RecordAppendEntryFactory
                                                                   ▼
                                                            eventlog.tryAppend(USER_COMMAND)
                                                                   │ Raft 复制 → commit → 重放
                                                                   ▼
                                                            EngineProcessService.processEvent（分区事务）
                                                                   │ 按 processIndex 分发
                                                                   ▼
                                                            XxxApiProcessor.processRecord
                              ◀──ApiResponseWriter(SBE)── CommandApiHandle.sendResponse
XxxCommandResponse ◀─gRPC── responseObserver.onNext          │
                              (requestId 关联回程)             ├─ 直答型：adResponse 直接回写（不落日志）
                                                                   └─ 持久化型：addCommand/addEvent → 状态机推进
                                                                        │ addEvent 同时 addState
                                                                        ▼
                                                                   RocksdbBusinessRepositoryApplier
                                                                        │ Entity.wrap(record)
                                                                        ▼
                                                                   KvStore → RocksDB
```

## 六层职责表（快速索引）

| 层 | 模块 | 关键类 | 干什么 | 文档 |
|----|------|--------|--------|------|
| ① 客户端 | `clients/client-java` | `XxxCommand(+Impl)`、`KunpengClient(Impl)`、`XxxCommandResponse(Impl)` | 命令构建器 + gRPC async stub + 可重试 Future | [01-client.md](./01-client.md) |
| ② 网关 | `gateway-protocol`（proto）、`gateway-grpc` | `xxx_service.proto`、`GrpcXxxServiceImpl`、`GatewayGrpcService` | proto 契约 + gRPC 服务 → broker 请求转换 | [02-gateway.md](./02-gateway.md) |
| ③ 协议 | `protocol-business(-impl)` | `ValueType`、`CommandApiXxxValueLifeCycle`、`Xxx{Request,Response}Record`、`XxxApiRecordRegister`、`ValueLifeCycle` | Record 定义 + 编号池 + 反序列化注册（**全链的契约层**） | [03-protocol.md](./03-protocol.md) |
| ④ Broker | `broker-client`（请求侧）、`broker`（入口/回程） | `XxxRequest extends CommandApiBrokerRequest`、`CommandApiHandleImpl`、`BrokerClientImpl` | 分区路由编码 + 命令入 eventlog + requestId 响应回程 | [04-broker.md](./04-broker.md) |
| ⑤ 引擎 | `engine/bpmn-engine` | `XxxApiProcessor`、`XxxApiProcessorRegister`、`ApiCommandProcessorRegister`、`LogEventWriter`、`EngineProcessService` | 命令处理 + 状态机推进 + 日志写入/状态应用 | [05-engine.md](./05-engine.md) |
| ⑥ 存储 | `repository/business-repository(+kvstore+rocksdb)` | `RocksdbBusinessRepositoryApplier`、`RepositoryXxxApplierRegister`、`XxxEntity` | EVENT Record → Entity → RocksDB 落地 | [06-storage.md](./06-storage.md) |

## 各端职责与边界（谁负责什么、谁绝不碰什么）

| 端 | 核心职责（owns） | 输入 → 输出 | 明确不做（边界） |
|----|------------------|-------------|------------------|
| **① 客户端** SDK 门面 | 命令 API 形态（fluent 接口/步骤分阶段）；必填参数前置校验（`ArgumentUtil`）；变量 JSON 序列化；gRPC 发送（超时 deadline、按 StatusCode 可重试）；channel/stub 装配、凭据、负载均衡与网关发现 | 业务方法调用 → `KunpengFuture<XxxCommandResponse>` | 不做业务规则校验（引擎判）；不决定分区路由（只透传 scopeKey 等路由参数）；不解释响应业务语义 |
| **② 网关** 无状态转发 | 对外协议契约（proto 字段、rpc 错误码语义）；proto ⇄ broker 请求/响应双向转换（含 Document↔JSON）；路由参数语义落地（scopeKey>0 才透传）；错误统一出口（`GrpcErrorHandle`→gRPC Status）；服务生命周期与健康上报 | gRPC 请求 → broker 请求（异步 future）→ gRPC 响应 | 不含业务逻辑与校验规则；无任何状态；不直连引擎/存储；不做重试决策（透传 broker-client 能力） |
| **③ 协议** 契约唯一源 | 类型身份（`ValueType` 码）与状态身份（`CommandApiXxxValueLifeCycle` 的 value/processIndex/recordIndex）；Record 载体定义（structpack key-id）；**反序列化注册**（`RecordValueMapper`，broker 入口与响应解码的依赖）；编号池与 `size()` 维护；`INTENT_CLASSES`/`fromProtocolValue` 全局接入 | 无运行时输入输出——纯身份定义，其他层引用它 | 无任何业务逻辑；不定义传输方式；字段/编号变更=协议变更（先走 enum-standards/structpack 评估） |
| **④ Broker** 命令通道 | **broker-client（网关侧库）**：请求封装 + 分区路由策略（随机/按 key/指定）；统一帧编码；leader 寻址；messaging 收发与超时重试。**broker 本体（CommandApiHandle）**：requestId 生成与请求挂账；Record 反序列化；命令追加 eventlog（USER_COMMAND）→ Raft 复制；按 requestId 回程响应（SBE 编码）；背板保护（磁盘满拒流、丢 leader 注销） | 统一请求帧 → 分区日志条目；引擎响应 → 统一响应帧 | 不理解业务语义（只认 Record 不认业务）；不推进状态机；不落业务状态（日志不算状态，重放才落） |
| **⑤ 引擎** 业务大脑 | 命令处理与业务规则校验（读 immutable 仓储）；模式执行（直答 `adResponse` / 持久化 `addCommand`+`addEvent`）；key 发号（`nextCurrentSourceKey` 等）；域状态机推进、跨分区命令、定时器与 BPMN 行为（`behavior()`）；分区事务（重放→处理→响应→日志→commit→副作用）；处理器幂等（必然重放） | `BusinessLogRecord` → 响应 Record / 新日志条目 / 状态应用 | 不直接写存储（必须 `addEvent`→applier）；不管请求-响应关联（requestId 只透传）；不做网络出口（job 推送走 `addSideEffect` + 端口注入） |
| **⑥ 存储** 派生状态 | EVENT→Entity 落地（applier 按 EVENT/valueType/valueState 三元组分发）；读仓储族（`Immutable*` 供引擎查询）；key 发号仓储；kvstore→RocksDB 列族布局与持久化；事务内写入；崩溃后由日志重放重建（Entity 是纯派生数据） | Record(直答型不进来) → RocksDB 键值 | 不做业务判断；不被网关/客户端直接触达（只有引擎经 applier/immutable 仓储访问）；不产生日志（只消费 EVENT） |

> 一句话记住分工：**客户端管好用，网关管翻译，协议管身份，broker 管投递，引擎管决策，存储管落地**。写代码时如果发现某端在做下一段的事（比如网关里写校验、引擎里直连存储），就是越界了。

## 两种命令模式（动手前先选型）

| | 直答型（查询/求值类） | 持久化型（状态变更类） |
|---|---|---|
| 例子 | 表达式求值、拓扑查询 | 创建流程实例、完成用户任务 |
| 处理器做的事 | 校验 → `writer.adResponse(lifeCycle, requestId, respRecord)` 直接回写 | 校验 → 取 key → 构造域 Record → `addEvent`/`addCommand` 写日志 |
| 落日志 | 否（不产生 Record） | 是（COMMAND/EVENT 进 eventlog → Raft） |
| 落存储 | 否 | EVENT 经 applier → Entity → RocksDB |
| 响应时机 | 处理器内同步回 | 状态机推进到可响应状态时，用 `AsyncRequestRecord` 记录的原 requestId 回写 |
| 样例 | `ExpressionEvaluateApiProcessor` | `ProcessInstanceCreateProcessor` |

> 经验法则：改流程/任务/变量状态 → 持久化型；纯计算/读取 → 直答型。直答型不占日志、不占存储，但也没有审计与重放。

## 新增命令固定改动清单（核心 checklist）

以下注册点**漏任何一个链路都不通**，自检时逐项打勾：

| # | 改动点 | 文件/位置 | 漏了的后果 |
|---|--------|-----------|-----------|
| 1 | `ValueType` 加 `XXX_API((short) N)`（顺延） | `protocol-business/.../ValueType.java` | 编译不过 / 协议解析 UNKNOWN |
| 2 | `CommandApiXxxValueLifeCycle`（REQUEST 带 `PROCESS_INDEX_N`，RESPONSE 带 `RECORD_INDEX_N`） | `protocol-business/.../record/commandapi/record/xxx/` | 无状态身份，无法注册 |
| 3 | 编号池顺延：`RecordProcessIndex`/`RecordMappingIndex` 加常量 **并同步改 `size()`** | 同上两个接口 | 数组越界 / 处理器容器容量不足 |
| 4 | `XxxRequestRecord` / `XxxResponseRecord`（structpack key-id） | `protocol-business-impl/.../record/commandapi/xxx/` | 无载体 |
| 5 | `XxxApiRecordRegister` 注册 lifecycle→Record 供应商，挂入 `CommandApiRecordRegister` | protocol-business-impl | **broker 入口 `valueMapper.getValue()` 抛异常**、响应解码 value=null |
| 6 | `ValueLifeCycle.INTENT_CLASSES` + `fromProtocolValue()` 加 case | `protocol-business/.../ValueLifeCycle.java` | 反序列化 IllegalState |
| 7 | broker-client `XxxRequest extends CommandApiBrokerRequest`（含路由） | `broker-client/.../commandapi/xxx/request/` | 网关无法构造请求 |
| 8 | 引擎 `XxxApiProcessor` + `XxxApiProcessorRegister`，挂入 `ApiCommandProcessorRegister` | `bpmn-engine/.../commandapi/xxx/` | 提交后 `Not Have LogEventProcessor registered` |
| 9 | `xxx_service.proto` + `GrpcXxxServiceImpl`，挂入 `GatewayGrpcService.createGrpcServices()` | gateway-protocol / gateway-grpc | 客户端无入口 |
| 10 | `XxxCommand(+Impl)` + `KunpengClient` 方法 + `KunpengClientImpl` stub 装配 | clients/client-java | SDK 无 API |
| 11 | （仅持久化型）域 LifeCycle 状态、`XxxEntity`+wrap/unwrap、`RepositoryXxxApplierRegister` | protocol / business-repository | EVENT 不落库、重启丢状态 |

## 实施顺序（依赖序，非链路序）

文档编号按**请求流**编排（01→06），动手按**依赖序**：

```
1. protocol（ValueType → LifeCycle → 编号池 → Record → RecordRegister → ValueLifeCycle 注册）
   编译验证：./gradlew :kunpeng:protocol:protocol-business:compileJava :kunpeng:protocol:protocol-business-impl:compileJava
2. broker-client 请求类
   编译验证：./gradlew :kunpeng:broker:broker-client:compileJava
3. 引擎处理器 + 注册器（持久化型先补：域状态/Entity/applier）
   编译验证：./gradlew :kunpeng:engine:bpmn-engine:compileJava
4. gateway proto → build 生成 stub → GrpcXxxServiceImpl → GatewayGrpcService 挂接
   编译验证：./gradlew :kunpeng:gateway:gateway-protocol:compileJava :kunpeng:gateway:gateway-grpc:compileJava
5. client SDK
   编译验证：./gradlew :clients:client-java:compileJava
6. 测试同步 + 全链路 ./gradlew buildSkipTest
```

## 契约自检（收尾必查）

- [ ] 编号池：新状态顺延编号，`RecordProcessIndex.size()`/`RecordMappingIndex.size()` 已同步（处理器数组按 size() 分配）
- [ ] `valueType()` + `valueLifeCycle()` 双标记与注册的 lifecycle 一致（`LogEventProcessors` 按 processIndex 定位数组槽位，重复注册直接抛异常）
- [ ] Record 字段 key-id 全新、不复用不改既有（structpack 强规则）
- [ ] 持久化型：Entity `wrap`/`unwrap` 成对覆盖新字段；applier 已注册进 `RocksdbBusinessRepositoryApplier.initRegister()`
- [ ] 客户端 DTO ↔ proto message ↔ broker Record 字段三方对齐
- [ ] 直答型确认确实不需要落日志（无审计诉求）；持久化型确认响应回写路径（requestId 经 `AsyncRequestRecord` 传递）
- [ ] `./gradlew buildSkipTest` 全绿；提交前根目录 `./gradlew spotlessApply`（commit-standards）

## 与其他技能协作

| 场景 | 用什么 |
|------|--------|
| Record 类怎么写（骨架/key/addXxx/嵌套） | `record-standards` + `structpack` |
| 编号池申请、LifeCycle 枚举契约 | `enum-standards` |
| Entity 与 wrap/unwrap | `repository-standards` |
| eventlog append 管线 / EL 批帧 / 恢复 | `eventlog-design` |
| 分区事务、Raft、调度 | `scheduler` / `cluster-dispatch-design` |
| Record 外发 sink | `sink-design` |
| 改已有功能（非新增） | `iteration` |
| 提交 | `commit-standards` |

## 文档索引

| 文档 | 内容 |
|------|------|
| [01-client.md](./01-client.md) | 客户端命令层：接口/Impl/Response/客户端装配 |
| [02-gateway.md](./02-gateway.md) | proto 契约与 gRPC 服务实现 |
| [03-protocol.md](./03-protocol.md) | ValueType/LifeCycle/编号池/Record/反序列化注册 |
| [04-broker.md](./04-broker.md) | broker-client 请求与路由、broker 入口、响应回程 |
| [05-engine.md](./05-engine.md) | 处理器两种模式、LogEventWriter API、分区事务循环 |
| [06-storage.md](./06-storage.md) | applier 体系、Entity、KvStore/RocksDB |
| [07-example-expression.md](./07-example-expression.md) | 表达式求值链活样例（逐层文件清单） |
