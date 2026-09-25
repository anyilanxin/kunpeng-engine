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

import com.anyilanxin.kunpeng.broker.client.business.BrokerClient;
import com.anyilanxin.kunpeng.broker.client.business.BrokerResponse;
import com.anyilanxin.kunpeng.broker.client.business.commandapi.processinstance.request.ProcessInstanceCancelRequest;
import com.anyilanxin.kunpeng.broker.client.business.commandapi.processinstance.request.ProcessInstanceCreateRequest;
import com.anyilanxin.kunpeng.gateway.grpc.GrpcErrorHandle;
import com.anyilanxin.kunpeng.gateway.grpc.service.GrpcService;
import com.anyilanxin.kunpeng.gateway.grpc.service.ProcessInstanceServiceGrpc;
import com.anyilanxin.kunpeng.gateway.grpc.service.ProcessInstanceServiceOuterClass;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.processinstance.create.ProcessInstanceCreateResponseRecord;
import com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.processinstance.cancel.ProcessInstanceCancelResponseRecordValue;
import io.grpc.stub.StreamObserver;
import java.util.concurrent.CompletableFuture;

/**
 * 流程实例 gRPC 服务实现。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class GrpcProcessInstanceServiceImpl
    extends ProcessInstanceServiceGrpc.ProcessInstanceServiceImplBase implements GrpcService {
  private final BrokerClient brokerClient;
  private final GrpcErrorHandle handle;

  public GrpcProcessInstanceServiceImpl(
      final BrokerClient brokerClient, final GrpcErrorHandle handle) {
    this.brokerClient = brokerClient;
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
    final ProcessInstanceCreateRequest brokerRequest =
        new ProcessInstanceCreateRequest()
            .setProcessDefinitionId(request.getProcessDefinitionId())
            .setProcessDefinitionKey(request.getProcessDefinitionKey(), request.getVersion())
            .setVariable(ensureJsonSet(request.getVariable()));
    final CompletableFuture<BrokerResponse<ProcessInstanceCreateResponseRecord>> future =
        brokerClient.sendRequest(brokerRequest);

    future.whenComplete(
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
    throw new RuntimeException("暂未实现");
  }

  @Override
  public void cancelProcessInstance(
      final ProcessInstanceServiceOuterClass.CancelProcessInstanceRequest request,
      final StreamObserver<ProcessInstanceServiceOuterClass.CancelProcessInstanceResponse>
          responseObserver) {
    final long processInstanceId = request.getProcessInstanceId();
    final ProcessInstanceCancelRequest brokerRequest =
        new ProcessInstanceCancelRequest(processInstanceId);
    final CompletableFuture<BrokerResponse<ProcessInstanceCancelResponseRecordValue>> future =
        brokerClient.sendRequest(brokerRequest);
    future.whenComplete(
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
