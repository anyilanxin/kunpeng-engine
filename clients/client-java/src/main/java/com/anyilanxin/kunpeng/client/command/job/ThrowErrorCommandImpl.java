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
package com.anyilanxin.kunpeng.client.command.job;

import com.anyilanxin.kunpeng.client.command.*;
import com.anyilanxin.kunpeng.gateway.grpc.service.JobServiceGrpc;
import com.anyilanxin.kunpeng.gateway.grpc.service.JobServiceOuterClass;
import io.grpc.stub.StreamObserver;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

public final class ThrowErrorCommandImpl
    extends CommandWithVariables2<ThrowErrorCommandStep1.ThrowErrorCommandStep2>
    implements ThrowErrorCommandStep1, ThrowErrorCommandStep1.ThrowErrorCommandStep2 {

  private final JobServiceGrpc.JobServiceStub asyncStub;
  private final JobServiceOuterClass.ThrowErrorRequest.Builder grpcRequestObjectBuilder;
  private final Predicate<CredentialsProvider.StatusCode> retryPredicate;
  private Duration requestTimeout;
  private final long jobKey;

  public ThrowErrorCommandImpl(
      final JobServiceGrpc.JobServiceStub asyncStub,
      final JsonMapper jsonMapper,
      final long key,
      final Duration requestTimeout,
      final Predicate<CredentialsProvider.StatusCode> retryPredicate) {
    super(jsonMapper);
    this.asyncStub = asyncStub;
    this.requestTimeout = requestTimeout;
    this.retryPredicate = retryPredicate;
    grpcRequestObjectBuilder = JobServiceOuterClass.ThrowErrorRequest.newBuilder();
    grpcRequestObjectBuilder.setJobKey(key);
    jobKey = key;
  }

  @Override
  public ThrowErrorCommandStep2 errorCode(final String errorCode) {
    grpcRequestObjectBuilder.setErrorCode(errorCode);
    return this;
  }

  @Override
  public ThrowErrorCommandStep2 errorMessage(final String errorMsg) {
    grpcRequestObjectBuilder.setErrorMessage(errorMsg);
    return this;
  }

  @Override
  public ThrowErrorCommandStep2 setVariablesInternal(final String variables) {
    grpcRequestObjectBuilder.setVariables(variables);
    return this;
  }

  @Override
  public FinalCommandStep<Void> requestTimeout(final Duration requestTimeout) {
    this.requestTimeout = requestTimeout;
    return this;
  }

  @Override
  public KunpengFuture<Void> send() {
    return sendGrpcRequest();
  }

  private KunpengFuture<Void> sendGrpcRequest() {
    final JobServiceOuterClass.ThrowErrorRequest request = grpcRequestObjectBuilder.build();

    final RetriableClientFutureImpl<Void, JobServiceOuterClass.ThrowErrorResponse> future =
        new RetriableClientFutureImpl<>(
            retryPredicate, streamObserver -> sendGrpcRequest(request, streamObserver));

    sendGrpcRequest(request, future);
    return future;
  }

  private void sendGrpcRequest(
      final JobServiceOuterClass.ThrowErrorRequest request,
      final StreamObserver<JobServiceOuterClass.ThrowErrorResponse> streamObserver) {
    asyncStub
        .withDeadlineAfter(requestTimeout.toMillis(), TimeUnit.MILLISECONDS)
        .throwError(request, streamObserver);
  }
}
