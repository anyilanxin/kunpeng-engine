/*
 * Copyright © 2026 anyilanxin zxh (anyilanxin@aliyun.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.client.command.expression;

import com.anyilanxin.kunpeng.client.KunpengClientConfiguration;
import com.anyilanxin.kunpeng.client.command.ArgumentUtil;
import com.anyilanxin.kunpeng.client.command.CommandWithVariables2;
import com.anyilanxin.kunpeng.client.command.CredentialsProvider;
import com.anyilanxin.kunpeng.client.command.FinalCommandStep;
import com.anyilanxin.kunpeng.client.command.JsonMapper;
import com.anyilanxin.kunpeng.client.command.KunpengFuture;
import com.anyilanxin.kunpeng.client.command.RetriableClientFutureImpl;
import com.anyilanxin.kunpeng.gateway.grpc.service.ExpressionServiceGrpc;
import com.anyilanxin.kunpeng.gateway.grpc.service.ExpressionServiceOuterClass;
import io.grpc.stub.StreamObserver;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

public final class EvaluateExpressionCommandImpl
    extends CommandWithVariables2<EvaluateExpressionCommandImpl>
    implements EvaluateExpressionCommand {

  private final ExpressionServiceGrpc.ExpressionServiceStub asyncStub;
  private final Predicate<CredentialsProvider.StatusCode> retryPredicate;
  private final ExpressionServiceOuterClass.EvaluateExpressionRequest.Builder requestBuilder;
  private Duration requestTimeout;

  public EvaluateExpressionCommandImpl(
      final ExpressionServiceGrpc.ExpressionServiceStub asyncStub,
      final KunpengClientConfiguration config,
      final JsonMapper jsonMapper,
      final Predicate<CredentialsProvider.StatusCode> retryPredicate) {
    super(jsonMapper);
    this.asyncStub = asyncStub;
    this.retryPredicate = retryPredicate;

    requestBuilder = ExpressionServiceOuterClass.EvaluateExpressionRequest.newBuilder();
    requestTimeout(config.getDefaultRequestTimeout());
  }

  @Override
  protected EvaluateExpressionCommandImpl setVariablesInternal(final String variables) {
    requestBuilder.setVariables(variables);
    return this;
  }

  @Override
  public FinalCommandStep<EvaluateExpressionCommandResponse> requestTimeout(
      final Duration requestTimeout) {
    this.requestTimeout = requestTimeout;
    return this;
  }

  @Override
  public KunpengFuture<EvaluateExpressionCommandResponse> send() {
    return sendGrpcRequest();
  }

  private KunpengFuture<EvaluateExpressionCommandResponse> sendGrpcRequest() {
    final ExpressionServiceOuterClass.EvaluateExpressionRequest request = requestBuilder.build();

    final RetriableClientFutureImpl<
            EvaluateExpressionCommandResponse,
            ExpressionServiceOuterClass.EvaluateExpressionResponse>
        future =
            new RetriableClientFutureImpl<>(
                response -> new EvaluateExpressionCommandResponseImpl(response, objectMapper),
                retryPredicate,
                streamObserver -> sendGrpcRequest(request, streamObserver));
    sendGrpcRequest(request, future);
    return future;
  }

  private void sendGrpcRequest(
      final ExpressionServiceOuterClass.EvaluateExpressionRequest request,
      final StreamObserver<ExpressionServiceOuterClass.EvaluateExpressionResponse> streamObserver) {
    asyncStub
        .withDeadlineAfter(requestTimeout.toMillis(), TimeUnit.MILLISECONDS)
        .evaluateExpression(request, streamObserver);
  }

  @Override
  public EvaluateExpressionCommand expression(final String expression) {
    ArgumentUtil.ensureNotNull("expression", expression);
    requestBuilder.setExpression(expression);
    return this;
  }

  @Override
  public EvaluateExpressionCommand tenantId(final String tenantId) {
    requestBuilder.setTenantId(tenantId);
    return this;
  }

  @Override
  public EvaluateExpressionCommand scopeKey(final Long scopeKey) {
    requestBuilder.setScopeKey(scopeKey == null ? 0L : scopeKey);
    return this;
  }
}
