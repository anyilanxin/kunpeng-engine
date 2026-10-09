/*
 * Copyright © 2026 anyilanxin zxh(anyilanxin@aliyun.com)
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.gateway.grpc.service.impl;

import static com.anyilanxin.kunpeng.gateway.grpc.utils.RequestUtil.ensureJsonSet;
import static com.anyilanxin.kunpeng.structpack.util.DocumentUtil.convertToJson;

import com.anyilanxin.kunpeng.broker.client.business.BrokerClient;
import com.anyilanxin.kunpeng.broker.client.business.BrokerResponse;
import com.anyilanxin.kunpeng.broker.client.business.commandapi.expression.request.EvaluateExpressionRequest;
import com.anyilanxin.kunpeng.gateway.grpc.GrpcErrorHandle;
import com.anyilanxin.kunpeng.gateway.grpc.service.ExpressionServiceGrpc;
import com.anyilanxin.kunpeng.gateway.grpc.service.ExpressionServiceOuterClass;
import com.anyilanxin.kunpeng.gateway.grpc.service.GrpcService;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.expression.EvaluateExpressionResponseRecord;
import io.grpc.stub.StreamObserver;
import java.util.concurrent.CompletableFuture;

/**
 * 表达式评估 gRPC 服务实现。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class GrpcExpressionServiceImpl extends ExpressionServiceGrpc.ExpressionServiceImplBase
    implements GrpcService {
  private final BrokerClient brokerClient;
  private final GrpcErrorHandle handle;

  public GrpcExpressionServiceImpl(final BrokerClient brokerClient, final GrpcErrorHandle handle) {
    this.brokerClient = brokerClient;
    this.handle = handle;
  }

  @Override
  public String getServiceName() {
    return "Expression Service";
  }

  @Override
  public void evaluateExpression(
      final ExpressionServiceOuterClass.EvaluateExpressionRequest request,
      final StreamObserver<ExpressionServiceOuterClass.EvaluateExpressionResponse>
          responseObserver) {
    final long scopeKey = request.getScopeKey();
    final EvaluateExpressionRequest brokerRequest =
        new EvaluateExpressionRequest()
            .setExpression(request.getExpression())
            .setVariables(ensureJsonSet(request.getVariables()))
            .setTenantId(request.getTenantId())
            .setScopeKey(scopeKey > 0 ? scopeKey : null);
    final CompletableFuture<BrokerResponse<EvaluateExpressionResponseRecord>> future =
        brokerClient.sendRequest(brokerRequest);
    future.whenComplete(
        (brokerResponse, throwable) -> {
          if (throwable != null) {
            responseObserver.onError(handle.error(responseObserver, throwable));
          } else {
            if (brokerResponse.isSuccess()) {
              final EvaluateExpressionResponseRecord value = brokerResponse.getValue();
              final ExpressionServiceOuterClass.EvaluateExpressionResponse response =
                  ExpressionServiceOuterClass.EvaluateExpressionResponse.newBuilder()
                      .setResult(convertToJson(value.getResultBuffer()))
                      .build();
              responseObserver.onNext(response);
              value.reset();
              responseObserver.onCompleted();
            } else {
              responseObserver.onError(handle.error(responseObserver, brokerResponse));
            }
          }
        });
  }
}
