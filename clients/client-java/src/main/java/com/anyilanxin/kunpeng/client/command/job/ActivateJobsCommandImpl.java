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

import com.anyilanxin.kunpeng.client.KunpengClientConfiguration;
import com.anyilanxin.kunpeng.client.command.CredentialsProvider;
import com.anyilanxin.kunpeng.client.command.FinalCommandStep;
import com.anyilanxin.kunpeng.client.command.JsonMapper;
import com.anyilanxin.kunpeng.client.command.KunpengFuture;
import com.anyilanxin.kunpeng.client.command.RetriableStreamingFutureImpl;
import com.anyilanxin.kunpeng.gateway.grpc.service.JobServiceGrpc;
import com.anyilanxin.kunpeng.gateway.grpc.service.JobServiceOuterClass;
import io.grpc.stub.StreamObserver;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

/**
 * 拉取命令实现（workjob 设计 §8.1）：走 JobService.PullJobs 长轮询——deadline 内挂起， 有 job 立即返回；空批 = 到期，client
 * 侧立即重挂即等效常驻挂起。
 */
public final class ActivateJobsCommandImpl
    implements ActivateJobsCommandStep1,
        ActivateJobsCommandStep1.ActivateJobsCommandStep2,
        ActivateJobsCommandStep1.ActivateJobsCommandStep3 {

  /** gRPC 调用截止在请求超时基础上追加的余量，给服务端留出组装响应的时间 */
  private static final Duration CALL_GRACE = Duration.ofSeconds(10);

  private final JobServiceGrpc.JobServiceStub stub;
  private final JsonMapper jsonMapper;
  private final Predicate<CredentialsProvider.StatusCode> retryPredicate;
  private final JobServiceOuterClass.PullRequest.Builder requestBuilder;
  private Duration requestTimeout;

  private final Set<String> defaultTenantIds;
  private final Set<String> customTenantIds;

  public ActivateJobsCommandImpl(
      final JobServiceGrpc.JobServiceStub asyncStub,
      final KunpengClientConfiguration config,
      final JsonMapper jsonMapper,
      final Predicate<CredentialsProvider.StatusCode> retryPredicate) {
    this.stub = asyncStub;
    this.jsonMapper = jsonMapper;
    this.retryPredicate = retryPredicate;
    requestBuilder = JobServiceOuterClass.PullRequest.newBuilder();
    requestTimeout(config.getDefaultRequestTimeout());
    timeout(config.getDefaultJobTimeout());
    workerName(config.getDefaultJobWorkerName());
    defaultTenantIds = new HashSet<>(config.getDefaultJobWorkerTenantIds());
    customTenantIds = new HashSet<>();
  }

  @Override
  public ActivateJobsCommandStep2 jobType(final String jobType) {
    requestBuilder.setType(jobType);
    return this;
  }

  @Override
  public ActivateJobsCommandStep3 maxJobsToActivate(final int maxJobsToActivate) {
    requestBuilder.setMaxBatch(maxJobsToActivate);
    return this;
  }

  @Override
  public ActivateJobsCommandStep3 timeout(final Duration timeout) {
    // PULL 语义：timeout 即服务端挂起时长（long-poll deadline）
    requestBuilder.setDeadlineMs(timeout.toMillis());
    return this;
  }

  @Override
  public ActivateJobsCommandStep3 workerName(final String workerName) {
    if (workerName != null) {
      requestBuilder.setWorker(workerName);
    }
    return this;
  }

  @Override
  public ActivateJobsCommandStep3 fetchVariables(final List<String> fetchVariables) {
    // 变量裁剪由 broker 侧派发时决定；当前 wire 协议直传全量变量
    return this;
  }

  @Override
  public ActivateJobsCommandStep3 fetchVariables(final String... fetchVariables) {
    return fetchVariables(Arrays.asList(fetchVariables));
  }

  @Override
  public FinalCommandStep<ActivateJobsResponse> requestTimeout(final Duration requestTimeout) {
    this.requestTimeout = requestTimeout;
    return this;
  }

  @Override
  public KunpengFuture<ActivateJobsResponse> send() {
    requestBuilder.clearTenantIds();
    if (customTenantIds.isEmpty()) {
      requestBuilder.addAllTenantIds(defaultTenantIds);
    } else {
      requestBuilder.addAllTenantIds(customTenantIds);
    }
    return dispatch();
  }

  private KunpengFuture<ActivateJobsResponse> dispatch() {
    final JobServiceOuterClass.PullRequest request = requestBuilder.build();

    final ActivateJobsResponseImpl response = new ActivateJobsResponseImpl(jsonMapper);
    final RetriableStreamingFutureImpl<ActivateJobsResponse, JobServiceOuterClass.PullResponse>
        future =
            new RetriableStreamingFutureImpl<>(
                response,
                response::addResponse,
                retryPredicate,
                observer -> invokePull(request, observer));

    invokePull(request, future);
    return future;
  }

  private void invokePull(
      final JobServiceOuterClass.PullRequest request,
      final StreamObserver<JobServiceOuterClass.PullResponse> observer) {
    stub.withDeadlineAfter(requestTimeout.plus(CALL_GRACE).toMillis(), TimeUnit.MILLISECONDS)
        .pullJobs(request, observer);
  }

  @Override
  public ActivateJobsCommandStep3 tenantId(final String tenantId) {
    customTenantIds.add(tenantId);
    return this;
  }

  @Override
  public ActivateJobsCommandStep3 tenantIds(final List<String> tenantIds) {
    customTenantIds.clear();
    customTenantIds.addAll(tenantIds);
    return this;
  }

  @Override
  public ActivateJobsCommandStep3 tenantIds(final String... tenantIds) {
    return tenantIds(Arrays.asList(tenantIds));
  }
}
