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
import com.anyilanxin.kunpeng.client.command.*;
import com.anyilanxin.kunpeng.gateway.grpc.service.ProcessInstanceServiceGrpc;
import com.anyilanxin.kunpeng.gateway.grpc.service.ProcessInstanceServiceOuterClass;
import io.grpc.stub.StreamObserver;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

/**
 * @author zxuanhong
 * @since 2026.9.0
 */
public class CreateProcessInstanceWithResultCommandImpl
    implements CreateProcessInstanceCommand.CreateProcessInstanceWithResultCommandStep1 {

  private static final Duration DEADLINE_OFFSET = Duration.ofSeconds(10);
  private final ProcessInstanceServiceGrpc.ProcessInstanceServiceStub asyncStub;
  private final JsonMapper jsonMapper;
  private final Predicate<CredentialsProvider.StatusCode> retryPredicate;
  private final ProcessInstanceServiceOuterClass.CreateProcessInstanceWithResultRequest.Builder
      requestBuilder;
  private Duration requestTimeout;
  private final Set<String> defaultTenantIds;
  private final Set<String> customTenantIds;

  public CreateProcessInstanceWithResultCommandImpl(
      final ProcessInstanceServiceGrpc.ProcessInstanceServiceStub asyncStub,
      final KunpengClientConfiguration config,
      final JsonMapper jsonMapper,
      final Predicate<CredentialsProvider.StatusCode> retryPredicate) {
    this.asyncStub = asyncStub;
    this.jsonMapper = jsonMapper;
    this.retryPredicate = retryPredicate;

    requestBuilder =
        ProcessInstanceServiceOuterClass.CreateProcessInstanceWithResultRequest.newBuilder();
    requestTimeout(config.getDefaultRequestTimeout());
    defaultTenantIds = new HashSet<>(config.getDefaultJobWorkerTenantIds());
    customTenantIds = new HashSet<>();
  }

  @Override
  public CreateProcessInstanceCommand.CreateProcessInstanceWithResultCommandStep1 fetchVariables(
      final List<String> fetchVariables) {
    return this;
  }

  @Override
  public CreateProcessInstanceCommand.CreateProcessInstanceWithResultCommandStep1 fetchVariables(
      final String... fetchVariables) {
    return this;
  }

  @Override
  public CreateProcessInstanceCommand.CreateProcessInstanceWithResultCommandStep1 tenantId(
      final String tenantId) {
    return this;
  }

  @Override
  public FinalCommandStep<CreateProcessInstanceWithResultCommandResponse> requestTimeout(
      final Duration requestTimeout) {
    this.requestTimeout = requestTimeout;
    return this;
  }

  @Override
  public KunpengFuture<CreateProcessInstanceWithResultCommandResponse> send() {
    return sendGrpcRequest();
  }

  private KunpengFuture<CreateProcessInstanceWithResultCommandResponse> sendGrpcRequest() {
    final ProcessInstanceServiceOuterClass.CreateProcessInstanceWithResultRequest request =
        requestBuilder.build();
    final RetriableClientFutureImpl<
            CreateProcessInstanceWithResultCommandResponse,
            ProcessInstanceServiceOuterClass.CreateProcessInstanceWithResultResponse>
        future =
            new RetriableClientFutureImpl<>(
                CreateProcessInstanceWithResultCommandResponseImpl::new,
                retryPredicate,
                streamObserver -> sendGrpcRequest(request, streamObserver));

    sendGrpcRequest(request, future);

    return future;
  }

  private void sendGrpcRequest(
      final ProcessInstanceServiceOuterClass.CreateProcessInstanceWithResultRequest request,
      final StreamObserver<ProcessInstanceServiceOuterClass.CreateProcessInstanceWithResultResponse>
          streamObserver) {
    asyncStub
        .withDeadlineAfter(requestTimeout.toMillis(), TimeUnit.MILLISECONDS)
        .createProcessInstanceWithResult(request, streamObserver);
  }
}
