# 05 引擎层（engine/bpmn-engine commandapi）

## 本端职责与边界

**职责（本端 owns）**——全链唯一有业务判断权的地方：

- 命令处理与**业务规则校验**：读 immutable 仓储（`writer.getRepository()`）判断"流程定义存不存在""状态允不允许"
- 命令模式执行：直答型 `adResponse` 立即回；持久化型 `addCommand`/`addEvent` 写日志驱动域状态机
- key 发号：`nextCurrentSourceKey()` / `nextKey(resourceId)` / `nextGlobalKey()`（实体身份从这里出生）
- 状态机推进：域 LifeCycle 各状态的处理器；跨分区命令（`InterPartitionCommandSender`）；BPMN 行为门面（`behavior()`：变量/定时器/…）；表达式/脚本引擎
- 分区事务：重放→处理（级联命令/事件，批上限 maxBatch）→发响应→日志追加→commit→副作用 flush；`EngineRollbackException` 整体回滚
- 处理器幂等：日志驱动必然重放，副作用一律走 `addSideEffect`（提交后执行）

**边界（不做）**：

- ❌ 不直接写存储——写状态唯一通道是 `addEvent`（经 applier 落库），禁止摸 KvStore 写接口
- ❌ 不管请求-响应关联——requestId 从 RecordMetadata 原样透传，注册/超时/清理都在 broker CommandApiHandle
- ❌ 不做网络出口——job 推送等经 `JobDeliveryPort` 端口由 broker 层装配注入，处理器不持网络客户端

**输入 → 输出**：`BusinessLogRecord`（重放）→ 响应 Record（直答）/ 新日志条目（命令与事件）/ 状态应用（随 EVENT）

> 定位一句话：引擎是**决策者**——收命令、判规则、改状态、发响应；所有"业务上对不对"的判断只发生在这里。

```
LogEventProcessor<T>                    基接口：valueType() + valueLifeCycles() + processRecord(record)
  └── LogEventProcessorSingleState<T>   单状态便捷接口：只声明 valueLifeCycle() 一个标记
```

- 分发依据是 `lifeCycle.processIndex()`（③ 编号池的 `PROCESS_INDEX_N`）——`LogEventProcessors` 内部就是 `LogEventProcessor[RecordProcessIndex.size()]` 数组
- 重复注册同一 processIndex → 启动抛 `IllegalStateException`；有 Record 没处理器 → 处理时抛 `Not Have LogEventProcessor`
- `tryHandleError(command, error)` 返回 `EXPECTED_ERROR/UNEXPECTED_ERROR`，区分业务拒绝（回错误响应）与系统异常（告警）

## 处理器 + 注册器（两件套）

```
kunpeng/engine/bpmn-engine/src/main/java/com/anyilanxin/kunpeng/engine/bpmn/commandapi/xxx/
├── XxxApiProcessorRegister.java       # processors.onCommand(new XxxApiProcessor(writer))
│                                      # 多个处理器链式 .onCommand(...)
└── XxxApiProcessor.java               # 处理器本体
```

挂入总注册器 `commandapi/ApiCommandProcessorRegister.registerRepository(processors, writer)`（加一行，保持域名字母序）。

## 模式 A：直答型（查询/求值）

`ExpressionEvaluateApiProcessor` 完整形态：

```java
public class ExpressionEvaluateApiProcessor
    implements LogEventProcessorSingleState<EvaluateExpressionRequestRecord> {
  private final LogEventWriter writer;

  @Override
  public void processRecord(final BusinessLogRecord<EvaluateExpressionRequestRecord> record) {
    final var value = record.getValue();
    // 1) 计算/校验，失败即回错误（requestId 从 record 取）
    // 2) 成功：构造 Response Record → 直接回写
    final var response = new EvaluateExpressionResponseRecord();
    response.setResult(result);
    writer.adResponse(CommandApiExpressionValueLifeCycle.EVALUATE_RESPONSE,
        record.getRequestId(), response);
  }

  @Override public ValueType valueType() { return ValueType.EXPRESSION_API; }
  @Override public CommandApiExpressionValueLifeCycle valueLifeCycle() {
    return CommandApiExpressionValueLifeCycle.EVALUATE_REQUEST;   // 与注册的请求态一致
  }
}
```

