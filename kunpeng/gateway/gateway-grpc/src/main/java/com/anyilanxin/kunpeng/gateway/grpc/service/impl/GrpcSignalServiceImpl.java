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

import static com.anyilanxin.kunpeng.broker.client.business.ClientRequest.RANDOM_PARTITION;
import static com.anyilanxin.kunpeng.gateway.grpc.utils.RequestUtil.ensureJsonSet;

import com.anyilanxin.kunpeng.broker.client.business.BrokerClient;
import com.anyilanxin.kunpeng.broker.client.business.BrokerResponse;
import com.anyilanxin.kunpeng.broker.client.business.commandapi.signal.request.SignalCorrelationRequest;
import com.anyilanxin.kunpeng.gateway.grpc.GrpcErrorHandle;
import com.anyilanxin.kunpeng.gateway.grpc.service.GrpcService;
import com.anyilanxin.kunpeng.gateway.grpc.service.SignalServiceGrpc;
import com.anyilanxin.kunpeng.gateway.grpc.service.SignalServiceOuterClass;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.signal.correlation.SignalCorrelationResponseRecord;
import com.anyilanxin.kunpeng.protocol.gateway.GatewayLoggers;
import io.grpc.stub.StreamObserver;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;

/**
 * 信号 gRPC 服务实现。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class GrpcSignalServiceImpl extends SignalServiceGrpc.SignalServiceImplBase
    implements GrpcService {
  private static final Logger LOG = GatewayLoggers.GATEWAY_LOGGER_GRPC;
  private final BrokerClient brokerClient;
  private final GrpcErrorHandle handle;

  public GrpcSignalServiceImpl(final BrokerClient brokerClient, final GrpcErrorHandle handle) {
    this.brokerClient = brokerClient;
    this.handle = handle;
  }

  @Override
  public String getServiceName() {
    return "Signal Service";
  }

  @Override
  public void signalCorrelation(
      final SignalServiceOuterClass.SignalCorrelationRequest request,
      final StreamObserver<SignalServiceOuterClass.SignalCorrelationResponse> responseObserver) {
    final SignalCorrelationRequest requestCorrelation =
        new SignalCorrelationRequest(request.getSignalName());
    requestCorrelation
        .setTenantId(request.getTenantId())
        .setProcessInstanceId(request.getProcessInstanceId())
        .setVariable(ensureJsonSet(request.getVariable()));
    if (request.getProcessInstanceId() <= 0) {
      requestCorrelation.setPartitionId(RANDOM_PARTITION);
    }
    final CompletableFuture<BrokerResponse<SignalCorrelationResponseRecord>> messageRequest =
        brokerClient.sendRequest(requestCorrelation);
    messageRequest.whenComplete(
        (correlationResponse, messageThrowable) -> {
          if (messageThrowable != null) {
            responseObserver.onError(handle.error(responseObserver, messageThrowable));
          } else {
            if (correlationResponse.isSuccess()) {
              final SignalServiceOuterClass.SignalCorrelationResponse response =
                  SignalServiceOuterClass.SignalCorrelationResponse.newBuilder().build();
              responseObserver.onNext(response);
              responseObserver.onCompleted();
            } else {
              responseObserver.onError(handle.error(responseObserver, correlationResponse));
            }
          }
        });
  }
}
