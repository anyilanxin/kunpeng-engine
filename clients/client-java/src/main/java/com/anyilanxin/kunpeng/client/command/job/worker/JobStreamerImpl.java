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
import com.anyilanxin.kunpeng.client.command.FinalCommandStep;
import com.anyilanxin.kunpeng.client.command.KunpengFuture;
import com.anyilanxin.kunpeng.client.command.job.ActivatedJob;
import com.anyilanxin.kunpeng.client.command.job.StreamJobsCommandStep1;
import com.anyilanxin.kunpeng.client.command.job.StreamJobsResponse;
import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.StatusRuntimeException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;
import org.slf4j.Logger;

/** 长连接推送消费器的守护循环：持有一条 StreamJobs 命令，断开/失败按退避重挂，配置了 streamTimeout 则按租约周期主动轮换重建。 */
@ThreadSafe
final class JobStreamerImpl implements JobStreamer {
  private static final Logger LOGGER = ClientLoggers.JOB_WORKER_LOGGER;

  private final JobClient jobClient;
  private final String jobType;
  private final String workerName;
  private final Duration timeout;
  private final List<String> fetchVariables;
  private final List<String> tenantIds;
  private final Duration streamTimeout;
  private final BackoffSupplier backoffSupplier;
  private final ScheduledExecutorService executor;
  private final Lock streamLock;

  @GuardedBy("streamLock") private KunpengFuture<StreamJobsResponse> activeStream;

  @GuardedBy("streamLock") private FinalCommandStep<StreamJobsResponse> command;

  @GuardedBy("streamLock") private boolean closed;

  @GuardedBy("streamLock") private long backoffDelay;

  @GuardedBy("streamLock") private ScheduledFuture<?> rotateTask;

  JobStreamerImpl(
      final JobClient jobClient,
      final String jobType,
      final String workerName,
      final Duration timeout,
      final List<String> fetchVariables,
      final List<String> tenantIds,
      final Duration streamTimeout,
      final BackoffSupplier backoffSupplier,
      final ScheduledExecutorService executor) {
    this.jobClient = jobClient;
    this.jobType = jobType;
    this.workerName = workerName;
    this.timeout = timeout;
    this.fetchVariables = fetchVariables;
    this.tenantIds = tenantIds;
    this.streamTimeout = streamTimeout;
    this.backoffSupplier = backoffSupplier;
    this.executor = executor;

    streamLock = new ReentrantLock();
  }

  @Override
  public void close() {
    runLocked(this::closeUnderLock);
  }

  @Override
  public boolean isOpen() {
    return !closed;
  }

  @Override
  public void openStreamer(final Consumer<ActivatedJob> jobConsumer) {
    reopen(assembleCommand(jobConsumer));
  }

  private void reopen(final FinalCommandStep<StreamJobsResponse> nextCommand) {
    runLocked(
        () -> {
          if (closed) {
            LOGGER.trace("流已关闭，跳过重挂 [type: {}, worker: {}]", jobType, workerName);
            return;
          }
          command = nextCommand;
          openUnderLock();
        });
  }

  private void onStreamTerminated(final Throwable error) {
    runLocked(() -> onTerminatedUnderLock(error));
  }

  private FinalCommandStep<StreamJobsResponse> assembleCommand(
      final Consumer<ActivatedJob> jobConsumer) {
    StreamJobsCommandStep1.StreamJobsCommandStep3 step =
        jobClient
            .newStreamJobsCommand()
            .jobType(jobType)
            .consumer(jobConsumer)
            .workerName(workerName)
            .tenantIds(tenantIds)
            .timeout(timeout);

    if (fetchVariables != null) {
      step = step.fetchVariables(fetchVariables);
    }

    return step;
  }

  /** 以可中断方式持锁执行；被中断则恢复中断标记后放弃本次操作 */
  private void runLocked(final Runnable body) {
    try {
      streamLock.lockInterruptibly();
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
      return;
    }

    try {
      body.run();
    } finally {
      streamLock.unlock();
    }
  }

