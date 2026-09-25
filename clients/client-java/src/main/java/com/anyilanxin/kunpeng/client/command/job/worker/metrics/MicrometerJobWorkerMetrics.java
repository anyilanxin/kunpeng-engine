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
package com.anyilanxin.kunpeng.client.command.job.worker.metrics;

import com.anyilanxin.kunpeng.client.command.job.worker.JobWorkerMetrics;
import io.micrometer.core.instrument.Counter;
import java.util.Objects;

public final class MicrometerJobWorkerMetrics implements JobWorkerMetrics {

  private final Counter jobActivatedCounter;
  private final Counter jobHandledCounter;

  public MicrometerJobWorkerMetrics(
      final Counter jobActivatedCounter, final Counter jobHandledCounter) {
    this.jobActivatedCounter = Objects.requireNonNull(jobActivatedCounter, "必须提供 job 激活计数器");
    this.jobHandledCounter = Objects.requireNonNull(jobHandledCounter, "必须提供 job 处理计数器");
  }

  @Override
  public void jobActivated(final int count) {
    jobActivatedCounter.increment(count);
  }

  @Override
  public void jobHandled(final int count) {
    jobHandledCounter.increment(count);
  }
}