## 模式 B：持久化型（状态变更）

`ProcessInstanceCreateProcessor` 范式，四步：

```java
// 1) 读仓储校验（immutable 读仓储，只读）
final ProcessDefinitionRuntime def = bpmnResource.getRuntime(processDefinitionId);
if (def == null) { writer.adErrorResponse(requestId, 0, "流程定义 id 不存在:..."); return; }

// 2) 生成实体 key（分区 source 维度发号）
final long processInstanceId = writer.nextCurrentSourceKey();

// 3) 构造域 Record（域 LifeCycle 状态，不是 API LifeCycle）
final ProcessInstanceRecord record = new ProcessInstanceRecord()
    .setProcessInstanceId(processInstanceId)....;

// 4) 写日志：
writer.addEvent(asyncRequestRecord.getKey(), AsyncRequestLifeCycle.CREATED, requestId, requestRecord);
//   ^ EVENT：进日志 + 立即应用状态（applier 落库），异步响应凭据（内含原 requestId）
writer.addCommand(record.getProcessInstanceId(), ProcessInstanceLifeCycle.ACTIVATING, requestId, record);
//   ^ COMMAND：只进日志，触发域状态机（对应状态处理器继续推进）
```

响应在**状态机推进到可响应状态**时由对应处理器用 `AsyncRequestRecord` 里保存的 requestId 调 `adResponse` 回写（例：`ProcessInstanceActivateAndResultProcessor` 支撑 withResult 语义）。

## LogEventWriter API 速查

| 方法 | 语义 | 进日志 | 落状态 |
|------|------|:--:|:--:|
| `addCommand(key, lifeCycle, requestId, value)` | 触发状态机的命令 | ✔ | ✘（由状态处理器决定） |
| `addEvent(key, lifeCycle, requestId, value)` | 已发生的事件 | ✔ | ✔（同步 addState） |
| `adResponse(lifeCycle, requestId, value)` | API 响应直答 | ✘ | ✘ |
| `adEmptyResponse(requestId[, key])` | 空成功响应 | ✘ | ✘ |
| `adErrorResponse(requestId, code, message)` | 错误响应 | ✘ | ✘ |
| `addSideEffect(producer)` | 提交后副作用（job 推送等） | — | — |
| `nextCurrentSourceKey()` / `nextKey(resourceId)` / `nextGlobalKey()` | 发号 | — | — |
| `getRepository()` | immutable 读仓储族 | — | — |
| `behavior()` | BPMN 行为门面（variable/timer/...） | — | — |

**铁律**：处理器里对 Repository 只能经 `writer.getRepository()`（immutable 视图）；写状态只能走 `addEvent`，不允许直写仓储。

## 分区事务循环（EngineProcessService.processEvent，背景知识）

```
try (processingCollect):
  transaction.run {
     重放 LoggedEntry → recordValueMapper.getCacheValue(lifeCycle) → TypedRecordReader → addInitCommand
     while (processingCollect.hasNext() && 批未超 maxBatch):
         bpmnEngine.processEvent(next, processingCollect)      # 处理器在此被调，产出级联命令/事件
     processingCollect.sendResponse()                          # 响应先于落盘发出
     logStreamWriter.tryAppend(WriteContext.INTERNAL, entries) # 写分区日志（→ Raft）
  }
  transaction.commit()                                          # RocksDB 写前日志语义
  sideEffect flush                                              # job 推送等提交后副作用
catch EngineRollbackException → sendResponse + rollback
```

- 引擎是**日志驱动单线程 actor**：gateway 命令经 broker 写入 eventlog（USER_COMMAND），commit 后重放才进处理器——所以处理器**必然重放安全**（幂等），别在处理器里做不可重放的副作用（用 `addSideEffect`）
- `USER_COMMAND` 与 `INTERNAL` 两种 WriteContext 只影响流控计费语义，处理器无感

## 验证

```bash
./gradlew :kunpeng:engine:bpmn-engine:compileJava
```

处理器单测：构造 `BusinessLogRecord`（value + requestId + metadata）直调 `processRecord`，断言经由 writer 的输出（可 fake `ProcessingCollectSupplier`）。
