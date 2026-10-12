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

import com.anyilanxin.kunpeng.broker.client.business.client.ProcessInstanceClient;
import com.anyilanxin.kunpeng.broker.client.business.commandapi.processinstance.request.ProcessInstanceCancelRequest;
import com.anyilanxin.kunpeng.broker.client.business.commandapi.processinstance.request.ProcessInstanceCreateRequest;
import com.anyilanxin.kunpeng.gateway.grpc.GrpcErrorHandle;
import com.anyilanxin.kunpeng.gateway.grpc.service.GrpcService;
import com.anyilanxin.kunpeng.gateway.grpc.service.ProcessInstanceServiceGrpc;
import com.anyilanxin.kunpeng.gateway.grpc.service.ProcessInstanceServiceOuterClass;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.processinstance.create.ProcessInstanceCreateResponseRecord;
import io.grpc.stub.StreamObserver;

/**
 * 流程实例 gRPC 服务实现。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class GrpcProcessInstanceServiceImpl
    extends ProcessInstanceServiceGrpc.ProcessInstanceServiceImplBase implements GrpcService {
  private final ProcessInstanceClient commands;
  private final GrpcErrorHandle handle;

  public GrpcProcessInstanceServiceImpl(
      final ProcessInstanceClient commands, final GrpcErrorHandle handle) {
    this.commands = commands;
    this.handle = handle;
  }

  @Override
  public String getServiceName() {
    return "Process Instance Service";
  }

  @Override
  public void createProcessInstance(
      final ProcessInstanceServiceOuterClass.CreateProcessInstanceRequest request,
      final StreamObserver<ProcessInstanceServiceOuterClass.CreateProcessInstanceResponse>
          responseObserver) {
    // 定义定位二选一：processDefinitionId 与 processDefinitionKey（含版本）不可同时缺省或同时提供
    final boolean byId = request.getProcessDefinitionId() > 0;
    final boolean byKey = !request.getProcessDefinitionKey().isEmpty();
    if (byId == byKey) {
      responseObserver.onError(
          handle.error(
              responseObserver,
              new IllegalArgumentException("processDefinitionId 与 processDefinitionKey 必须二选一")));
      return;
    }
    final ProcessInstanceCreateRequest brokerRequest = new ProcessInstanceCreateRequest();
    if (byId) {
      brokerRequest.setProcessDefinitionId(request.getProcessDefinitionId());
    } else {
      brokerRequest.setProcessDefinitionKey(
          request.getProcessDefinitionKey(), request.getVersion());
    }
    brokerRequest
        .setVariable(ensureJsonSet(request.getVariable()))
        .setTenantId(request.getTenantId());
    commands
        .createProcessInstance(brokerRequest)
        .whenComplete(
            (brokerResponse, throwable) -> {
              if (throwable != null) {
                responseObserver.onError(handle.error(responseObserver, throwable));
              } else {
                if (brokerResponse.isSuccess()) {
                  final ProcessInstanceCreateResponseRecord value = brokerResponse.getValue();
                  final ProcessInstanceServiceOuterClass.CreateProcessInstanceResponse response =
                      ProcessInstanceServiceOuterClass.CreateProcessInstanceResponse.newBuilder()
                          .setProcessInstanceId(value.getProcessInstanceId())
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

  @Override
  public void createProcessInstanceWithResult(
      final ProcessInstanceServiceOuterClass.CreateProcessInstanceWithResultRequest request,
      final StreamObserver<ProcessInstanceServiceOuterClass.CreateProcessInstanceWithResultResponse>
          responseObserver) {
    // 引擎侧暂无结果等待语义（创建即返回），显式回 UNIMPLEMENTED；裸抛异常只会得到无信息量的 UNKNOWN
    responseObserver.onError(
        io.grpc.Status.UNIMPLEMENTED
            .withDescription("createProcessInstanceWithResult is not implemented yet")
            .asException());
  }

  @Override
  public void cancelProcessInstance(
      final ProcessInstanceServiceOuterClass.CancelProcessInstanceRequest request,
      final StreamObserver<ProcessInstanceServiceOuterClass.CancelProcessInstanceResponse>
          responseObserver) {
    final long processInstanceId = request.getProcessInstanceId();
    final ProcessInstanceCancelRequest brokerRequest =
        new ProcessInstanceCancelRequest(processInstanceId);
    commands
        .cancelProcessInstance(brokerRequest)
        .whenComplete(
            (brokerResponse, throwable) -> {
              if (throwable != null) {
                responseObserver.onError(handle.error(responseObserver, throwable));
              } else {
                if (brokerResponse.isSuccess()) {
                  final ProcessInstanceServiceOuterClass.CancelProcessInstanceResponse response =
                      ProcessInstanceServiceOuterClass.CancelProcessInstanceResponse.newBuilder()
                          .build();
                  responseObserver.onNext(response);
                  responseObserver.onCompleted();
                } else {
                  responseObserver.onError(handle.error(responseObserver, brokerResponse));
                }
              }
            });
  }
}
