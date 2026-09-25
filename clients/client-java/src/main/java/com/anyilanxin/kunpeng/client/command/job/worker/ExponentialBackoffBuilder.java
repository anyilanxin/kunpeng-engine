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

import java.util.Random;

public interface ExponentialBackoffBuilder {

  /**
   * Sets the maximum retry delay.
   *
   * <p>Note that the jitter may push the retry delay over this maximum.
   *
   * <p>Default is 5000ms.
   *
   * @param maxDelay the maximum delay before retrying in ms
   * @return the builder for this exponential backoff
   */
  ExponentialBackoffBuilder maxDelay(long maxDelay);

  /**
   * Sets the minimum retry delay.
   *
   * <p>Note that the jitter may push the retry delay below this minimum.
   *
   * <p>Default is 50ms.
   *
   * @param minDelay the minimum delay before retrying is ms
   * @return the builder for this exponential backoff
   */
  ExponentialBackoffBuilder minDelay(long minDelay);

  /**
   * Sets the backoff multiplication factor. The previous delay is multiplied by this factor.
   * Default is 1.6.
   *
   * @param backoffFactor the factor to multiply with the previous delay to determine the next delay
   * @return the builder for this exponential backoff
   */
  ExponentialBackoffBuilder backoffFactor(double backoffFactor);

  /**
   * Sets the jitter factor. The next delay is changed randomly within a range of +/- this factor.
   *
   * <p>For example, if the next delay is calculated to be 1s and the jitterFactor is 0.1 then the
   * actual next delay can be somewhere between 0.9 and 1.1s.
   *
   * <p>Default is 0.1
   *
   * @param jitterFactor the range of possible jitter defined as a factor
   * @return the builder for this exponential backoff
   */
  ExponentialBackoffBuilder jitterFactor(double jitterFactor);

  /**
   * Sets the random number generator used to add jitter to the next delay.
   *
   * <p>Default is {@code new java.util.Random()}.
   *
   * @param random the random to use for jitter
   * @return the builder for this exponential backoff
   */
  ExponentialBackoffBuilder random(Random random);

  /**
   * @return a new {@link BackoffSupplier} with the provided configuration options.
   */
  BackoffSupplier build();
}
