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
import com.anyilanxin.kunpeng.broker.client.business.commandapi.variable.request.VariableRemoveRequest;
import com.anyilanxin.kunpeng.broker.client.business.commandapi.variable.request.VariableUpdateRequest;
import com.anyilanxin.kunpeng.gateway.grpc.GrpcErrorHandle;
import com.anyilanxin.kunpeng.gateway.grpc.service.GrpcService;
import com.anyilanxin.kunpeng.gateway.grpc.service.VariableServiceGrpc;
import com.anyilanxin.kunpeng.gateway.grpc.service.VariableServiceOuterClass;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.variable.remove.VariableRemoveResponseRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.variable.update.VariableUpdateResponseRecord;
import io.grpc.stub.StreamObserver;
import java.util.concurrent.CompletableFuture;

/**
 * 变量 gRPC 服务实现。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class GrpcVariableServiceImpl extends VariableServiceGrpc.VariableServiceImplBase
    implements GrpcService {
  private final BrokerClient brokerClient;
  private final GrpcErrorHandle handle;

  public GrpcVariableServiceImpl(final BrokerClient brokerClient, final GrpcErrorHandle handle) {
    this.brokerClient = brokerClient;
    this.handle = handle;
  }

  @Override
  public String getServiceName() {
    return "Variable Service";
  }

  @Override
  public void variableUpdate(
      final VariableServiceOuterClass.VariableUpdateRequest request,
      final StreamObserver<VariableServiceOuterClass.VariableUpdateResponse> responseObserver) {
    final VariableUpdateRequest brokerRequest = new VariableUpdateRequest();
    brokerRequest
        .setVariable(ensureJsonSet(request.getVariable()))
        .setActivityInstanceId(request.getActivityInstanceId())
        .setProcessInstanceId(request.getProcessInstanceId())
        .setTaskId(request.getTaskId());
    final CompletableFuture<BrokerResponse<VariableUpdateResponseRecord>> future =
        brokerClient.sendRequest(brokerRequest);
    future.whenComplete(
        (brokerResponse, throwable) -> {
          if (throwable != null) {
            responseObserver.onError(handle.error(responseObserver, throwable));
          } else {
            if (brokerResponse.isSuccess()) {
              final VariableServiceOuterClass.VariableUpdateResponse response =
                  VariableServiceOuterClass.VariableUpdateResponse.newBuilder().build();
              responseObserver.onNext(response);
              responseObserver.onCompleted();
            } else {
              responseObserver.onError(handle.error(responseObserver, brokerResponse));
            }
          }
        });
  }

  @Override
  public void variableRemove(
      final VariableServiceOuterClass.VariableRemoveRequest request,
      final StreamObserver<VariableServiceOuterClass.VariableRemoveResponse> responseObserver) {
    final VariableRemoveRequest brokerRequest = new VariableRemoveRequest();
    brokerRequest
        .setRemoveVariable(request.getRemoveVariableList().stream().toList())
        .setActivityInstanceId(request.getActivityInstanceId())
        .setProcessInstanceId(request.getProcessInstanceId())
        .setTaskId(request.getTaskId());
    final CompletableFuture<BrokerResponse<VariableRemoveResponseRecord>> future =
        brokerClient.sendRequest(brokerRequest);
    future.whenComplete(
        (brokerResponse, throwable) -> {
          if (throwable != null) {
            responseObserver.onError(handle.error(responseObserver, throwable));
          } else {
            if (brokerResponse.isSuccess()) {
              final VariableServiceOuterClass.VariableRemoveResponse response =
                  VariableServiceOuterClass.VariableRemoveResponse.newBuilder().build();
              responseObserver.onNext(response);
              responseObserver.onCompleted();
            } else {
              responseObserver.onError(handle.error(responseObserver, brokerResponse));
            }
          }
        });
  }
}
