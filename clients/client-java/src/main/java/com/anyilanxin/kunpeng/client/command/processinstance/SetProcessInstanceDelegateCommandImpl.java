/*
 * Copyright © 2026 anyilanxin zxh(anyilanxin@aliyun.com)
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

import com.anyilanxin.kunpeng.client.AuthorizationHeaders;
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

/**
 * 设置流程实例代办人命令实现：授权头经 AuthorizationHeaders 注入（显式/provider，发送线程解析）。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class SetProcessInstanceDelegateCommandImpl
    implements SetProcessInstanceDelegateCommand {
  private final ProcessInstanceServiceGrpc.ProcessInstanceServiceStub asyncStub;
  private final KunpengClientConfiguration config;
  private final Predicate<CredentialsProvider.StatusCode> retryPredicate;
  private final ProcessInstanceServiceOuterClass.SetProcessInstanceDelegateRequest.Builder
      requestBuilder;
  private Duration requestTimeout;

  public SetProcessInstanceDelegateCommandImpl(
      final ProcessInstanceServiceGrpc.ProcessInstanceServiceStub asyncStub,
      final KunpengClientConfiguration config,
      final Predicate<CredentialsProvider.StatusCode> retryPredicate) {
    this.asyncStub = asyncStub;
    this.config = config;
    this.retryPredicate = retryPredicate;
    requestBuilder =
        ProcessInstanceServiceOuterClass.SetProcessInstanceDelegateRequest.newBuilder();
    requestTimeout(config.getDefaultRequestTimeout());
  }

  @Override
  public FinalCommandStep<SetProcessInstanceDelegateCommandResponse> requestTimeout(
      final Duration requestTimeout) {
    this.requestTimeout = requestTimeout;
    return this;
  }

  @Override
  public KunpengFuture<SetProcessInstanceDelegateCommandResponse> send() {
    final ProcessInstanceServiceOuterClass.SetProcessInstanceDelegateRequest request =
        requestBuilder.build();
    // 授权头在请求线程一次性物化到 stub：重试跑在 gRPC 回调线程，宿主 ThreadLocal 身份（如 SecurityContextHolder）不可见
    final ProcessInstanceServiceGrpc.ProcessInstanceServiceStub attached =
        AuthorizationHeaders.attach(asyncStub, null, null, config);

    final RetriableClientFutureImpl<
            SetProcessInstanceDelegateCommandResponse,
            ProcessInstanceServiceOuterClass.SetProcessInstanceDelegateResponse>
        future =
            new RetriableClientFutureImpl<>(
                SetProcessInstanceDelegateCommandResponseImpl::new,
                retryPredicate,
                streamObserver -> sendGrpcRequest(attached, request, streamObserver));
    sendGrpcRequest(attached, request, future);
    return future;
  }

  private void sendGrpcRequest(
      final ProcessInstanceServiceGrpc.ProcessInstanceServiceStub attached,
      final ProcessInstanceServiceOuterClass.SetProcessInstanceDelegateRequest request,
      final StreamObserver<ProcessInstanceServiceOuterClass.SetProcessInstanceDelegateResponse>
          streamObserver) {
    attached
        .withDeadlineAfter(requestTimeout.toMillis(), TimeUnit.MILLISECONDS)
        .setProcessInstanceDelegate(request, streamObserver);
  }

  @Override
  public SetProcessInstanceDelegateCommand processInstanceId(final long processInstanceId) {
    requestBuilder.setProcessInstanceId(processInstanceId);
    return this;
  }

  @Override
  public SetProcessInstanceDelegateCommand delegateUserId(final String delegateUserId) {
    requestBuilder.setDelegateUserId(delegateUserId);
    return this;
  }
}
