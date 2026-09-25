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

public final class CompleteJobCommandImpl extends CommandWithVariables3<CompleteJobCommandStep1>
    implements CompleteJobCommandStep1 {

  private final JobServiceGrpc.JobServiceStub asyncStub;
  private final JobServiceOuterClass.CompleteJobRequest.Builder grpcRequestObjectBuilder;
  private final Predicate<CredentialsProvider.StatusCode> retryPredicate;
  private Duration requestTimeout;
  private final long jobKey;
  private final JsonMapper jsonMapper;

  public CompleteJobCommandImpl(
      final JobServiceGrpc.JobServiceStub asyncStub,
      final JsonMapper jsonMapper,
      final Duration requestTimeout,
      final Predicate<CredentialsProvider.StatusCode> retryPredicate,
      final long key) {
    super(jsonMapper);
    this.asyncStub = asyncStub;
    this.requestTimeout = requestTimeout;
    this.retryPredicate = retryPredicate;
    grpcRequestObjectBuilder = JobServiceOuterClass.CompleteJobRequest.newBuilder();
    grpcRequestObjectBuilder.setJobKey(key);
    jobKey = key;
    this.jsonMapper = jsonMapper;
  }

  @Override
  public FinalCommandStep<CompleteJobResponse> requestTimeout(final Duration requestTimeout) {
    this.requestTimeout = requestTimeout;
    return this;
  }

  @Override
  public KunpengFuture<CompleteJobResponse> send() {
    return sendGrpcRequest();
  }

  private KunpengFuture<CompleteJobResponse> sendGrpcRequest() {
    final JobServiceOuterClass.CompleteJobRequest request = grpcRequestObjectBuilder.build();

    final RetriableClientFutureImpl<CompleteJobResponse, JobServiceOuterClass.CompleteJobResponse>
        future =
            new RetriableClientFutureImpl<>(
                CompleteJobResponseImpl::new,
                retryPredicate,
                streamObserver -> sendGrpcRequest(request, streamObserver));

    sendGrpcRequest(request, future);
    return future;
  }

  private void sendGrpcRequest(
      final JobServiceOuterClass.CompleteJobRequest request,
      final StreamObserver<JobServiceOuterClass.CompleteJobResponse> streamObserver) {
    asyncStub
        .withDeadlineAfter(requestTimeout.toMillis(), TimeUnit.MILLISECONDS)
        .completeJob(request, streamObserver);
  }

  @Override
  protected CompleteJobCommandStep1 setVariablesInternal(final String variables) {
    grpcRequestObjectBuilder.setVariables(variables);
    return this;
  }

  @Override
  protected CompleteJobCommandStep1 setLocalVariablesInternal(final String localVariables) {
    grpcRequestObjectBuilder.setLocalVariables(localVariables);
    return this;
  }
}
