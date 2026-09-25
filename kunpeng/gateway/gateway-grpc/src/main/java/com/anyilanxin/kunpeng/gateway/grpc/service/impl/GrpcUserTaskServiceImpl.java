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
import com.anyilanxin.kunpeng.broker.client.business.commandapi.usertask.request.UserTaskCancelRequest;
import com.anyilanxin.kunpeng.broker.client.business.commandapi.usertask.request.UserTaskCompleteRequest;
import com.anyilanxin.kunpeng.gateway.grpc.GrpcErrorHandle;
import com.anyilanxin.kunpeng.gateway.grpc.service.GrpcService;
import com.anyilanxin.kunpeng.gateway.grpc.service.UserTaskServiceGrpc;
import com.anyilanxin.kunpeng.gateway.grpc.service.UserTaskServiceOuterClass;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.usertask.cancel.UserTaskCancelResponseRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.usertask.complete.UserTaskCompleteResponseRecord;
import io.grpc.stub.StreamObserver;
import java.util.concurrent.CompletableFuture;

/**
 * 用户任务 gRPC 服务实现。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class GrpcUserTaskServiceImpl extends UserTaskServiceGrpc.UserTaskServiceImplBase
    implements GrpcService {
  private final BrokerClient brokerClient;
  private final GrpcErrorHandle handle;

  public GrpcUserTaskServiceImpl(final BrokerClient brokerClient, final GrpcErrorHandle handle) {
    this.brokerClient = brokerClient;
    this.handle = handle;
  }

  @Override
  public String getServiceName() {
    return "User Task Service";
  }

  @Override
  public void completeUserTask(
      final UserTaskServiceOuterClass.CompleteUserTaskRequest request,
      final StreamObserver<UserTaskServiceOuterClass.CompleteUserTaskResponse> responseObserver) {
    final long userTaskId = request.getUserTaskId();
    final UserTaskCompleteRequest brokerRequest = new UserTaskCompleteRequest(userTaskId);
    brokerRequest.setVariable(ensureJsonSet(request.getVariable()));
    final CompletableFuture<BrokerResponse<UserTaskCompleteResponseRecord>> future =
        brokerClient.sendRequest(brokerRequest);
    future.whenComplete(
        (brokerResponse, throwable) -> {
          if (throwable != null) {
            responseObserver.onError(handle.error(responseObserver, throwable));
          } else {
            if (brokerResponse.isSuccess()) {
              final UserTaskServiceOuterClass.CompleteUserTaskResponse response =
                  UserTaskServiceOuterClass.CompleteUserTaskResponse.newBuilder().build();
              responseObserver.onNext(response);
              responseObserver.onCompleted();
            } else {
              responseObserver.onError(handle.error(responseObserver, brokerResponse));
            }
          }
        });
  }

  @Override
  public void cancelUserTask(
      final UserTaskServiceOuterClass.CancelUserTaskRequest request,
      final StreamObserver<UserTaskServiceOuterClass.CancelUserTaskResponse> responseObserver) {
    final long userTaskId = request.getUserTaskId();
    final UserTaskCancelRequest brokerRequest = new UserTaskCancelRequest(userTaskId);
    final CompletableFuture<BrokerResponse<UserTaskCancelResponseRecord>> future =
        brokerClient.sendRequest(brokerRequest);
    future.whenComplete(
        (brokerResponse, throwable) -> {
          if (throwable != null) {
            responseObserver.onError(handle.error(responseObserver, throwable));
          } else {
            if (brokerResponse.isSuccess()) {
              final UserTaskServiceOuterClass.CancelUserTaskResponse response =
                  UserTaskServiceOuterClass.CancelUserTaskResponse.newBuilder().build();
              responseObserver.onNext(response);
              responseObserver.onCompleted();
            } else {
              responseObserver.onError(handle.error(responseObserver, brokerResponse));
            }
          }
        });
  }
}
