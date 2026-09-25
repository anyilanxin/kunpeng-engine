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
package com.anyilanxin.kunpeng.client.command.usertask.complete;

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

public final class CompleteUserTaskCommandImpl
    extends CommandWithVariables2<CompleteUserTaskCommandImpl>
    implements CompleteUserTaskCommand, CompleteUserTaskCommand.CompleteUserTaskCommandStep1 {

  private static final Duration DEADLINE_OFFSET = Duration.ofSeconds(10);
  private final UserTaskServiceGrpc.UserTaskServiceStub asyncStub;
  private final JsonMapper jsonMapper;
  private final Predicate<CredentialsProvider.StatusCode> retryPredicate;
  private final UserTaskServiceOuterClass.CompleteUserTaskRequest.Builder requestBuilder;
  private final KunpengClientConfiguration config;
  private Duration requestTimeout;
  private final Set<String> defaultTenantIds;
  private final Set<String> customTenantIds;

  public CompleteUserTaskCommandImpl(
      final UserTaskServiceGrpc.UserTaskServiceStub asyncStub,
      final KunpengClientConfiguration config,
      final JsonMapper jsonMapper,
      final Predicate<CredentialsProvider.StatusCode> retryPredicate) {
    super(jsonMapper);
    this.config = config;
    this.asyncStub = asyncStub;
    this.jsonMapper = jsonMapper;
    this.retryPredicate = retryPredicate;

    requestBuilder = UserTaskServiceOuterClass.CompleteUserTaskRequest.newBuilder();
    requestTimeout(config.getDefaultRequestTimeout());
    defaultTenantIds = new HashSet<>(config.getDefaultJobWorkerTenantIds());
    customTenantIds = new HashSet<>();
  }

  @Override
  public CompleteUserTaskCommandStep1 tenantId(final String tenantId) {
    return this;
  }

  @Override
  protected CompleteUserTaskCommandImpl setVariablesInternal(final String variables) {
    requestBuilder.setVariable(variables);
    return this;
  }

  @Override
  public FinalCommandStep<CompleteUserTaskCommandResponse> requestTimeout(
      final Duration requestTimeout) {
    this.requestTimeout = requestTimeout;
    return this;
  }

  @Override
  public KunpengFuture<CompleteUserTaskCommandResponse> send() {
    return sendGrpcRequest();
  }

  private KunpengFuture<CompleteUserTaskCommandResponse> sendGrpcRequest() {
    final UserTaskServiceOuterClass.CompleteUserTaskRequest request = requestBuilder.build();

    final RetriableClientFutureImpl<
            CompleteUserTaskCommandResponse, UserTaskServiceOuterClass.CompleteUserTaskResponse>
        future =
            new RetriableClientFutureImpl<>(
                CompleteUserTaskCommandResponseImpl::new,
                retryPredicate,
                streamObserver -> sendGrpcRequest(request, streamObserver));
    sendGrpcRequest(request, future);
    return future;
  }

  private void sendGrpcRequest(
      final UserTaskServiceOuterClass.CompleteUserTaskRequest request,
      final StreamObserver<UserTaskServiceOuterClass.CompleteUserTaskResponse> streamObserver) {
    asyncStub
        .withDeadlineAfter(requestTimeout.toMillis(), TimeUnit.MILLISECONDS)
        .completeUserTask(request, streamObserver);
  }

  @Override
  public CompleteUserTaskCommandStep1 taskId(final long userTaskId) {
    requestBuilder.setUserTaskId(userTaskId);
    return this;
  }
}
