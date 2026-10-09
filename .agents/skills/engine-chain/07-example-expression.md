# 07 活样例：表达式求值链（2026.9，直答型最小实现）

一条从 SDK 到引擎的完整直答型命令，13 组改动点全部在此，可作为新命令的抄写模板。源码 commit：`feat: add engine-side expression evaluation command chain`。

## 需求背景

在引擎侧解析并求值表达式（connector 的 FEEL/QL 表达式求值走此链），返回求值结果。无状态、不落库 → 选直答型。

## 逐层文件清单

### ③ 协议层（先做）

| 文件 | 改动 |
|------|------|
| `protocol-business/.../ValueType.java` | 加 `EXPRESSION_API((short) 53)` |
| `protocol-business/.../record/commandapi/record/expression/CommandApiExpressionValueLifeCycle.java` | 新建：`EVALUATE_REQUEST((short)0, PROCESS_INDEX_227, RECORD_INDEX_85)` / `EVALUATE_RESPONSE((short)1, NOT_PROCESS_INDEX, RECORD_INDEX_86)` |
| `protocol-business/.../RecordProcessIndex.java` | 尾部加 `PROCESS_INDEX_227`，`size()` 改为 `PROCESS_INDEX_227 + 1` |
| `protocol-business/.../RecordMappingIndex.java` | 尾部加 `RECORD_INDEX_85/86`，`size()` 改为 `RECORD_INDEX_86 + 1` |
| `protocol-business/.../ValueLifeCycle.java` | `INTENT_CLASSES` 加类 + `fromProtocolValue` 加 `case EXPRESSION_API` |
| `protocol-business-impl/.../record/commandapi/expression/EvaluateExpressionRequestRecord.java` | 新建（字段：expression/variables/tenantId，key-id 自描述） |
| `protocol-business-impl/.../record/commandapi/expression/EvaluateExpressionResponseRecord.java` | 新建（字段：result Document） |
| `protocol-business-impl/.../record/commandapi/expression/ExpressionApiRecordRegister.java` | 新建：两态注册 Record 供应商 |
| `protocol-business-impl/.../record/commandapi/CommandApiRecordRegister.java` | 挂接 `ExpressionApiRecordRegister.register(valueMapper)` |

### ④ Broker 层

| 文件 | 改动 |
|------|------|
| `broker-client/.../commandapi/expression/request/EvaluateExpressionRequest.java` | 新建：默认 `RANDOM_PARTITION`；`setScopeKey` 非空时 `setKey + NOT_SPECIFIED_PARTITION`（按作用域路由到实体所在分区） |

### ⑤ 引擎层

| 文件 | 改动 |
|------|------|
| `bpmn-engine/.../commandapi/expression/ExpressionEvaluateApiProcessor.java` | 新建：`ScriptEngine.parse` → `evaluateObject` → `adResponse(EVALUATE_RESPONSE, requestId, response)`；失败 `adErrorResponse` |
| `bpmn-engine/.../commandapi/expression/ExpressionApiProcessorRegister.java` | 新建：`onCommand(new ExpressionEvaluateApiProcessor(writer))` |
| `bpmn-engine/.../commandapi/ApiCommandProcessorRegister.java` | 挂接 |

### ② 网关层

| 文件 | 改动 |
|------|------|
| `gateway-protocol/src/main/proto/expression_service.proto` | 新建（expression/variables/tenantId/scopeKey 四字段 + stream 响应） |
| `gateway-grpc/.../service/impl/GrpcExpressionServiceImpl.java` | 新建：proto→broker 请求（`ensureJsonSet` 补空变量；`scopeKey>0` 才透传）→ `brokerClient.sendRequest` → `convertToJson(value.getResultBuffer())` 回包 + `value.reset()` |
| `gateway-grpc/.../GatewayGrpcService.java` | `createGrpcServices()` 加一行 |

### ① 客户端层

| 文件 | 改动 |
|------|------|
| `clients/client-java/.../command/expression/EvaluateExpressionCommand.java` | 新建接口（expression/tenantId/scopeKey + 变量） |
| `clients/client-java/.../command/expression/EvaluateExpressionCommandImpl.java` | 新建（RetriableClientFutureImpl 三段式） |
| `clients/client-java/.../command/expression/EvaluateExpressionCommandResponse(Impl).java` | 新建（`getResult()`） |
| `clients/client-java/.../KunpengClient(Impl).java` | `newEvaluateExpressionCommand()` + stub 装配 |

## 设计决策点评（讲 why）

- **scopeKey 双路由**：无作用域 → 随机分区摊负载；有作用域（如元素实例 key）→ 按 key 定分区，与被求值实体同分区，避免跨分区取变量
- **resultBuffer 走 structpack Document**：求值结果可能是对象/数组，Document 编码到网关再 `convertToJson` 成 String，proto 只传字符串
- **EVALUATE_RESPONSE 用 `NOT_PROCESS_INDEX`**：响应不进处理器分发（没有处理器要跑），只占 recordIndex 保元数据索引唯一
- **variables 在网关 `ensureJsonSet`**：空变量统一补 `{}`，引擎侧不再判空——边界处归一化

## 用它当模板抄新命令

1. 把上表「文件 → 改动」里的 Expression 全局替换成你的域名
2. 先过 SKILL.md 的 11 项 checklist 逐项核对
3. 若是持久化型：把 ⑤ 换成 `ProcessInstanceCreateProcessor` 四步范式（见 05-engine.md），并补 06-storage.md 的三件套
