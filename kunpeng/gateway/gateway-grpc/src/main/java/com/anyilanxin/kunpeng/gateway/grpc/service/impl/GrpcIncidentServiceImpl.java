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

import com.anyilanxin.kunpeng.broker.client.business.BrokerClient;
import com.anyilanxin.kunpeng.broker.client.business.BrokerResponse;
import com.anyilanxin.kunpeng.broker.client.business.commandapi.incident.request.IncidentResolveRequest;
import com.anyilanxin.kunpeng.gateway.grpc.GrpcErrorHandle;
import com.anyilanxin.kunpeng.gateway.grpc.service.GrpcService;
import com.anyilanxin.kunpeng.gateway.grpc.service.IncidentServiceGrpc;
import com.anyilanxin.kunpeng.gateway.grpc.service.IncidentServiceOuterClass;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.incident.resolve.IncidentResolveResponseRecord;
import com.anyilanxin.kunpeng.protocol.gateway.GatewayLoggers;
import io.grpc.stub.StreamObserver;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;

/**
 * 事件（incident）gRPC 服务实现。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class GrpcIncidentServiceImpl extends IncidentServiceGrpc.IncidentServiceImplBase
    implements GrpcService {
  private static final Logger LOG = GatewayLoggers.GATEWAY_LOGGER_GRPC;
  private final BrokerClient brokerClient;
  private final GrpcErrorHandle handle;

  public GrpcIncidentServiceImpl(final BrokerClient brokerClient, final GrpcErrorHandle handle) {
    this.brokerClient = brokerClient;
    this.handle = handle;
  }

  @Override
  public String getServiceName() {
    return "Incident Service";
  }

  @Override
  public void incidentResolve(
      final IncidentServiceOuterClass.IncidentResolveRequest request,
      final StreamObserver<IncidentServiceOuterClass.IncidentResolveResponse> responseObserver) {
    final IncidentResolveRequest brokerRequest =
        new IncidentResolveRequest(request.getIncidentId()).setTenantId(request.getTenantId());
    final CompletableFuture<BrokerResponse<IncidentResolveResponseRecord>> future =
        brokerClient.sendRequest(brokerRequest);
    future.whenComplete(
        (brokerResponse, throwable) -> {
          if (throwable != null) {
            responseObserver.onError(handle.error(responseObserver, throwable));
          } else {
            if (brokerResponse.isSuccess()) {
              final IncidentServiceOuterClass.IncidentResolveResponse response =
                  IncidentServiceOuterClass.IncidentResolveResponse.newBuilder().build();
              responseObserver.onNext(response);
              responseObserver.onCompleted();
            } else {
              responseObserver.onError(handle.error(responseObserver, brokerResponse));
            }
          }
        });
  }
}
