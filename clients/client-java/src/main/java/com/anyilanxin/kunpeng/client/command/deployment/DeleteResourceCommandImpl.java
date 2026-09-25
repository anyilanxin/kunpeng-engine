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
package com.anyilanxin.kunpeng.client.command.deployment;

import com.anyilanxin.kunpeng.client.KunpengClientConfiguration;
import com.anyilanxin.kunpeng.client.command.*;
import com.anyilanxin.kunpeng.client.response.DeleteResourceResponse;
import com.anyilanxin.kunpeng.gateway.grpc.service.DeploymentServiceGrpc;
import com.anyilanxin.kunpeng.gateway.grpc.service.DeploymentServiceOuterClass;
import io.grpc.stub.StreamObserver;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

public class DeleteResourceCommandImpl implements DeleteResourceCommand {

  private final DeploymentServiceOuterClass.DeleteResourceRequest.Builder requestBuilder =
      DeploymentServiceOuterClass.DeleteResourceRequest.newBuilder();
  private final DeploymentServiceGrpc.DeploymentServiceStub asyncStub;
  private final Predicate<CredentialsProvider.StatusCode> retryPredicate;
  private Duration requestTimeout;
  private final long resourceKey;
  private final JsonMapper jsonMapper;

  public DeleteResourceCommandImpl(
      final long resourceKey,
      final DeploymentServiceGrpc.DeploymentServiceStub asyncStub,
      final Predicate<CredentialsProvider.StatusCode> retryPredicate,
      final KunpengClientConfiguration config,
      final JsonMapper jsonMapper) {
    this.asyncStub = asyncStub;
    this.retryPredicate = retryPredicate;
    requestBuilder.setResourceKey(resourceKey);
    requestTimeout = config.getDefaultRequestTimeout();
    this.resourceKey = resourceKey;
    this.jsonMapper = jsonMapper;
    requestTimeout(requestTimeout);
  }

  @Override
  public FinalCommandStep<DeleteResourceResponse> requestTimeout(final Duration requestTimeout) {
    this.requestTimeout = requestTimeout;
    return this;
  }

  @Override
  public KunpengFuture<DeleteResourceResponse> send() {
    return sendGrpcRequest();
  }

  private KunpengFuture<DeleteResourceResponse> sendGrpcRequest() {
    final DeploymentServiceOuterClass.DeleteResourceRequest request = requestBuilder.build();

    final RetriableClientFutureImpl<
            DeleteResourceResponse, DeploymentServiceOuterClass.DeleteResourceResponse>
        future =
            new RetriableClientFutureImpl<>(
                DeleteResourceResponse::new,
                retryPredicate,
                streamObserver -> sendGrpcRequest(request, streamObserver));

    sendGrpcRequest(request, future);
    return future;
  }

  private void sendGrpcRequest(
      final DeploymentServiceOuterClass.DeleteResourceRequest request,
      final StreamObserver<DeploymentServiceOuterClass.DeleteResourceResponse> streamObserver) {
    asyncStub
        .withDeadlineAfter(requestTimeout.toMillis(), TimeUnit.MILLISECONDS)
        .deleteResource(request, streamObserver);
  }
}
