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

import com.anyilanxin.kunpeng.client.command.job.worker.metrics.MicrometerJobWorkerMetricsBuilder;
import com.anyilanxin.kunpeng.client.command.job.worker.metrics.MicrometerJobWorkerMetricsBuilderImpl;

/** Worker metrics API. Allows basic instrumenting of job activation and handling. */
public interface JobWorkerMetrics {

  /**
   * Called every time one or more jobs are activated.
   *
   * <p>NOTE: this is called <em>before</em> the job is worked on.
   *
   * @param count the amount of jobs that were activated
   */
  default void jobActivated(final int count) {}

  /**
   * Called every time one or more jobs are handled.
   *
   * <p>NOTE: this is called <em>after</em> a job has been worked on, successfully or not.
   *
   * @param count the amount of jobs that were handled
   */
  default void jobHandled(final int count) {}

  /**
   * Returns a new builder for the Micrometer bridge.
   *
   * @throws UnsupportedOperationException if Micrometer is not found in the class path
   */
  static MicrometerJobWorkerMetricsBuilder micrometer() {
    try {
      Class.forName("io.micrometer.core.instrument.MeterRegistry");
    } catch (final ClassNotFoundException e) {
      throw new UnsupportedOperationException(
          "Expected to create Micrometer worker metrics, but it seems Micrometer is not in your classpath",
          e);
    }

    return new MicrometerJobWorkerMetricsBuilderImpl();
  }

  /** Returns an implementation which does nothing. */
  static JobWorkerMetrics noop() {
    return new JobWorkerMetrics() {};
  }
}
