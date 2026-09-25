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
import io.micrometer.core.instrument.*;

public final class MicrometerJobWorkerMetricsBuilderImpl
    implements MicrometerJobWorkerMetricsBuilder {
  private MeterRegistry meterRegistry = Metrics.globalRegistry;
  private Iterable<Tag> tags = Tags.empty();

  @Override
  public MicrometerJobWorkerMetricsBuilder withMeterRegistry(final MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry == null ? Metrics.globalRegistry : meterRegistry;
    return this;
  }

  @Override
  public MicrometerJobWorkerMetricsBuilder withTags(final Iterable<Tag> tags) {
    this.tags = tags == null ? Tags.empty() : tags;
    return this;
  }

  @Override
  public JobWorkerMetrics build() {
    final Counter jobActivatedCounter = meterRegistry.counter(Names.JOB_ACTIVATED.asString(), tags);
    final Counter jobHandledCounter = meterRegistry.counter(Names.JOB_HANDLED.asString(), tags);
    return new MicrometerJobWorkerMetrics(jobActivatedCounter, jobHandledCounter);
  }
}
