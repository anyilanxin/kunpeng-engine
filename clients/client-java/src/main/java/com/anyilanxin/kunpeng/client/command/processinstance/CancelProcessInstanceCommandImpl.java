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
package com.anyilanxin.kunpeng.client.command.processinstance;

import com.anyilanxin.kunpeng.client.KunpengClientConfiguration;
import com.anyilanxin.kunpeng.client.command.CredentialsProvider;
import com.anyilanxin.kunpeng.client.command.FinalCommandStep;
import com.anyilanxin.kunpeng.client.command.KunpengFuture;
import com.anyilanxin.kunpeng.client.command.RetriableClientFutureImpl;
import com.anyilanxin.kunpeng.gateway.grpc.service.ProcessInstanceServiceGrpc;
import com.anyilanxin.kunpeng.gateway.grpc.service.ProcessInstanceServiceOuterClass;
import io.grpc.stub.StreamObserver;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

public final class CancelProcessInstanceCommandImpl implements CancelProcessInstanceCommand {
  private final ProcessInstanceServiceGrpc.ProcessInstanceServiceStub asyncStub;
  private final Predicate<CredentialsProvider.StatusCode> retryPredicate;
  private final ProcessInstanceServiceOuterClass.CancelProcessInstanceRequest.Builder
      requestBuilder;
  private Duration requestTimeout;

  public CancelProcessInstanceCommandImpl(
      final ProcessInstanceServiceGrpc.ProcessInstanceServiceStub asyncStub,
      final KunpengClientConfiguration config,
      final Predicate<CredentialsProvider.StatusCode> retryPredicate) {
    this.asyncStub = asyncStub;
    this.retryPredicate = retryPredicate;

    requestBuilder = ProcessInstanceServiceOuterClass.CancelProcessInstanceRequest.newBuilder();
    requestTimeout(config.getDefaultRequestTimeout());
  }

  @Override
  public FinalCommandStep<CancelProcessInstanceCommandResponse> requestTimeout(
      final Duration requestTimeout) {
    this.requestTimeout = requestTimeout;
    return this;
  }

  @Override
  public KunpengFuture<CancelProcessInstanceCommandResponse> send() {
    return sendGrpcRequest();
  }

  private KunpengFuture<CancelProcessInstanceCommandResponse> sendGrpcRequest() {
    final ProcessInstanceServiceOuterClass.CancelProcessInstanceRequest request =
        requestBuilder.build();

    final RetriableClientFutureImpl<
            CancelProcessInstanceCommandResponse,
            ProcessInstanceServiceOuterClass.CancelProcessInstanceResponse>
        future =
            new RetriableClientFutureImpl<>(
                CancelProcessInstanceCommandResponseImpl::new,
                retryPredicate,
                streamObserver -> sendGrpcRequest(request, streamObserver));
    sendGrpcRequest(request, future);
    return future;
  }

  private void sendGrpcRequest(
      final ProcessInstanceServiceOuterClass.CancelProcessInstanceRequest request,
      final StreamObserver<ProcessInstanceServiceOuterClass.CancelProcessInstanceResponse>
          streamObserver) {
    asyncStub
        .withDeadlineAfter(requestTimeout.toMillis(), TimeUnit.MILLISECONDS)
        .cancelProcessInstance(request, streamObserver);
  }

  @Override
  public CancelProcessInstanceCommand processInstanceId(final long processInstanceId) {
    requestBuilder.setProcessInstanceId(processInstanceId);
    return this;
  }
}
