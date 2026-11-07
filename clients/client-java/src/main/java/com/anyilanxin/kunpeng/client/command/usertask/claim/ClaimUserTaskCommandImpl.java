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
package com.anyilanxin.kunpeng.client.command.usertask.claim;

import com.anyilanxin.kunpeng.client.AuthorizationHeaders;
import com.anyilanxin.kunpeng.client.KunpengClientConfiguration;
import com.anyilanxin.kunpeng.client.command.*;
import com.anyilanxin.kunpeng.gateway.grpc.service.UserTaskServiceGrpc;
import com.anyilanxin.kunpeng.gateway.grpc.service.UserTaskServiceOuterClass;
import io.grpc.stub.StreamObserver;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

/**
 * 签收用户任务命令实现：授权头经 AuthorizationHeaders 注入（显式/provider，发送线程解析）。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class ClaimUserTaskCommandImpl implements ClaimUserTaskCommand {
  private final UserTaskServiceGrpc.UserTaskServiceStub asyncStub;
  private final KunpengClientConfiguration config;
  private final Predicate<CredentialsProvider.StatusCode> retryPredicate;
  private final UserTaskServiceOuterClass.ClaimUserTaskRequest.Builder requestBuilder;
  private final Set<String> defaultTenantIds;
  private final Set<String> customTenantIds;
  private Duration requestTimeout;

  public ClaimUserTaskCommandImpl(
      final UserTaskServiceGrpc.UserTaskServiceStub asyncStub,
      final KunpengClientConfiguration config,
      final Predicate<CredentialsProvider.StatusCode> retryPredicate) {
    this.config = config;
    this.asyncStub = asyncStub;
    this.retryPredicate = retryPredicate;
    requestBuilder = UserTaskServiceOuterClass.ClaimUserTaskRequest.newBuilder();
    requestTimeout(config.getDefaultRequestTimeout());
    defaultTenantIds = new HashSet<>(config.getDefaultJobWorkerTenantIds());
    customTenantIds = new HashSet<>();
  }

  @Override
  public FinalCommandStep<ClaimUserTaskCommandResponse> requestTimeout(
      final Duration requestTimeout) {
    this.requestTimeout = requestTimeout;
    return this;
  }

  @Override
  public KunpengFuture<ClaimUserTaskCommandResponse> send() {
    final UserTaskServiceOuterClass.ClaimUserTaskRequest request = requestBuilder.build();
    final RetriableClientFutureImpl<
            ClaimUserTaskCommandResponse, UserTaskServiceOuterClass.ClaimUserTaskResponse>
        future =
            new RetriableClientFutureImpl<>(
                ClaimUserTaskCommandResponseImpl::new,
                retryPredicate,
                streamObserver -> sendGrpcRequest(request, streamObserver));
    sendGrpcRequest(request, future);
    return future;
  }

  private void sendGrpcRequest(
      final UserTaskServiceOuterClass.ClaimUserTaskRequest request,
      final StreamObserver<UserTaskServiceOuterClass.ClaimUserTaskResponse> streamObserver) {
    AuthorizationHeaders.attach(asyncStub, null, null, config)
        .withDeadlineAfter(requestTimeout.toMillis(), TimeUnit.MILLISECONDS)
        .claimUserTask(request, streamObserver);
  }

  @Override
  public ClaimUserTaskCommand taskId(final long userTaskId) {
    requestBuilder.setUserTaskId(userTaskId);
    return this;
  }

  @Override
  public ClaimUserTaskCommand tenantId(final String tenantId) {
    requestBuilder.setTenantId(tenantId);
    return this;
  }
}
