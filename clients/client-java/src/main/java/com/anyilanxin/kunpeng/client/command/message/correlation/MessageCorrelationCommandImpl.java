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
package com.anyilanxin.kunpeng.client.command.message.correlation;

import com.anyilanxin.kunpeng.client.KunpengClientConfiguration;
import com.anyilanxin.kunpeng.client.command.*;
import com.anyilanxin.kunpeng.gateway.grpc.service.MessageServiceGrpc;
import com.anyilanxin.kunpeng.gateway.grpc.service.MessageServiceOuterClass;
import io.grpc.stub.StreamObserver;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

public final class MessageCorrelationCommandImpl
    extends CommandWithVariables2<MessageCorrelationCommandImpl>
    implements MessageCorrelationCommand, MessageCorrelationCommand.MessageCorrelationCommandStep1 {
  private static final Duration DEADLINE_OFFSET = Duration.ofSeconds(10);
  private final MessageServiceGrpc.MessageServiceStub asyncStub;
  private final JsonMapper jsonMapper;
  private final Predicate<CredentialsProvider.StatusCode> retryPredicate;
  private final MessageServiceOuterClass.MessageCorrelationRequest.Builder requestBuilder;
  private final KunpengClientConfiguration config;
  private Duration requestTimeout;
  private final Set<String> defaultTenantIds;
  private final Set<String> customTenantIds;

  public MessageCorrelationCommandImpl(
      final MessageServiceGrpc.MessageServiceStub asyncStub,
      final KunpengClientConfiguration config,
      final JsonMapper jsonMapper,
      final Predicate<CredentialsProvider.StatusCode> retryPredicate) {
    super(jsonMapper);
    this.config = config;
    this.asyncStub = asyncStub;
    this.jsonMapper = jsonMapper;
    this.retryPredicate = retryPredicate;

    requestBuilder = MessageServiceOuterClass.MessageCorrelationRequest.newBuilder();
    requestTimeout(config.getDefaultRequestTimeout());
    defaultTenantIds = new HashSet<>(config.getDefaultJobWorkerTenantIds());
    customTenantIds = new HashSet<>();
  }

  @Override
  public FinalCommandStep<MessageCorrelationCommandResponse> requestTimeout(
      final Duration requestTimeout) {
    this.requestTimeout = requestTimeout;
    return this;
  }

  @Override
  public KunpengFuture<MessageCorrelationCommandResponse> send() {
    return sendGrpcRequest();
  }

  private KunpengFuture<MessageCorrelationCommandResponse> sendGrpcRequest() {
    final MessageServiceOuterClass.MessageCorrelationRequest request = requestBuilder.build();
    final RetriableClientFutureImpl<
            MessageCorrelationCommandResponse, MessageServiceOuterClass.MessageCorrelationResponse>
        future =
            new RetriableClientFutureImpl<>(
                MessageCorrelationCommandResponseImpl::new,
                retryPredicate,
                streamObserver -> sendGrpcRequest(request, streamObserver));
    sendGrpcRequest(request, future);
    return future;
  }

  private void sendGrpcRequest(
      final MessageServiceOuterClass.MessageCorrelationRequest request,
      final StreamObserver<MessageServiceOuterClass.MessageCorrelationResponse> streamObserver) {
    asyncStub
        .withDeadlineAfter(requestTimeout.toMillis(), TimeUnit.MILLISECONDS)
        .messageCorrelation(request, streamObserver);
  }

  @Override
  public MessageCorrelationCommandStep1 processInstanceId(final long processInstanceId) {
    requestBuilder.setProcessInstanceId(processInstanceId);
    return this;
  }

  @Override
  public MessageCorrelationCommandStep1 messageName(final String messageName) {
    requestBuilder.setMessageName(messageName);
    return this;
  }

  @Override
  public MessageCorrelationCommandStep1 correlationKey(final String correlationKey) {
    if (correlationKey != null) {
      requestBuilder.setCorrelationKey(correlationKey);
    }
    return this;
  }

  @Override
  public MessageCorrelationCommandStep1 tenantId(final String tenantId) {
    if (tenantId != null) {
      requestBuilder.setTenantId(tenantId);
    }
    return this;
  }

  @Override
  protected MessageCorrelationCommandImpl setVariablesInternal(final String variables) {
    requestBuilder.setVariable(variables);
    return this;
  }
}
