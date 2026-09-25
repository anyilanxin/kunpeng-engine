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
import com.anyilanxin.kunpeng.broker.client.business.commandapi.deployment.request.DeploymentCreateRequest;
import com.anyilanxin.kunpeng.gateway.grpc.GrpcErrorHandle;
import com.anyilanxin.kunpeng.gateway.grpc.service.DeploymentServiceGrpc;
import com.anyilanxin.kunpeng.gateway.grpc.service.DeploymentServiceOuterClass;
import com.anyilanxin.kunpeng.gateway.grpc.service.GrpcService;
import com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.deployment.create.DeploymentCreateResponseRecordValue;
import com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.deployment.create.DeploymentProcessDefinitionResponseRecordValue;
import com.anyilanxin.kunpeng.protocol.gateway.GatewayLoggers;
import io.grpc.stub.StreamObserver;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;

/**
 * 部署 gRPC 服务实现。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class GrpcDeploymentServiceImpl extends DeploymentServiceGrpc.DeploymentServiceImplBase
    implements GrpcService {
  private static final Logger LOG = GatewayLoggers.GATEWAY_LOGGER_GRPC;
  private final BrokerClient brokerClient;
  private final GrpcErrorHandle handle;

  public GrpcDeploymentServiceImpl(final BrokerClient brokerClient, final GrpcErrorHandle handle) {
    this.brokerClient = brokerClient;
    this.handle = handle;
  }

  @Override
  public String getServiceName() {
    return "Deployment Service";
  }

  @Override
  public void deployResource(
      final DeploymentServiceOuterClass.DeployResourceRequest request,
      final StreamObserver<DeploymentServiceOuterClass.DeployResourceResponse> responseObserver) {
    final DeploymentCreateRequest brokerRequest = new DeploymentCreateRequest();
    request
        .getResourcesList()
        .forEach(v -> brokerRequest.addResource(v.getName(), v.getContent().toByteArray()));
    final CompletableFuture<BrokerResponse<DeploymentCreateResponseRecordValue>> future =
        brokerClient.sendRequest(brokerRequest);
    future.whenComplete(
        (brokerResponse, throwable) -> {
          if (throwable != null) {
            responseObserver.onError(handle.error(responseObserver, throwable));
          } else {
            if (brokerResponse.isSuccess()) {
              try {
                final DeploymentCreateResponseRecordValue responseRecordValue =
                    brokerResponse.getValue();
                final DeploymentServiceOuterClass.DeployResourceResponse.Builder builder =
                    DeploymentServiceOuterClass.DeployResourceResponse.newBuilder();
                builder
                    .setDeploymentId(responseRecordValue.getDeploymentId())
                    .setTenantId(responseRecordValue.getTenantId());
                for (final DeploymentProcessDefinitionResponseRecordValue processDefinitionValue :
                    responseRecordValue.getProcessDefinitions()) {
                  final DeploymentServiceOuterClass.ProcessDefinition processDefinition =
                      DeploymentServiceOuterClass.ProcessDefinition.newBuilder()
                          .setProcessDefinitionId(processDefinitionValue.getProcessDefinitionId())
                          .setProcessDefinitionKey(processDefinitionValue.getProcessDefinitionKey())
                          .setProcessDefinitionVersion(
                              processDefinitionValue.getProcessDefinitionVersion())
                          .setResourceName(processDefinitionValue.getResourceDefinitionName())
                          .build();
                  builder.addProcessDefinitions(processDefinition);
                }
                responseObserver.onNext(builder.build());
                responseRecordValue.reset();
                responseObserver.onCompleted();
              } catch (final Exception e) {
                LOG.error("Failed to build deployment response", e);
              }
            } else {
              responseObserver.onError(handle.error(responseObserver, brokerResponse));
            }
          }
        });
  }

  @Override
  public void deleteResource(
      final DeploymentServiceOuterClass.DeleteResourceRequest request,
      final StreamObserver<DeploymentServiceOuterClass.DeleteResourceResponse> responseObserver) {
    //    brokerClient
    //        .sendRequest(new SimpleRequestDto())
    //        .whenComplete(
    //            (brokerResponse, throwable) -> {
    //              if (throwable != null) {
    //                responseObserver.onError(throwable);
    //              } else {
    //                final DeploymentServiceOuterClass.DeleteResourceResponse
    // deleteResourceResponse =
    //                    DeploymentServiceOuterClass.DeleteResourceResponse.newBuilder().build();
    //                responseObserver.onNext(deleteResourceResponse);
    //                responseObserver.onCompleted();
    //              }
    //            });
  }
}
