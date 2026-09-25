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

/**
 * The {@link JobWorker} uses this interface to determine the retry delay after each failed request.
 * After a successful request, or if no requests have been sent yet, the delay is reset to the job
 * worker's polling interval (see {@link
 * JobWorkerBuilderStep1.JobWorkerBuilderStep3#pollInterval(Duration)}).
 *
 * <p>The supplier is called after a failed request. The worker will then await the supplied delay
 * before sending the next request.
 */
@FunctionalInterface
public interface BackoffSupplier {

  /**
   * @return a builder to configure and create a new exponential backoff {@link ExponentialBackoff}.
   */
  static ExponentialBackoffBuilder newBackoffBuilder() {
    return new ExponentialBackoffBuilderImpl();
  }

  /**
   * Returns the delay before the next retry. The delay should be specified in milliseconds.
   *
   * @param currentRetryDelay the last used retry delay
   * @return the new retry delay
   */
  long supplyRetryDelay(final long currentRetryDelay);
}
