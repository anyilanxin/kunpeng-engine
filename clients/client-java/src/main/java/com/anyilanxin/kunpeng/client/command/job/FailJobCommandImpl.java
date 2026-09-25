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

public final class FailJobCommandImpl
    extends CommandWithVariables2<FailJobCommandStep1.FailJobCommandStep2>
    implements FailJobCommandStep1, FailJobCommandStep1.FailJobCommandStep2 {

  private final JobServiceGrpc.JobServiceStub asyncStub;
  private final JobServiceOuterClass.FailJobRequest.Builder grpcRequestObjectBuilder;
  private final Predicate<CredentialsProvider.StatusCode> retryPredicate;
  private final Duration requestTimeout;

  public FailJobCommandImpl(
      final JobServiceGrpc.JobServiceStub asyncStub,
      final JsonMapper jsonMapper,
      final long key,
      final Duration requestTimeout,
      final Predicate<CredentialsProvider.StatusCode> retryPredicate) {
    super(jsonMapper);
    this.asyncStub = asyncStub;
    this.retryPredicate = retryPredicate;
    grpcRequestObjectBuilder = JobServiceOuterClass.FailJobRequest.newBuilder();
    grpcRequestObjectBuilder.setJobKey(key);
    this.requestTimeout = requestTimeout;
  }

  @Override
  public FailJobCommandStep2 retries(final int retries) {
    grpcRequestObjectBuilder.setRetries(retries);
    return this;
  }

  @Override
  public FailJobCommandStep2 retryBackoff(final Duration backoffTimeout) {
    grpcRequestObjectBuilder.setRetryBackOff(backoffTimeout.toMillis());
    return this;
  }

  @Override
  public FailJobCommandStep2 errorMessage(final String errorMsg) {
    grpcRequestObjectBuilder.setErrorMessage(errorMsg);
    return this;
  }

  @Override
  public FailJobCommandStep2 setVariablesInternal(final String variables) {
    grpcRequestObjectBuilder.setVariables(variables);
    return this;
  }

  @Override
  public FinalCommandStep<FailJobResponse> requestTimeout(final Duration requestTimeout) {
    return this;
  }

  @Override
  public KunpengFuture<FailJobResponse> send() {
    return sendGrpcRequest();
  }

  private KunpengFuture<FailJobResponse> sendGrpcRequest() {
    final JobServiceOuterClass.FailJobRequest request = grpcRequestObjectBuilder.build();

    final RetriableClientFutureImpl<FailJobResponse, JobServiceOuterClass.FailJobResponse> future =
        new RetriableClientFutureImpl<>(
            FailJobResponseImpl::new,
            retryPredicate,
            streamObserver -> sendGrpcRequest(request, streamObserver));

    sendGrpcRequest(request, future);
    return future;
  }

  private void sendGrpcRequest(
      final JobServiceOuterClass.FailJobRequest request,
      final StreamObserver<JobServiceOuterClass.FailJobResponse> streamObserver) {
    asyncStub
        .withDeadlineAfter(requestTimeout.toMillis(), TimeUnit.MILLISECONDS)
        .failJob(request, streamObserver);
  }
}
