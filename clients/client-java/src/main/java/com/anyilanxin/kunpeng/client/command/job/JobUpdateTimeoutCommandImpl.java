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

public class JobUpdateTimeoutCommandImpl
    implements UpdateTimeoutJobCommandStep1,
        UpdateTimeoutJobCommandStep1.UpdateTimeoutJobCommandStep2 {

  private final JobServiceGrpc.JobServiceStub asyncStub;
  private final JobServiceOuterClass.UpdateJobTimeoutRequest.Builder grpcRequestObjectBuilder;
  private final Predicate<CredentialsProvider.StatusCode> retryPredicate;
  private Duration requestTimeout;

  public JobUpdateTimeoutCommandImpl(
      final JobServiceGrpc.JobServiceStub asyncStub,
      final long jobKey,
      final Duration requestTimeout,
      final Predicate<CredentialsProvider.StatusCode> retryPredicate,
      final JsonMapper jsonMapper) {
    this.asyncStub = asyncStub;
    this.requestTimeout = requestTimeout;
    this.retryPredicate = retryPredicate;
    grpcRequestObjectBuilder = JobServiceOuterClass.UpdateJobTimeoutRequest.newBuilder();
    grpcRequestObjectBuilder.setJobKey(jobKey);
  }

  @Override
  public UpdateTimeoutJobCommandStep2 timeout(final long timeout) {
    grpcRequestObjectBuilder.setTimeout(timeout);
    return this;
  }

  @Override
  public UpdateTimeoutJobCommandStep2 timeout(final Duration timeout) {
    return timeout(timeout.toMillis());
  }

  @Override
  public FinalCommandStep<UpdateTimeoutJobResponse> requestTimeout(final Duration requestTimeout) {
    this.requestTimeout = requestTimeout;
    return this;
  }

  @Override
  public KunpengFuture<UpdateTimeoutJobResponse> send() {
    return sendGrpcRequest();
  }

  private KunpengFuture<UpdateTimeoutJobResponse> sendGrpcRequest() {
    final JobServiceOuterClass.UpdateJobTimeoutRequest request = grpcRequestObjectBuilder.build();

    final RetriableClientFutureImpl<
            UpdateTimeoutJobResponse, JobServiceOuterClass.UpdateJobTimeoutResponse>
        future =
            new RetriableClientFutureImpl<>(
                UpdateTimeoutJobResponseImpl::new,
                retryPredicate,
                streamObserver -> sendGrpcRequest(request, streamObserver));

    sendGrpcRequest(request, future);
    return future;
  }

  private void sendGrpcRequest(
      final JobServiceOuterClass.UpdateJobTimeoutRequest request,
      final StreamObserver<JobServiceOuterClass.UpdateJobTimeoutResponse> streamObserver) {
    asyncStub
        .withDeadlineAfter(requestTimeout.toMillis(), TimeUnit.MILLISECONDS)
        .updateJobTimeout(request, streamObserver);
  }

  @Override
  public UpdateTimeoutJobCommandStep2 operationReference(final long operationReference) {
    grpcRequestObjectBuilder.setOperationReference(operationReference);
    return this;
  }
}
