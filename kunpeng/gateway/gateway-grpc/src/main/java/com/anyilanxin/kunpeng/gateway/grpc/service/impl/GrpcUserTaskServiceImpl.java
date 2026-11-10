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

import com.anyilanxin.kunpeng.broker.client.business.client.UserTaskClient;
import com.anyilanxin.kunpeng.broker.client.business.commandapi.usertask.request.AddSignAfterRequest;
import com.anyilanxin.kunpeng.broker.client.business.commandapi.usertask.request.AddSignBeforeRequest;
import com.anyilanxin.kunpeng.broker.client.business.commandapi.usertask.request.UserTaskCancelRequest;
import com.anyilanxin.kunpeng.broker.client.business.commandapi.usertask.request.UserTaskClaimRequest;
import com.anyilanxin.kunpeng.broker.client.business.commandapi.usertask.request.UserTaskCompleteRequest;
import com.anyilanxin.kunpeng.gateway.grpc.AuthorizationMetadataContext;
import com.anyilanxin.kunpeng.gateway.grpc.GrpcErrorHandle;
import com.anyilanxin.kunpeng.gateway.grpc.service.GrpcService;
import com.anyilanxin.kunpeng.gateway.grpc.service.UserTaskServiceGrpc;
import com.anyilanxin.kunpeng.gateway.grpc.service.UserTaskServiceOuterClass;
import com.anyilanxin.kunpeng.protocol.business.record.command.usertask.AddSignMode;
import io.grpc.stub.StreamObserver;

/**
 * 用户任务 gRPC 服务实现。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class GrpcUserTaskServiceImpl extends UserTaskServiceGrpc.UserTaskServiceImplBase
    implements GrpcService {
  private final UserTaskClient commands;
  private final GrpcErrorHandle handle;

  public GrpcUserTaskServiceImpl(final UserTaskClient commands, final GrpcErrorHandle handle) {
    this.commands = commands;
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
    AuthorizationMetadataContext.attachTo(brokerRequest);
    // 完成携带后加签：名单非空时模式必须显式（NONE 由引擎拒绝）
    if (request.getAddSignAssigneesCount() > 0) {
      brokerRequest
          .setAddSignAssignees(request.getAddSignAssigneesList())
          .setAddSignMode(addSignMode(request.getAddSignMode()));
    }
    commands
        .completeUserTask(brokerRequest)
        .whenComplete(
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
    AuthorizationMetadataContext.attachTo(brokerRequest);
    commands
        .cancelUserTask(brokerRequest)
        .whenComplete(
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

  @Override
  public void claimUserTask(
      final UserTaskServiceOuterClass.ClaimUserTaskRequest request,
      final StreamObserver<UserTaskServiceOuterClass.ClaimUserTaskResponse> responseObserver) {
    // 签收人不随请求传递：授权元数据操作人身份由引擎侧设置
    final UserTaskClaimRequest brokerRequest = new UserTaskClaimRequest(request.getUserTaskId());
    AuthorizationMetadataContext.attachTo(brokerRequest);
    commands
        .claimUserTask(brokerRequest)
        .whenComplete(
            (brokerResponse, throwable) -> {
              if (throwable != null) {
                responseObserver.onError(handle.error(responseObserver, throwable));
              } else {
                if (brokerResponse.isSuccess()) {
                  responseObserver.onNext(
                      UserTaskServiceOuterClass.ClaimUserTaskResponse.newBuilder().build());
                  responseObserver.onCompleted();
                } else {
                  responseObserver.onError(handle.error(responseObserver, brokerResponse));
                }
              }
            });
  }

  @Override
  public void addSignBeforeUserTask(
      final UserTaskServiceOuterClass.AddSignBeforeUserTaskRequest request,
      final StreamObserver<UserTaskServiceOuterClass.AddSignBeforeUserTaskResponse>
          responseObserver) {
    final AddSignBeforeRequest brokerRequest =
        new AddSignBeforeRequest(request.getUserTaskId())
            .setAssignees(request.getAssigneesList())
            .setAddSignMode(addSignMode(request.getAddSignMode()));
    AuthorizationMetadataContext.attachTo(brokerRequest);
    commands
        .addSignBeforeUserTask(brokerRequest)
        .whenComplete(
            (brokerResponse, throwable) -> {
              if (throwable != null) {
                responseObserver.onError(handle.error(responseObserver, throwable));
              } else {
                if (brokerResponse.isSuccess()) {
                  responseObserver.onNext(
                      UserTaskServiceOuterClass.AddSignBeforeUserTaskResponse.newBuilder().build());
                  responseObserver.onCompleted();
                } else {
                  responseObserver.onError(handle.error(responseObserver, brokerResponse));
                }
              }
            });
  }

  @Override
  public void addSignAfterUserTask(
      final UserTaskServiceOuterClass.AddSignAfterUserTaskRequest request,
      final StreamObserver<UserTaskServiceOuterClass.AddSignAfterUserTaskResponse>
          responseObserver) {
    final AddSignAfterRequest brokerRequest =
        new AddSignAfterRequest(request.getUserTaskId())
            .setAssignees(request.getAssigneesList())
            .setAddSignMode(addSignMode(request.getAddSignMode()));
    AuthorizationMetadataContext.attachTo(brokerRequest);
    commands
        .addSignAfterUserTask(brokerRequest)
        .whenComplete(
            (brokerResponse, throwable) -> {
              if (throwable != null) {
                responseObserver.onError(handle.error(responseObserver, throwable));
              } else {
                if (brokerResponse.isSuccess()) {
                  responseObserver.onNext(
                      UserTaskServiceOuterClass.AddSignAfterUserTaskResponse.newBuilder().build());
                  responseObserver.onCompleted();
                } else {
                  responseObserver.onError(handle.error(responseObserver, brokerResponse));
                }
              }
            });
  }

  private AddSignMode addSignMode(final UserTaskServiceOuterClass.AddSignMode mode) {
    return switch (mode) {
      case ADD_SIGN_MODE_PARALLEL -> AddSignMode.PARALLEL;
      case ADD_SIGN_MODE_SEQUENTIAL -> AddSignMode.SEQUENTIAL;
      default -> AddSignMode.NONE;
    };
  }
}
