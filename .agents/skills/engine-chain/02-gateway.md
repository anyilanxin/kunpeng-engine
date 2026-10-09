# 02 网关层（gateway-protocol + gateway-grpc）

## 本端职责与边界

**职责（本端 owns）**：

- 对外协议契约：`xxx_service.proto` 的 message 字段、rpc 签名、错误码语义注释（唯一对外承诺，字段只增不改）
- 协议转换：proto message ⇄ broker 请求/响应的双向字段映射；二进制 Document ↔ JSON String 转换（`DocumentUtil.convertToJson`）
- 路由参数语义落地：proto 的可空约定（如 `scopeKey=0` 表示无）→ broker 请求的 null/随机分区语义
- 错误统一出口：broker 异常/失败响应 → gRPC Status（`GrpcErrorHandle`），网关不自造错误码
- 服务生命周期与健康上报（`GrpcService.getServiceName`、纳入 `GatewayHealthManager`）

**边界（不做）**：

- ❌ 业务逻辑与校验规则——网关无状态、不判断、不记忆
- ❌ 直连引擎或存储——只经 `BrokerClient` 一个出口
- ❌ 重试/超时决策——透传 broker-client 的 `sendRequestWithRetry` 能力，自己不实现退避

**输入 → 输出**：gRPC 请求 → broker 请求（异步 future）→ gRPC 响应

> 定位一句话：网关是**翻译官**，把 gRPC 方言翻译成 broker 方言，仅此而已。

## proto 契约（gateway-protocol）

```
kunpeng/gateway/gateway-protocol/src/main/proto/xxx_service.proto
```

```protobuf
syntax = 'proto3';
package gateway_protocol;

option java_multiple_files = false;
option java_package = "com.anyilanxin.kunpeng.gateway.grpc.service";
option go_package = "./;pb";

message EvaluateExpressionRequest {   // 字段与 broker Record 对齐（03-protocol）
  string expression = 1;
  string variables = 2;
  string tenantId = 3;
  int64 scopeKey = 4;
}

message EvaluateExpressionResponse {
  string result = 1;
}

service ExpressionService {
  // rpc 注释里写清错误码语义（INVALID_ARGUMENT: expression is blank 等）
  rpc EvaluateExpression (EvaluateExpressionRequest) returns (stream EvaluateExpressionResponse) {
  }
}
```

规范：

- 一个业务域一个 `xxx_service.proto`，放 `src/main/proto/`；`kunpeng.grpc-java` 插件自动编译出 `XxxServiceGrpc` + `XxxServiceOuterClass`，**不需要手工跑生成**
- rpc 返回统一用 `returns (stream ...)`（服务端流，即便只回一条）
- 字段编号只增不改；message 字段与 broker 侧 Record 字段一一对齐，别在网关做字段裁剪

## gRPC 服务实现（gateway-grpc）

```
kunpeng/gateway/gateway-grpc/src/main/java/com/anyilanxin/kunpeng/gateway/grpc/service/impl/GrpcXxxServiceImpl.java
```

固定骨架（照抄 `GrpcExpressionServiceImpl`）：

```java
public class GrpcExpressionServiceImpl extends ExpressionServiceGrpc.ExpressionServiceImplBase
    implements GrpcService {
  private final BrokerClient brokerClient;
  private final GrpcErrorHandle handle;

  @Override
  public String getServiceName() { return "Expression Service"; }   // 健康检查展示名

  @Override
  public void evaluateExpression(
      final ExpressionServiceOuterClass.EvaluateExpressionRequest request,
      final StreamObserver<ExpressionServiceOuterClass.EvaluateExpressionResponse> responseObserver) {
    // ① proto → broker 请求（路由参数在此定：scopeKey>0 才透传，否则 null 走随机分区）
    final var brokerRequest = new EvaluateExpressionRequest()
        .setExpression(request.getExpression())
        .setVariables(ensureJsonSet(request.getVariables()))   // 空变量补 {}，RequestUtil 静态导入
        .setTenantId(request.getTenantId())
        .setScopeKey(scopeKey > 0 ? scopeKey : null);

    // ② 异步转发
    brokerClient.sendRequest(brokerRequest).whenComplete((brokerResponse, throwable) -> {
      if (throwable != null) {
        responseObserver.onError(handle.error(responseObserver, throwable));
        return;
      }
      if (brokerResponse.isSuccess()) {
        final var value = brokerResponse.getValue();
        final var response = ....newBuilder()
            .setResult(convertToJson(value.getResultBuffer()))   // structpack DocumentUtil
            .build();
        responseObserver.onNext(response);
        value.reset();                                          // Record 对象池归还，防泄漏
        responseObserver.onCompleted();
      } else {
        responseObserver.onError(handle.error(responseObserver, brokerResponse));
      }
    });
  }
}
```

要点：

- 二进制缓冲字段（变量、结果文档）用 `com.anyilanxin.kunpeng.structpack.util.DocumentUtil.convertToJson` 在网关转 String，**别把 DirectBuffer 泄漏到 proto 层**
- `value.reset()` 必须在响应写完后调用（Record 是池化对象）
- 所有错误出口统一走 `GrpcErrorHandle.error(...)`，不要自己构造 Status

## 服务注册（一处）

`GatewayGrpcService.createGrpcServices()` 的 `List.of(...)` 里加一行：

```java
new GrpcExpressionServiceImpl(brokerClient, errorHandle),
```

`createServer()` 会遍历 `addService`，健康检查（`GatewayHealthManagerImpl`）自动纳入。

## 验证

```bash
./gradlew :kunpeng:gateway:gateway-protocol:compileJava :kunpeng:gateway:gateway-grpc:compileJava
```

> REST 面（gateway-rest）按需另行暴露，不在本链最小集内。
