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

import com.anyilanxin.kunpeng.client.ClientLoggers;
import com.anyilanxin.kunpeng.client.command.job.ActivateJobsCommandStep1;
import com.anyilanxin.kunpeng.client.command.job.ActivatedJob;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.time.Duration;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import org.slf4j.Logger;

/** PULL 模式单轮拉取执行器：发起一次批量激活长轮询，逐条回调消费、空批即到期；失败仅在 worker 仍开放时上抛。 */
public final class JobPollerImpl implements JobPoller {

  private static final Logger LOG = ClientLoggers.JOB_POLLER_LOGGER;

  private final JobClient jobClient;
  private final Duration requestTimeout;
  private final String jobType;
  private final String workerName;
  private final Duration timeout;
  private final List<String> fetchVariables;
  private final List<String> tenantIds;

  public JobPollerImpl(
      final JobClient jobClient,
      final Duration requestTimeout,
      final String jobType,
      final String workerName,
      final Duration timeout,
      final List<String> fetchVariables,
      final List<String> tenantIds,
      final int maxJobsToActivate) {
    this.jobClient = jobClient;
    this.requestTimeout = requestTimeout;
    this.jobType = jobType;
    this.workerName = workerName;
    this.timeout = timeout;
    this.fetchVariables = fetchVariables;
    this.tenantIds = tenantIds;
  }

  /**
   * 轮询拉取可用 job；拉回的 job 均已处于激活态。
   *
   * @param maxJobsToActivate 本轮拉取的批量上限
   * @param jobConsumer 逐条消费激活 job
   * @param doneCallback 回调本轮激活总数
   * @param errorCallback 回调拉取异常
   * @param openSupplier 探测消费端是否仍开放
   */
  @Override
  public void poll(
      final int maxJobsToActivate,
      final Consumer<ActivatedJob> jobConsumer,
      final IntConsumer doneCallback,
      final Consumer<Throwable> errorCallback,
      final BooleanSupplier openSupplier) {
    LOG.trace("发起拉取，本轮上限 {} 条 [worker: {}, type: {}]", maxJobsToActivate, workerName, jobType);

    assembleCommand(maxJobsToActivate)
        .requestTimeout(requestTimeout)
        .send()
        .whenComplete(
            (response, error) -> {
              if (error != null) {
                if (openSupplier.getAsBoolean()) {
                  try {
                    reportFailure(error);
                  } finally {
                    errorCallback.accept(error);
                  }
                }
                return;
              }

              final List<ActivatedJob> jobs = response.getJobs();
              jobs.forEach(jobConsumer);
              if (jobs.isEmpty()) {
                LOG.trace("本轮空批（长轮询到期） [worker: {}, type: {}]", workerName, jobType);
              } else {
                LOG.debug(
                    "本轮拉回 {} 条已激活 job [worker: {}, type: {}]", jobs.size(), workerName, jobType);
              }
              doneCallback.accept(jobs.size());
            });
  }

  private ActivateJobsCommandStep1.ActivateJobsCommandStep3 assembleCommand(final int batchLimit) {
    final ActivateJobsCommandStep1.ActivateJobsCommandStep3 command =
        jobClient
            .newActivateJobsCommand()
            .jobType(jobType)
            .maxJobsToActivate(batchLimit)
            .timeout(timeout)
            .workerName(workerName)
            .tenantIds(tenantIds);
    if (fetchVariables != null) {
      command.fetchVariables(fetchVariables);
    }
    return command;
  }

  private void reportFailure(final Throwable error) {
    if (error instanceof final StatusRuntimeException statusError
        && statusError.getStatus().getCode() == Status.RESOURCE_EXHAUSTED.getCode()) {
      // 满载反压属常态信号，由外层退避机制消化；需要观测时开 trace 即可
      LOG.trace("拉取被服务端反压 [worker: {}, type: {}]", workerName, jobType, error);
      return;
    }

    LOG.warn("拉取 job 失败 [worker: {}, type: {}]", workerName, jobType, error);
  }
}
