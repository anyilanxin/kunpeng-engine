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
package com.anyilanxin.kunpeng.client.command.topology;

import com.anyilanxin.kunpeng.client.command.CredentialsProvider;
import com.anyilanxin.kunpeng.client.command.FinalCommandStep;
import com.anyilanxin.kunpeng.client.command.KunpengFuture;
import com.anyilanxin.kunpeng.client.command.RetriableClientFutureImpl;
import com.anyilanxin.kunpeng.gateway.grpc.service.ClusterManageServiceGrpc;
import com.anyilanxin.kunpeng.gateway.grpc.service.ClusterManageServiceOuterClass;
import io.grpc.stub.StreamObserver;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

public final class TopologyCommandImpl implements TopologyCommand {

  private final ClusterManageServiceGrpc.ClusterManageServiceStub asyncStub;
  private final Predicate<CredentialsProvider.StatusCode> retryPredicate;
  private Duration requestTimeout;

  public TopologyCommandImpl(
      final ClusterManageServiceGrpc.ClusterManageServiceStub asyncStub,
      final Duration requestTimeout,
      final Predicate<CredentialsProvider.StatusCode> retryPredicate) {
    this.asyncStub = asyncStub;
    this.requestTimeout = requestTimeout;
    this.retryPredicate = retryPredicate;
  }

  @Override
  public FinalCommandStep<TopologyCommandResponse> requestTimeout(final Duration requestTimeout) {
    this.requestTimeout = requestTimeout;
    return this;
  }

  @Override
  public KunpengFuture<TopologyCommandResponse> send() {
    return sendGrpcRequest();
  }

  private RetriableClientFutureImpl<
          TopologyCommandResponse, ClusterManageServiceOuterClass.ClusterTopologyResponse>
      sendGrpcRequest() {
    final ClusterManageServiceOuterClass.ClusterTopologyRequest request =
        ClusterManageServiceOuterClass.ClusterTopologyRequest.getDefaultInstance();

    final RetriableClientFutureImpl<
            TopologyCommandResponse, ClusterManageServiceOuterClass.ClusterTopologyResponse>
        future =
            new RetriableClientFutureImpl<>(
                TopologyCommandResponseImpl::new,
                retryPredicate,
                streamObserver -> sendGrpcRequest(request, streamObserver));

    sendGrpcRequest(request, future);

    return future;
  }

  private void sendGrpcRequest(
      final ClusterManageServiceOuterClass.ClusterTopologyRequest request,
      final StreamObserver<ClusterManageServiceOuterClass.ClusterTopologyResponse> streamObserver) {
    asyncStub
        .withDeadlineAfter(requestTimeout.toMillis(), TimeUnit.MILLISECONDS)
        .clusterTopology(request, streamObserver);
  }
}
