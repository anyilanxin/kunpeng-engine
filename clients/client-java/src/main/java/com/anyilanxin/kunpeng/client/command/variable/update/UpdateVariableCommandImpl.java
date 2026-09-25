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
package com.anyilanxin.kunpeng.client.command.variable.update;

import com.anyilanxin.kunpeng.client.KunpengClientConfiguration;
import com.anyilanxin.kunpeng.client.command.*;
import com.anyilanxin.kunpeng.gateway.grpc.service.VariableServiceGrpc;
import com.anyilanxin.kunpeng.gateway.grpc.service.VariableServiceOuterClass;
import io.grpc.stub.StreamObserver;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

public final class UpdateVariableCommandImpl
    extends CommandWithVariables2<UpdateVariableCommandImpl>
    implements UpdateVariableCommand, UpdateVariableCommand.UpdateVariableCommandStep1 {

  private static final Duration DEADLINE_OFFSET = Duration.ofSeconds(10);
  private final VariableServiceGrpc.VariableServiceStub asyncStub;
  private final Predicate<CredentialsProvider.StatusCode> retryPredicate;
  private final VariableServiceOuterClass.VariableUpdateRequest.Builder requestBuilder;
  private Duration requestTimeout;

  public UpdateVariableCommandImpl(
      final VariableServiceGrpc.VariableServiceStub asyncStub,
      final KunpengClientConfiguration config,
      final JsonMapper jsonMapper,
      final Predicate<CredentialsProvider.StatusCode> retryPredicate) {
    super(jsonMapper);
    this.asyncStub = asyncStub;
    this.retryPredicate = retryPredicate;

    requestBuilder = VariableServiceOuterClass.VariableUpdateRequest.newBuilder();
    requestTimeout(config.getDefaultRequestTimeout());
  }

  @Override
  protected UpdateVariableCommandImpl setVariablesInternal(final String variables) {
    requestBuilder.setVariable(variables);
    return this;
  }

  @Override
  public FinalCommandStep<UpdateVariableCommandResponse> requestTimeout(
      final Duration requestTimeout) {
    this.requestTimeout = requestTimeout;
    return this;
  }

  @Override
  public KunpengFuture<UpdateVariableCommandResponse> send() {
    return sendGrpcRequest();
  }

  private KunpengFuture<UpdateVariableCommandResponse> sendGrpcRequest() {
    final VariableServiceOuterClass.VariableUpdateRequest request = requestBuilder.build();

    final RetriableClientFutureImpl<
            UpdateVariableCommandResponse, VariableServiceOuterClass.VariableUpdateResponse>
        future =
            new RetriableClientFutureImpl<>(
                UpdateVariableCommandResponseImpl::new,
                retryPredicate,
                streamObserver -> sendGrpcRequest(request, streamObserver));
    sendGrpcRequest(request, future);
    return future;
  }

  private void sendGrpcRequest(
      final VariableServiceOuterClass.VariableUpdateRequest request,
      final StreamObserver<VariableServiceOuterClass.VariableUpdateResponse> streamObserver) {
    asyncStub
        .withDeadlineAfter(requestTimeout.toMillis(), TimeUnit.MILLISECONDS)
        .variableUpdate(request, streamObserver);
  }

  @Override
  public UpdateVariableCommandStep1 taskId(final long userTaskId) {
    requestBuilder.setTaskId(userTaskId);
    return this;
  }

  @Override
  public UpdateVariableCommandStep1 processInstanceId(long processInstanceId) {
    requestBuilder.setProcessInstanceId(processInstanceId);
    return this;
  }

  @Override
  public UpdateVariableCommandStep1 activityInstanceId(long activityInstanceId) {
    requestBuilder.setActivityInstanceId(activityInstanceId);
    return this;
  }
}
