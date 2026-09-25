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

public final class JobUpdateRetriesCommandImpl
    implements UpdateRetriesJobCommandStep1,
        UpdateRetriesJobCommandStep1.UpdateRetriesJobCommandStep2 {

  private final JobServiceGrpc.JobServiceStub asyncStub;
  private final JobServiceOuterClass.UpdateJobRetriesRequest.Builder grpcRequestObjectBuilder;
  private final Predicate<CredentialsProvider.StatusCode> retryPredicate;
  private Duration requestTimeout;

  public JobUpdateRetriesCommandImpl(
      final JobServiceGrpc.JobServiceStub asyncStub,
      final Duration requestTimeout,
      final Predicate<CredentialsProvider.StatusCode> retryPredicate,
      final JsonMapper jsonMapper,
      final long jobKey) {
    this.asyncStub = asyncStub;
    this.requestTimeout = requestTimeout;
    this.retryPredicate = retryPredicate;
    grpcRequestObjectBuilder = JobServiceOuterClass.UpdateJobRetriesRequest.newBuilder();
    grpcRequestObjectBuilder.setJobKey(jobKey);
  }

  @Override
  public UpdateRetriesJobCommandStep2 retries(final int retries) {
    grpcRequestObjectBuilder.setRetries(retries);
    return this;
  }

  @Override
  public FinalCommandStep<UpdateRetriesJobResponse> requestTimeout(final Duration requestTimeout) {
    this.requestTimeout = requestTimeout;
    return this;
  }

  @Override
  public KunpengFuture<UpdateRetriesJobResponse> send() {
    return sendGrpcRequest();
  }

  private KunpengFuture<UpdateRetriesJobResponse> sendGrpcRequest() {
    final JobServiceOuterClass.UpdateJobRetriesRequest request = grpcRequestObjectBuilder.build();

    final RetriableClientFutureImpl<
            UpdateRetriesJobResponse, JobServiceOuterClass.UpdateJobRetriesResponse>
        future =
            new RetriableClientFutureImpl<>(
                UpdateRetriesJobResponseImpl::new,
                retryPredicate,
                streamObserver -> sendGrpcRequest(request, streamObserver));

    sendGrpcRequest(request, future);
    return future;
  }

  private void sendGrpcRequest(
      final JobServiceOuterClass.UpdateJobRetriesRequest request,
      final StreamObserver<JobServiceOuterClass.UpdateJobRetriesResponse> streamObserver) {
    asyncStub
        .withDeadlineAfter(requestTimeout.toMillis(), TimeUnit.MILLISECONDS)
        .updateJobRetries(request, streamObserver);
  }

  @Override
  public UpdateRetriesJobCommandStep2 operationReference(final long operationReference) {
    grpcRequestObjectBuilder.setOperationReference(operationReference);
    return this;
  }
}
