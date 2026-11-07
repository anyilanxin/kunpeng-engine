/*
 * Copyright © 2026 anyilanxin zxh (anyilanxin@aliyun.com)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.anyilanxin.kunpeng.client.command.usertask.addsign;

import com.anyilanxin.kunpeng.client.AuthorizationHeaders;
import com.anyilanxin.kunpeng.client.KunpengClientConfiguration;
import com.anyilanxin.kunpeng.client.command.CommandWithTenantStep;
import com.anyilanxin.kunpeng.client.command.CredentialsProvider;
import com.anyilanxin.kunpeng.client.command.FinalCommandStep;
import com.anyilanxin.kunpeng.client.command.JsonMapper;
import com.anyilanxin.kunpeng.client.command.KunpengFuture;
import com.anyilanxin.kunpeng.client.command.RetriableClientFutureImpl;
import com.anyilanxin.kunpeng.client.command.usertask.AddSignMode;
import com.anyilanxin.kunpeng.gateway.grpc.service.UserTaskServiceGrpc;
import com.anyilanxin.kunpeng.gateway.grpc.service.UserTaskServiceOuterClass;
import io.grpc.stub.StreamObserver;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

/**
 * 后加签预登记命令实现。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class AddSignAfterUserTaskCommandImpl
    implements AddSignAfterUserTaskCommand,
        CommandWithTenantStep<AddSignAfterUserTaskCommand>,
        FinalCommandStep<AddSignAfterUserTaskCommandResponse> {

  private static final Duration DEADLINE_OFFSET = Duration.ofSeconds(10);
  private final UserTaskServiceGrpc.UserTaskServiceStub asyncStub;
  private final JsonMapper jsonMapper;
  private final Predicate<CredentialsProvider.StatusCode> retryPredicate;
  private final UserTaskServiceOuterClass.AddSignAfterUserTaskRequest.Builder requestBuilder;
  private final KunpengClientConfiguration config;
  private Duration requestTimeout;
  private final Set<String> defaultTenantIds;
  private final Set<String> customTenantIds;

  public AddSignAfterUserTaskCommandImpl(
      final UserTaskServiceGrpc.UserTaskServiceStub asyncStub,
      final KunpengClientConfiguration config,
      final JsonMapper jsonMapper,
      final Predicate<CredentialsProvider.StatusCode> retryPredicate) {
    this.config = config;
    this.asyncStub = asyncStub;
    this.jsonMapper = jsonMapper;
    this.retryPredicate = retryPredicate;

    requestBuilder = UserTaskServiceOuterClass.AddSignAfterUserTaskRequest.newBuilder();
    requestTimeout(config.getDefaultRequestTimeout());
    defaultTenantIds = new HashSet<>(config.getDefaultJobWorkerTenantIds());
    customTenantIds = new HashSet<>();
  }

  @Override
  public AddSignAfterUserTaskCommand taskId(final long userTaskId) {
    requestBuilder.setUserTaskId(userTaskId);
    return this;
  }

  @Override
  public AddSignAfterUserTaskCommand assignees(final List<String> assignees) {
    requestBuilder.clearAssignees();
    requestBuilder.addAllAssignees(assignees);
    return this;
  }

  @Override
  public AddSignAfterUserTaskCommand addSignMode(final AddSignMode addSignMode) {
    requestBuilder.setAddSignMode(
        addSignMode == AddSignMode.SEQUENTIAL
            ? UserTaskServiceOuterClass.AddSignMode.ADD_SIGN_MODE_SEQUENTIAL
            : UserTaskServiceOuterClass.AddSignMode.ADD_SIGN_MODE_PARALLEL);
    return this;
  }

  @Override
  public AddSignAfterUserTaskCommand tenantId(final String tenantId) {
    requestBuilder.setTenantId(tenantId);
    return this;
  }

  @Override
  public FinalCommandStep<AddSignAfterUserTaskCommandResponse> requestTimeout(
      final Duration requestTimeout) {
    this.requestTimeout = requestTimeout;
    return this;
  }

  @Override
  public KunpengFuture<AddSignAfterUserTaskCommandResponse> send() {
    return sendGrpcRequest();
  }

  private KunpengFuture<AddSignAfterUserTaskCommandResponse> sendGrpcRequest() {
    final UserTaskServiceOuterClass.AddSignAfterUserTaskRequest request = requestBuilder.build();

    final RetriableClientFutureImpl<
            AddSignAfterUserTaskCommandResponse,
            UserTaskServiceOuterClass.AddSignAfterUserTaskResponse>
        future =
            new RetriableClientFutureImpl<>(
                AddSignAfterUserTaskCommandResponseImpl::new,
                retryPredicate,
                streamObserver -> sendGrpcRequest(request, streamObserver));
    sendGrpcRequest(request, future);
    return future;
  }

  private void sendGrpcRequest(
      final UserTaskServiceOuterClass.AddSignAfterUserTaskRequest request,
      final StreamObserver<UserTaskServiceOuterClass.AddSignAfterUserTaskResponse> streamObserver) {
    AuthorizationHeaders.attach(asyncStub, null, null, config)
        .withDeadlineAfter(requestTimeout.toMillis(), TimeUnit.MILLISECONDS)
        .addSignAfterUserTask(request, streamObserver);
  }
}
