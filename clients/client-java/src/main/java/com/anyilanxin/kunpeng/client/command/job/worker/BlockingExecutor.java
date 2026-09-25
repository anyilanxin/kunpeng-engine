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

      wrappedExecutor.execute(
          () -> {
            try {
              command.run();
            } finally {
              semaphore.release();
            }
          });
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
