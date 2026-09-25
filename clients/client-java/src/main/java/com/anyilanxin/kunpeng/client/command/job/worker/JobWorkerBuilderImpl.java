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
package com.anyilanxin.kunpeng.client.command.job.worker;

import static com.anyilanxin.kunpeng.client.command.ArgumentUtil.*;

import com.anyilanxin.kunpeng.client.KunpengClientConfiguration;
import java.io.Closeable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;

public final class JobWorkerBuilderImpl
    implements JobWorkerBuilderStep1,
        JobWorkerBuilderStep1.JobWorkerBuilderStep2,
        JobWorkerBuilderStep1.JobWorkerBuilderStep3 {

  public static final BackoffSupplier DEFAULT_BACKOFF_SUPPLIER =
      BackoffSupplier.newBackoffBuilder().build();
  public static final Duration DEFAULT_STREAMING_TIMEOUT = Duration.ofHours(8);
  private final JobClient jobClient;
  private final ScheduledExecutorService executorService;
  private final List<Closeable> closeables;
  private String jobType;
  private JobHandler handler;
  private Duration timeout;
  private String workerName;
  private int maxJobsActive;
  private Duration pollInterval;
  private Duration requestTimeout;
  private List<String> fetchVariables;
  private final List<String> defaultTenantIds;
  private final List<String> customTenantIds;
  private BackoffSupplier backoffSupplier;
  private boolean enableStreaming;
  private Duration streamingTimeout;
  private JobWorkerMetrics metrics = JobWorkerMetrics.noop();

  public JobWorkerBuilderImpl(
      final KunpengClientConfiguration configuration,
      final JobClient jobClient,
      final ScheduledExecutorService executorService,
      final List<Closeable> closeables) {
    this.jobClient = jobClient;
    this.executorService = executorService;
    this.closeables = closeables;

    timeout = configuration.getDefaultJobTimeout();
    workerName = configuration.getDefaultJobWorkerName();
    maxJobsActive = configuration.getDefaultJobWorkerMaxJobsActive();
    pollInterval = configuration.getDefaultJobPollInterval();
    requestTimeout = configuration.getDefaultRequestTimeout();
    enableStreaming = configuration.getDefaultJobWorkerStreamEnabled();
    defaultTenantIds = configuration.getDefaultJobWorkerTenantIds();
    customTenantIds = new ArrayList<>();
    backoffSupplier = DEFAULT_BACKOFF_SUPPLIER;
    streamingTimeout = DEFAULT_STREAMING_TIMEOUT;
  }

  @Override
  public JobWorkerBuilderStep2 jobType(final String type) {
    jobType = type;
    return this;
  }

  @Override
  public JobWorkerBuilderStep3 handler(final JobHandler handler) {
    this.handler = handler;
    return this;
  }

  @Override
  public JobWorkerBuilderStep3 timeout(final long timeout) {
    return timeout(Duration.ofMillis(timeout));
  }

  @Override
  public JobWorkerBuilderStep3 timeout(final Duration timeout) {
    this.timeout = timeout;
    return this;
  }

  @Override
  public JobWorkerBuilderStep3 name(final String workerName) {
    this.workerName = workerName;
    return this;
  }

  @Override
  public JobWorkerBuilderStep3 maxJobsActive(final int maxJobsActive) {
    this.maxJobsActive = maxJobsActive;
    return this;
  }

  @Override
  public JobWorkerBuilderStep3 pollInterval(final Duration pollInterval) {
    this.pollInterval = pollInterval;
    return this;
  }

  @Override
  public JobWorkerBuilderStep3 requestTimeout(final Duration requestTimeout) {
    this.requestTimeout = requestTimeout;
    return this;
  }

  @Override
  public JobWorkerBuilderStep3 fetchVariables(final List<String> fetchVariables) {
    this.fetchVariables = fetchVariables;
    return this;
  }

  @Override
  public JobWorkerBuilderStep3 fetchVariables(final String... fetchVariables) {
    return fetchVariables(Arrays.asList(fetchVariables));
  }

  @Override
  public JobWorkerBuilderStep3 backoffSupplier(final BackoffSupplier backoffSupplier) {
    this.backoffSupplier = backoffSupplier;
    return this;
  }

  @Override
  public JobWorkerBuilderStep3 streamEnabled(final boolean isStreamEnabled) {
    enableStreaming = isStreamEnabled;
    return this;
  }

  @Override
  public JobWorkerBuilderStep3 streamTimeout(final Duration timeout) {
    streamingTimeout = timeout;
    return this;
  }

  @Override
  public JobWorkerBuilderStep3 metrics(final JobWorkerMetrics metrics) {
    this.metrics = metrics == null ? JobWorkerMetrics.noop() : metrics;
    return this;
  }

  @Override
  public JobWorker open() {
    ensureNotNullNorEmpty("jobType", jobType);
    ensureNotNull("jobHandler", handler);
    ensurePositive("timeout", timeout);
    ensureNotNullNorEmpty("workerName", workerName);
    ensureGreaterThan("maxJobsActive", maxJobsActive, 0);

    final JobStreamer jobStreamer;
    final JobRunnableFactory jobRunnableFactory = new JobRunnableFactoryImpl(jobClient, handler);
    final JobPoller jobPoller =
        new JobPollerImpl(
            jobClient,
            requestTimeout,
            jobType,
            workerName,
            timeout,
            fetchVariables,
            getTenantIds(),
            maxJobsActive);

    final Executor jobExecutor;
    if (enableStreaming) {
      if (streamingTimeout != null) {
        ensurePositive("streamingTimeout", streamingTimeout);
      }

      jobStreamer =
          new JobStreamerImpl(
              jobClient,
              jobType,
              workerName,
              timeout,
              fetchVariables,
              getTenantIds(),
              streamingTimeout,
              backoffSupplier,
              executorService);
      jobExecutor = new BlockingExecutor(executorService, maxJobsActive, timeout);
    } else {
      jobStreamer = JobStreamer.noop();
      jobExecutor = executorService;
    }

    final JobWorkerImpl jobWorker =
        new JobWorkerImpl(
            maxJobsActive,
            executorService,
            pollInterval,
            jobRunnableFactory,
            jobPoller,
            jobStreamer,
            backoffSupplier,
            metrics,
            jobExecutor);
    closeables.add(jobWorker);
    return jobWorker;
  }

  @Override
  public JobWorkerBuilderStep3 tenantId(final String tenantId) {
    customTenantIds.add(tenantId);
    return this;
  }

  @Override
  public JobWorkerBuilderStep3 tenantIds(final List<String> tenantIds) {
    customTenantIds.addAll(tenantIds);
    return this;
  }

  @Override
  public JobWorkerBuilderStep3 tenantIds(final String... tenantIds) {
    tenantIds(Arrays.asList(tenantIds));
    return this;
  }

  private List<String> getTenantIds() {
    return customTenantIds.isEmpty() ? defaultTenantIds : customTenantIds;
  }
}
