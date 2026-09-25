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
import io.micrometer.common.docs.KeyName;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;

/**
 * 基于 Micrometer 的 {@link JobWorkerMetrics} 构建器。可选能力：需自行把 <a
 * href="https://micrometer.io">Micrometer</a> 加入类路径（安装说明见 <a
 * href="https://micrometer.io/docs/installing">官方指南</a>）。
 *
 * <p>产出的实现会上报两个计数器：job 激活数、job 处理完成数。两计数器之差可估算 worker 当前积压的 job 量。
 *
 * <p>注意：最终指标名可能被底层 registry 改写（如 Prometheus 会把点号替换为下划线）。
 */
public interface MicrometerJobWorkerMetricsBuilder {

  /**
   * 指定指标注册位置；传 null 则落到 {@link io.micrometer.core.instrument.Metrics#globalRegistry}
   *
   * @param meterRegistry 指标注册中心
   * @return 构建器自身，便于链式调用
   */
  MicrometerJobWorkerMetricsBuilder withMeterRegistry(final MeterRegistry meterRegistry);

  /**
   * 附加到全部 worker 指标上的标签；可传 null
   *
   * @param tags 全局标签
   * @return 构建器自身，便于链式调用
   */
  MicrometerJobWorkerMetricsBuilder withTags(final Iterable<Tag> tags);

  JobWorkerMetrics build();

  /** 指标名集合 */
  @SuppressWarnings("NullableProblems")
  enum Names implements KeyName {

    /** {@link JobWorkerMetrics#jobActivated(int)} 对应的计数器名 */
    JOB_ACTIVATED {
      @Override
      public String asString() {
        return "kunpeng.client.worker.job.activated";
      }
    },

    /** {@link JobWorkerMetrics#jobHandled(int)} 对应的计数器名 */
    JOB_HANDLED {
      @Override
      public String asString() {
        return "kunpeng.client.worker.job.handled";
      }
    }
  }
}
