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

import java.time.Duration;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

final class BlockingExecutor implements Executor {
  private static final TimeUnit TIMEOUT_UNIT = TimeUnit.MILLISECONDS;

  private final Executor wrappedExecutor;
  private final Semaphore semaphore;
  private final long timeoutMillis;

  public BlockingExecutor(
      final Executor wrappedExecutor, final int maxActivate, final Duration jobActivationTimeout) {
    this.wrappedExecutor = wrappedExecutor;
    semaphore = new Semaphore(maxActivate);
    timeoutMillis = jobActivationTimeout.toMillis();
  }

  @Override
  public void execute(final Runnable command) throws RejectedExecutionException {
    try {
      if (!semaphore.tryAcquire(timeoutMillis, TIMEOUT_UNIT)) {
        throw new RejectedExecutionException(
            String.format(
                "Not able to acquire lease in %d%s", timeoutMillis, TIMEOUT_UNIT.toString()));
      }

      try {
        wrappedExecutor.execute(
            () -> {
              try {
                command.run();
              } finally {
                semaphore.release();
              }
            });
      } catch (final RejectedExecutionException e) {
        // 已取许可但提交被拒（如执行器已关闭）：归还许可，否则并发额度只减不增永久缩容
        semaphore.release();
        throw e;
      }
    } catch (final InterruptedException e) {
      // 任务尚未提交，静默吞掉中断会丢失已激活的 job；恢复中断标记并按拒绝上报
      Thread.currentThread().interrupt();
      throw new RejectedExecutionException("Interrupted before submitting task", e);
    }
  }
}
