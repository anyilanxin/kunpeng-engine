# 04 Broker 层（broker-client 请求侧 + broker 入口/回程）

## 本端职责与边界

**职责（本端 owns）**：

- **broker-client（网关侧请求库）**：
  - 业务请求封装：`XxxRequest extends CommandApiBrokerRequest`，fluent setter 委托 Record
  - 分区路由策略：随机分区 / 按 key 哈希 / 指定分区（选型见下文路由表）
  - 统一帧编码（`ApiRequestWriterImpl`：key/valueType/lifeCycle/SBE 数据帧）
  - leader 寻址（拓扑感知）、messaging 收发、超时与重试（`sendRequestWithRetry`，10ms 退避）
- **broker 本体（CommandApiHandleImpl）**：
  - 命令入口：每分区订阅 `COMMAND_API/{partitionId}` topic；requestId 生成（IdGenerator）与请求表挂账（future 暂存）
  - 反序列化 + 入日志：`valueMapper.getValue(lifeCycle)` 拿空 Record → `wrap(data)` → `RecordAppendEntryFactory` → `eventlog.tryAppend(USER_COMMAND)` → Raft 复制
  - 响应回程：按 requestId 取回 future，SBE `ApiResponseWriter` 编码（成功带 Record 数据帧 / 失败带 code+message）→ messaging 回发
  - 背板保护：磁盘满拒流、丢 leader 注销 handler

**边界（不做）**：

- ❌ 不理解业务语义——broker 只认 Record 不认业务，任何新命令对它零改动
- ❌ 不推进状态机、不做业务校验（引擎职责）
- ❌ 不落业务状态——日志条目不是状态，重放经引擎+applier 才落库

**输入 → 输出**：统一请求帧 → 分区日志条目（去程）；引擎响应 Record → 统一响应帧（回程，requestId 关联）

> 定位一句话：broker 是**邮局**——按地址（分区路由）投递、开回执（requestId），不拆信、不看内容。

```
kunpeng/broker/broker-client/src/main/java/com/anyilanxin/kunpeng/broker/client/business/commandapi/xxx/request/XxxRequest.java
```

```java
public class EvaluateExpressionRequest
    extends CommandApiBrokerRequest<EvaluateExpressionRequestRecord> {

  public EvaluateExpressionRequest() {
    super(
        ValueType.EXPRESSION_API,                                  // ③ 的 ValueType
        CommandApiExpressionValueLifeCycle.EVALUATE_REQUEST,       // ③ 的请求态
        new EvaluateExpressionRequestRecord());                    // ④ 的 Record
    setPartitionId(RANDOM_PARTITION);                              // 默认路由策略
  }

  // fluent setter 一律委托给 getValue().setXxx(...)
  public EvaluateExpressionRequest setExpression(final String expression) {
    getValue().setExpression(expression);
    return this;
  }

  // 路由参数：设置 key 的同时切换为"按 key 定分区"
  public EvaluateExpressionRequest setScopeKey(final Long scopeKey) {
    if (scopeKey != null) {
      setKey(scopeKey);
      setPartitionId(NOT_SPECIFIED_PARTITION);
    }
    return this;
  }
}
```

### 分区路由规则（BrokerRequest 基类）

| 设置 | 语义 |
|------|------|
| `setPartitionId(RANDOM_PARTITION)` | 随机分区（无状态偏好，如表达式求值） |
| `setKey(k)` + `NOT_SPECIFIED_PARTITION` | 按 key 哈希定分区（实体亲和，如同 key 的变量/任务操作） |
| 显式 `setPartitionId(n)` | 直达指定分区（少用，如部署分区） |

**选型原则**：同一实体的连续命令必须路由到同一分区（事件日志按分区定序），无实体亲和的才用随机。

### 发送管线（BrokerClientImpl，不用改，但要懂）

`brokerClient.sendRequest(request)` → actor 线程内：寻址分区 leader → `ApiRequestWriterImpl` 统一编码（key/valueType/lifeCycle/SBE 数据帧）→ `messagingService.sendAndReceive(topic)` → 响应字节经 `ApiResponseReaderImpl` 解码成 `BrokerResponse<VALUE>`（同样依赖 ⑤ 的 Record 注册）。

- 超时：`sendRequest(request, timeout)`；失败重试：`sendRequestWithRetry(...)`（10ms 退避重发）
- topic 由 `TopicUtils.getTopicName(RecordType, partitionId)` 派生，新命令零改动

## broker 入口（CommandApiHandleImpl，不用改，但必须懂）

`kunpeng/broker/broker/src/main/java/com/anyilanxin/kunpeng/broker/commandapi/CommandApiHandleImpl.java`

请求进分区日志的完整路径：

```
messaging 收到（topic = COMMAND_API/{partitionId}）
  → handleRequest：idGenerator.nextId() 生成 requestId
      partitionsRequestMap[partitionId][requestId] = PartitionRequest(future, requestType, valueType)
  → writeCommand：
      RecordMetadata(requestId, valueType, lifeCycle, recordType, brokerVersion)
      recordValue = valueMapper.getValue(lifeCycle)     ★ 依赖 ⑤ 注册
      recordValue.wrap(requestReader.data())
      RecordAppendEntryFactory.of(key, metadata, recordValue)
      logStreamWriter.tryAppend(WriteContext.USER_COMMAND, appendEntry)   → eventlog → Raft
```

- **新命令在此零改动**——只要 ③⑤ 注册齐，入口天然认识你的 Record
- 磁盘满（`isDiskSpaceAvailable=false`）时请求直接拒绝；leader 丢失时 handler 注销

## 响应回程（requestId 关联）

```
引擎处理器 writer.adResponse(lifeCycle, requestId, responseRecord)      [05-engine]
  → processingCollect.sendResponse()
  → CommandApiHandle.sendResponse(BrokerResponseWriter)
      partitionsRequestMap[partitionId].remove(requestId)               按 requestId 找回 future
      ApiResponseWriterImpl（SBE）：requestType/valueType/lifeCycle + value.write(...) 或 fail(code, message)
      partitionRequest.responseFuture.complete(bytes)                  → messaging 回给网关
  → 网关 BrokerClient future 完成 → GrpcXxxServiceImpl responseObserver.onNext
```

要点：

- `requestId` 是全链请求-响应关联的唯一钥匙，由 broker 入口生成、经 RecordMetadata 进日志、由处理器原样带回
- 响应数据帧是 structpack Record 直写（`value.write(buffer)`），网关侧 `BrokerResponse.getValue()` 再 wrap 回来——所以响应 Record 也必须注册（⑤ 的 RESPONSE 态）
- `response.isSuccess()` 为 false 时走 `fail(code, message)`，网关映射为 gRPC 错误

## 管理面平行体系

`broker-admin-client` + `broker/bootstrap/.../AdminCommandApiHandleImpl` 是管理面（cluster-dispatch 用）的同构实现（`AdminValueLifeCycle`/`AdminRecordProcessIndex`），业务命令不碰。

## 验证

```bash
./gradlew :kunpeng:broker:broker-client:compileJava
```
