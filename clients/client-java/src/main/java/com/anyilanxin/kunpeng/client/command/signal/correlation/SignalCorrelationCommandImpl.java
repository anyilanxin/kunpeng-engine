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
package com.anyilanxin.kunpeng.client.command.signal.correlation;

import com.anyilanxin.kunpeng.client.KunpengClientConfiguration;
import com.anyilanxin.kunpeng.client.command.*;
import com.anyilanxin.kunpeng.gateway.grpc.service.SignalServiceGrpc;
import com.anyilanxin.kunpeng.gateway.grpc.service.SignalServiceOuterClass;
import io.grpc.stub.StreamObserver;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

public final class SignalCorrelationCommandImpl
    extends CommandWithVariables2<SignalCorrelationCommandImpl>
    implements SignalCorrelationCommand, SignalCorrelationCommand.SignalCorrelationCommandStep1 {
  private static final Duration DEADLINE_OFFSET = Duration.ofSeconds(10);
  private final SignalServiceGrpc.SignalServiceStub asyncStub;
  private final JsonMapper jsonMapper;
  private final Predicate<CredentialsProvider.StatusCode> retryPredicate;
  private final SignalServiceOuterClass.SignalCorrelationRequest.Builder requestBuilder;
  private final KunpengClientConfiguration config;
  private Duration requestTimeout;
  private final Set<String> defaultTenantIds;
  private final Set<String> customTenantIds;

  public SignalCorrelationCommandImpl(
      final SignalServiceGrpc.SignalServiceStub asyncStub,
      final KunpengClientConfiguration config,
      final JsonMapper jsonMapper,
      final Predicate<CredentialsProvider.StatusCode> retryPredicate) {
    super(jsonMapper);
    this.config = config;
    this.asyncStub = asyncStub;
    this.jsonMapper = jsonMapper;
    this.retryPredicate = retryPredicate;

    requestBuilder = SignalServiceOuterClass.SignalCorrelationRequest.newBuilder();
    requestTimeout(config.getDefaultRequestTimeout());
    defaultTenantIds = new HashSet<>(config.getDefaultJobWorkerTenantIds());
    customTenantIds = new HashSet<>();
  }

  @Override
  public FinalCommandStep<SignalCorrelationCommandResponse> requestTimeout(
      final Duration requestTimeout) {
    this.requestTimeout = requestTimeout;
    return this;
  }

  @Override
  public KunpengFuture<SignalCorrelationCommandResponse> send() {
    return sendGrpcRequest();
  }

  private KunpengFuture<SignalCorrelationCommandResponse> sendGrpcRequest() {
    final SignalServiceOuterClass.SignalCorrelationRequest request = requestBuilder.build();
    final RetriableClientFutureImpl<
            SignalCorrelationCommandResponse, SignalServiceOuterClass.SignalCorrelationResponse>
        future =
            new RetriableClientFutureImpl<>(
                SignalCorrelationCommandResponseImpl::new,
                retryPredicate,
                streamObserver -> sendGrpcRequest(request, streamObserver));
    sendGrpcRequest(request, future);
    return future;
  }

  private void sendGrpcRequest(
      final SignalServiceOuterClass.SignalCorrelationRequest request,
      final StreamObserver<SignalServiceOuterClass.SignalCorrelationResponse> streamObserver) {
    asyncStub
        .withDeadlineAfter(requestTimeout.toMillis(), TimeUnit.MILLISECONDS)
        .signalCorrelation(request, streamObserver);
  }

  @Override
  public SignalCorrelationCommandStep1 processInstanceId(final long processInstanceId) {
    requestBuilder.setProcessInstanceId(processInstanceId);
    return this;
  }

  @Override
  public SignalCorrelationCommandStep1 signalName(final String signalName) {
    requestBuilder.setSignalName(signalName);
    return this;
  }

  @Override
  public SignalCorrelationCommandStep1 tenantId(final String tenantId) {
    if (tenantId != null) {
      requestBuilder.setTenantId(tenantId);
    }
    return this;
  }

  @Override
  protected SignalCorrelationCommandImpl setVariablesInternal(final String variables) {
    requestBuilder.setVariable(variables);
    return this;
  }
}
