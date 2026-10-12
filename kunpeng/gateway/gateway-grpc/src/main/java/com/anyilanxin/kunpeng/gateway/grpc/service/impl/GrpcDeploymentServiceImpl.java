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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.gateway.grpc.service.impl;

import com.anyilanxin.kunpeng.broker.client.business.client.DeploymentClient;
import com.anyilanxin.kunpeng.broker.client.business.commandapi.deployment.request.DeploymentCreateRequest;
import com.anyilanxin.kunpeng.broker.client.business.commandapi.deployment.request.DeploymentDeleteRequest;
import com.anyilanxin.kunpeng.gateway.grpc.GrpcErrorHandle;
import com.anyilanxin.kunpeng.gateway.grpc.service.DeploymentServiceGrpc;
import com.anyilanxin.kunpeng.gateway.grpc.service.DeploymentServiceOuterClass;
import com.anyilanxin.kunpeng.gateway.grpc.service.GrpcService;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.deployment.create.DeploymentCreateResponseRecord;
import com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.deployment.create.DeploymentProcessDefinitionResponseRecordValue;
import com.anyilanxin.kunpeng.protocol.gateway.GatewayLoggers;
import io.grpc.stub.StreamObserver;
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
  private final DeploymentClient commands;
  private final GrpcErrorHandle handle;

  public GrpcDeploymentServiceImpl(final DeploymentClient commands, final GrpcErrorHandle handle) {
    this.commands = commands;
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
    commands
        .deployResource(brokerRequest)
        .whenComplete(
            (brokerResponse, throwable) -> {
              if (throwable != null) {
                responseObserver.onError(handle.error(responseObserver, throwable));
              } else {
                if (brokerResponse.isSuccess()) {
                  try {
                    final DeploymentCreateResponseRecord responseRecord = brokerResponse.getValue();
                    try {
                      final DeploymentServiceOuterClass.DeployResourceResponse.Builder builder =
                          DeploymentServiceOuterClass.DeployResourceResponse.newBuilder();
                      builder
                          .setDeploymentId(responseRecord.getDeploymentId())
                          .setTenantId(responseRecord.getTenantId());
                      for (final DeploymentProcessDefinitionResponseRecordValue
                          processDefinitionValue : responseRecord.getProcessDefinitions()) {
                        final DeploymentServiceOuterClass.ProcessDefinition processDefinition =
                            DeploymentServiceOuterClass.ProcessDefinition.newBuilder()
                                .setProcessDefinitionId(
                                    processDefinitionValue.getProcessDefinitionId())
                                .setProcessDefinitionKey(
                                    processDefinitionValue.getProcessDefinitionKey())
                                .setProcessDefinitionVersion(
                                    processDefinitionValue.getProcessDefinitionVersion())
                                .setResourceName(processDefinitionValue.getResourceDefinitionName())
                                .build();
                        builder.addProcessDefinitions(processDefinition);
                      }
                      responseObserver.onNext(builder.build());
                      responseObserver.onCompleted();
                    } finally {
                      responseRecord.reset();
                    }
                  } catch (final Exception e) {
                    LOG.error("Failed to build deployment response", e);
                    responseObserver.onError(handle.error(responseObserver, e));
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
    final DeploymentDeleteRequest brokerRequest =
        new DeploymentDeleteRequest().setDeploymentId(request.getResourceKey());
    commands
        .deleteResource(brokerRequest)
        .whenComplete(
            (brokerResponse, throwable) -> {
              if (throwable != null) {
                responseObserver.onError(handle.error(responseObserver, throwable));
              } else {
                if (brokerResponse.isSuccess()) {
                  final DeploymentServiceOuterClass.DeleteResourceResponse deleteResourceResponse =
                      DeploymentServiceOuterClass.DeleteResourceResponse.newBuilder().build();
                  responseObserver.onNext(deleteResourceResponse);
                  responseObserver.onCompleted();
                } else {
                  responseObserver.onError(handle.error(responseObserver, brokerResponse));
                }
              }
            });
  }
}
