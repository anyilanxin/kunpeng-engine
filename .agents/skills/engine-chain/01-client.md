# 01 客户端命令层（clients/client-java）

## 本端职责与边界

**职责（本端 owns）**：

- 命令 API 形态设计：fluent builder 接口（分步骤接口 `FinalCommandStep` / `CommandWithVariables`），业务方唯一入口
- 必填参数前置校验（`ArgumentUtil.ensureNotNull`），尽早失败在客户端本地
- 变量序列化：Map/POJO → JSON 字符串（经 `jsonMapper`）
- gRPC 发送：async stub + deadline（`requestTimeout`）、按 `CredentialsProvider.StatusCode` 的可重试 Future（`RetriableClientFutureImpl`）
- 连接与寻址：channel/stub 装配、`withCallCredentials` 凭据、负载均衡与网关发现（`KunpengLoadBalancerProvider` 系）

**边界（不做）**：

- ❌ 业务规则校验——"流程定义是否存在"这类判断属于引擎，客户端只校验"参数填没填"
- ❌ 分区路由决策——只在必要处透传路由参数（如 `scopeKey`），路由策略由 broker 侧落地
- ❌ 响应业务语义解释——DTO 只做 proto→Java 映射，不加工

**输入 → 输出**：业务方法调用 → `KunpengFuture<XxxCommandResponse>`

## 四件套

新增一个客户端命令 = 4 组文件改动（以表达式求值为参照）：

```
clients/client-java/src/main/java/com/anyilanxin/kunpeng/client/
├── KunpengClient.java                     # 接口加 newXxxCommand() 方法
├── KunpengClientImpl.java                 # stub 字段 + 装配 + new 实现
└── command/xxx/
    ├── XxxCommand.java                    # 命令接口（builder 步骤）
    ├── XxxCommandImpl.java                # 唯一实现
    ├── XxxCommandResponse.java            # 响应接口
    └── XxxCommandResponseImpl.java        # 响应实现（proto→DTO）
```

## 命令接口规范

```java
public interface EvaluateExpressionCommand
    extends FinalCommandStep<EvaluateExpressionCommandResponse>,
        CommandWithVariables<EvaluateExpressionCommand> {

  EvaluateExpressionCommand expression(String expression);   // 业务参数，必填用 ArgumentUtil 校验
  EvaluateExpressionCommand tenantId(String tenantId);       // 常规可选参数
  EvaluateExpressionCommand scopeKey(Long scopeKey);         // 路由参数（见 04-broker）
}
```

- `FinalCommandStep<RESP>` 提供 `requestTimeout(Duration)` 与 `send()`，业务参数方法只加自己的
- 带变量的命令额外 extends `CommandWithVariables`（`variables(String json)` / `variables(Map)`），与引擎侧 `ensureJsonSet` 对应
- 接口 javadoc 用中文一句话说明用途（仓库现状风格）

## 实现类规范（照抄 EvaluateExpressionCommandImpl）

```java
public final class EvaluateExpressionCommandImpl
    extends CommandWithVariables2<EvaluateExpressionCommandImpl>
    implements EvaluateExpressionCommand {

  // 1) 四参构造：asyncStub + config + jsonMapper + retryPredicate
  // 2) 字段：proto requestBuilder + requestTimeout（构造时取 config.getDefaultRequestTimeout()）
  // 3) send() 模式（固定三段）：
  private KunpengFuture<EvaluateExpressionCommandResponse> sendGrpcRequest() {
    final var request = requestBuilder.build();
    final var future = new RetriableClientFutureImpl<>(
        response -> new EvaluateExpressionCommandResponseImpl(response, objectMapper),  // proto→DTO
        retryPredicate,                                                                 // 按 StatusCode 决定重试
        streamObserver -> sendGrpcRequest(request, streamObserver));                    // 重试时重新发起
    sendGrpcRequest(request, future);
    return future;
  }

  private void sendGrpcRequest(final var request, final StreamObserver<...> streamObserver) {
    asyncStub.withDeadlineAfter(requestTimeout.toMillis(), TimeUnit.MILLISECONDS)
        .evaluateExpression(request, streamObserver);       // proto rpc 方法名
  }

  // 4) setVariablesInternal 覆写：requestBuilder.setVariables(variables)
  // 5) 必填参数：ArgumentUtil.ensureNotNull("expression", expression)
}
```

要点：

- `RetriableClientFutureImpl` 三参顺序固定：响应映射 / 重试谓词 / 重发 lambda；不要自己 new CompletableFuture
- 返回 `KunpengFuture`（不是 CompletableFuture），调用方可用 `join()` 阻塞或异步回调
- proto 字段没有可空语义：可空 Long 用 `scopeKey == null ? 0L : scopeKey` 之类约定，并在网关侧还原

## 响应 DTO

- `XxxCommandResponse` 接口只暴露业务字段 getter（如 `String getResult()`）
- `XxxCommandResponseImpl` 持有 proto response + objectMapper，getter 里做转换
- 需要暴露变量文档的（如 `CreateProcessInstanceWithResultCommandResponse.getVariables()`）返回 JSON 字符串，由调用方自解析

## 客户端装配（KunpengClientImpl 三处）

1. **stub 字段**：`private final ExpressionServiceGrpc.ExpressionServiceStub expressionService;`
2. **构造器装配**：`ExpressionServiceGrpc.newStub(channel).withCallCredentials(credentials)`（有独立寻址诉求时抽 `buildXxxService(...)` 静态工厂，如 `buildExpressionService`）
3. **工厂方法**：

```java
@Override
public EvaluateExpressionCommand newEvaluateExpressionCommand() {
  return new EvaluateExpressionCommandImpl(expressionService, configuration, jsonMapper, retryPredicate);
}
```

同时 `KunpengClient` 接口加对应方法 + javadoc。**接口与 Impl 两处都要加**，编译器会兜底。

## 验证

```bash
./gradlew :clients:client-java:compileJava
```

测试放同模块，最小覆盖：参数必填校验、send 后 future 映射（用 gRPC stub fake 或复用现有测试基建）。
