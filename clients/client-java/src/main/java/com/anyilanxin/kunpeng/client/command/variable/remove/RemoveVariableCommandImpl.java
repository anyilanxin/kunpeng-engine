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
package com.anyilanxin.kunpeng.client.command.variable.remove;

import com.anyilanxin.kunpeng.client.KunpengClientConfiguration;
import com.anyilanxin.kunpeng.client.command.CredentialsProvider;
import com.anyilanxin.kunpeng.client.command.FinalCommandStep;
import com.anyilanxin.kunpeng.client.command.KunpengFuture;
import com.anyilanxin.kunpeng.client.command.RetriableClientFutureImpl;
import com.anyilanxin.kunpeng.gateway.grpc.service.VariableServiceGrpc;
import com.anyilanxin.kunpeng.gateway.grpc.service.VariableServiceOuterClass;
import io.grpc.stub.StreamObserver;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

public final class RemoveVariableCommandImpl
    implements RemoveVariableCommand,
        RemoveVariableCommand.RemoveVariableCommandStep1,
        RemoveVariableCommand.RemoveVariableCommandStep2,
        RemoveVariableCommand.RemoveVariableCommandStep3 {
  private final VariableServiceGrpc.VariableServiceStub asyncStub;
  private final Predicate<CredentialsProvider.StatusCode> retryPredicate;
  private final VariableServiceOuterClass.VariableRemoveRequest.Builder requestBuilder;
  private Duration requestTimeout;

  public RemoveVariableCommandImpl(
      final VariableServiceGrpc.VariableServiceStub asyncStub,
      final KunpengClientConfiguration config,
      final Predicate<CredentialsProvider.StatusCode> retryPredicate) {
    this.asyncStub = asyncStub;
    this.retryPredicate = retryPredicate;

    requestBuilder = VariableServiceOuterClass.VariableRemoveRequest.newBuilder();
    requestTimeout(config.getDefaultRequestTimeout());
  }

  @Override
  public RemoveVariableCommandStep1 taskId(long userTaskId) {
    requestBuilder.setTaskId(userTaskId);
    return this;
  }

  @Override
  public RemoveVariableCommandStep1 processInstanceId(long processInstanceId) {
    requestBuilder.setProcessInstanceId(processInstanceId);
    return this;
  }

  @Override
  public RemoveVariableCommandStep1 activityInstanceId(long activityInstanceId) {
    requestBuilder.setActivityInstanceId(activityInstanceId);
    return this;
  }

  @Override
  public FinalCommandStep<RemoveVariableCommandResponse> requestTimeout(Duration requestTimeout) {
    this.requestTimeout = requestTimeout;
    return this;
  }

  @Override
  public KunpengFuture<RemoveVariableCommandResponse> send() {
    return sendGrpcRequest();
  }

  private KunpengFuture<RemoveVariableCommandResponse> sendGrpcRequest() {
    final VariableServiceOuterClass.VariableRemoveRequest request = requestBuilder.build();
    final RetriableClientFutureImpl<
            RemoveVariableCommandResponse, VariableServiceOuterClass.VariableRemoveResponse>
        future =
            new RetriableClientFutureImpl<>(
                RemoveVariableCommandResponseImpl::new,
                retryPredicate,
                streamObserver -> sendGrpcRequest(request, streamObserver));
    sendGrpcRequest(request, future);
    return future;
  }

  private void sendGrpcRequest(
      final VariableServiceOuterClass.VariableRemoveRequest request,
      final StreamObserver<VariableServiceOuterClass.VariableRemoveResponse> streamObserver) {
    asyncStub
        .withDeadlineAfter(requestTimeout.toMillis(), TimeUnit.MILLISECONDS)
        .variableRemove(request, streamObserver);
  }

  @Override
  public RemoveVariableCommandStep2 removeAll() {
    requestBuilder.setRemoveAll(true);
    requestBuilder.clearRemoveVariable();
    return this;
  }

  @Override
  public RemoveVariableCommandStep3 remove(String variableKey) {
    requestBuilder.setRemoveAll(false);
    requestBuilder.addRemoveVariable(variableKey);
    return this;
  }

  @Override
  public RemoveVariableCommandStep3 removes(List<String> variableKey) {
    requestBuilder.setRemoveAll(false);
    if (variableKey != null && !variableKey.isEmpty()) {
      variableKey.forEach(requestBuilder::addRemoveVariable);
    }

    return this;
  }
}