  @GuardedBy("streamLock") private void closeUnderLock() {
    LOGGER.debug("正在关闭 job 流 [type: {}, worker: {}]", jobType, workerName);
    closed = true;
    if (rotateTask != null) {
      rotateTask.cancel(true);
    }
    if (activeStream != null) {
      activeStream.cancel(true);
    }
    LOGGER.debug("job 流已关闭 [type: {}, worker: {}]", jobType, workerName);
  }

  @GuardedBy("streamLock") private void openUnderLock() {
    if (activeStream != null) {
      activeStream.cancel(true);
      activeStream = null;
    }

    final KunpengFuture<StreamJobsResponse> control = command.send();
    control.whenCompleteAsync((ignored, error) -> onStreamTerminated(error), executor);
    activeStream = control;
    LOGGER.debug("job 流已建立 [type: {}, worker: {}]", jobType, workerName);

    if (streamTimeout != null) {
      LOGGER.debug(
          "{} 秒后轮换重建 job 流 [type: {}, worker: {}]",
          streamTimeout.getSeconds(),
          jobType,
          workerName);
      if (rotateTask != null && !rotateTask.isDone()) {
        rotateTask.cancel(true);
      }
      rotateTask =
          executor.schedule(
              () -> rotateUnderLease(activeStream),
              streamTimeout.toMillis(),
              TimeUnit.MILLISECONDS);
    }
  }

  private void rotateUnderLease(final KunpengFuture<StreamJobsResponse> observed) {
    runLocked(
        () -> {
          if (closed) {
            return;
          }
          if (activeStream == observed) {
            LOGGER.debug("流租约到期，主动轮换 [type: {}, worker: {}]", jobType, workerName);
            // 取消会触发 onTerminatedUnderLock，由它完成重建
            activeStream.cancel(
                true, Status.CANCELLED.withCause(new StreamLeaseTimeoutException()).asException());
            activeStream = null;
          } else if (!observed.isDone()) {
            LOGGER.error("轮换任务对应的流实例已被替换 [type: {}, worker: {}]", jobType, workerName);
          }
        });
  }

  @GuardedBy("streamLock") private void onTerminatedUnderLock(final Throwable error) {
    if (closed) {
      LOGGER.trace("流已关闭，跳过重建 [type: {}, worker: {}]", jobType, workerName);
      return;
    }

    if (error != null) {
      if (error instanceof StatusRuntimeException) {
        final StatusRuntimeException statusError = (StatusRuntimeException) error;
        if (statusError.getStatus().getCode() == Status.CANCELLED.getCode()
            && statusError.getCause() instanceof StatusException
            && statusError.getCause().getCause() instanceof StreamLeaseTimeoutException) {
          LOGGER.debug("租约到期，立即轮换重建 job 流 [type: {}, worker: {}]", jobType, workerName);
          openUnderLock();
          return;
        }
      }
      reportStreamFailure(error);
      backoffDelay = backoffSupplier.supplyRetryDelay(backoffDelay);
      LOGGER.debug(
          "{} 后重挂 job 流 [type: {}, worker: {}]",
          Duration.ofMillis(backoffDelay),
          jobType,
          workerName);
      executor.schedule(() -> reopen(command), backoffDelay, TimeUnit.MILLISECONDS);
    }
  }

  private void reportStreamFailure(final Throwable error) {
    if (error instanceof StatusRuntimeException
        && ((StatusRuntimeException) error).getStatus().getCode()
            == Status.RESOURCE_EXHAUSTED.getCode()) {
      // 服务端流控反压属常态信号，仅留痕不告警
      LOGGER.trace("job 流被服务端反压 [type: {}, worker: {}]", jobType, workerName, error);
      return;
    }

    LOGGER.warn("job 流中断 [type: {}, worker: {}]", jobType, workerName, error);
  }

  /** 租约到期的内部标记异常：用于把主动轮换与真实故障区分开 */
  private static final class StreamLeaseTimeoutException extends RuntimeException {}
}
