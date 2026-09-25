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
package com.anyilanxin.kunpeng.client.command.gatewaydiscribe;

import com.anyilanxin.kunpeng.client.KunpengClientConfiguration;
import com.anyilanxin.kunpeng.client.command.*;
import com.anyilanxin.kunpeng.gateway.grpc.service.ClusterManageServiceGrpc;
import com.anyilanxin.kunpeng.gateway.grpc.service.ClusterManageServiceOuterClass;
import io.grpc.stub.StreamObserver;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

public class GatewayAddressQueryCommandImpl implements GatewayAddressQueryCommand {
  private final ClusterManageServiceGrpc.ClusterManageServiceStub asyncStub;
  private final JsonMapper jsonMapper;
  private final Predicate<CredentialsProvider.StatusCode> retryPredicate;
  private final ClusterManageServiceOuterClass.QueryGatewayRequest.Builder requestBuilder;
  private final KunpengClientConfiguration config;
  private Duration requestTimeout;

  public GatewayAddressQueryCommandImpl(
      final ClusterManageServiceGrpc.ClusterManageServiceStub asyncStub,
      final KunpengClientConfiguration config,
      final JsonMapper jsonMapper,
      final Predicate<CredentialsProvider.StatusCode> retryPredicate) {
    this.config = config;
    this.asyncStub = asyncStub;
    this.jsonMapper = jsonMapper;
    this.retryPredicate = retryPredicate;
    requestBuilder = ClusterManageServiceOuterClass.QueryGatewayRequest.newBuilder();
    requestTimeout(config.getDefaultRequestTimeout());
  }

  @Override
  public FinalCommandStep<Gateway> requestTimeout(final Duration requestTimeout) {
    this.requestTimeout = requestTimeout;
    return this;
  }

  @Override
  public KunpengFuture<Gateway> send() {
    return sendGrpcRequest();
  }

  private KunpengFuture<Gateway> sendGrpcRequest() {
    final ClusterManageServiceOuterClass.QueryGatewayRequest request = requestBuilder.build();
    final RetriableClientFutureImpl<Gateway, ClusterManageServiceOuterClass.QueryGatewayResponse>
        future =
            new RetriableClientFutureImpl<>(
                GatewayImpl::new,
                retryPredicate,
                streamObserver -> sendGrpcRequest(request, streamObserver));
    sendGrpcRequest(request, future);
    return future;
  }

  private void sendGrpcRequest(
      final ClusterManageServiceOuterClass.QueryGatewayRequest request,
      final StreamObserver<ClusterManageServiceOuterClass.QueryGatewayResponse> streamObserver) {
    asyncStub
        .withDeadlineAfter(requestTimeout.toMillis(), TimeUnit.MILLISECONDS)
        .queryGateway(request, streamObserver);
  }
}
