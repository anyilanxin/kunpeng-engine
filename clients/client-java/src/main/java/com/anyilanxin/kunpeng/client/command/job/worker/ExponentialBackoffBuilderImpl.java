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
import java.util.concurrent.TimeUnit;

public final class ExponentialBackoffBuilderImpl implements ExponentialBackoffBuilder {

  private static final long DEFAULT_MAX_DELAY = TimeUnit.SECONDS.toMillis(5);
  private static final long DEFAULT_MIN_DELAY = TimeUnit.MILLISECONDS.toMillis(50);
  private static final double DEFAULT_MULTIPLIER = 1.6;
  private static final double DEFAULT_JITTER = 0.1;
  private static final Random DEFAULT_RANDOM = new Random();

  private long maxDelay;
  private long minDelay;
  private double backoffFactor;
  private double jitterFactor;
  private Random random;

  public ExponentialBackoffBuilderImpl() {
    maxDelay = DEFAULT_MAX_DELAY;
    minDelay = DEFAULT_MIN_DELAY;
    backoffFactor = DEFAULT_MULTIPLIER;
    jitterFactor = DEFAULT_JITTER;
    random = DEFAULT_RANDOM;
  }

  @Override
  public ExponentialBackoffBuilder maxDelay(final long maxDelay) {
    this.maxDelay = maxDelay;
    return this;
  }

  @Override
  public ExponentialBackoffBuilder minDelay(final long minDelay) {
    this.minDelay = minDelay;
    return this;
  }

  @Override
  public ExponentialBackoffBuilder backoffFactor(final double backoffFactor) {
    this.backoffFactor = backoffFactor;
    return this;
  }

  @Override
  public ExponentialBackoffBuilder jitterFactor(final double jitterFactor) {
    this.jitterFactor = jitterFactor;
    return this;
  }

  @Override
  public ExponentialBackoffBuilder random(final Random random) {
    this.random = random;
    return this;
  }

  @Override
  public BackoffSupplier build() {
    return new ExponentialBackoff(maxDelay, minDelay, backoffFactor, jitterFactor, random);
  }
}
