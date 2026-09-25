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

import com.anyilanxin.kunpeng.client.command.job.ActivatedJob;

/** Implementations MUST be thread-safe. */
@FunctionalInterface
public interface JobHandler {

  /**
   * Handles a job. Implements the work to be done whenever a job of a certain type is received.
   *
   * <p>In case the job handler throws an exception the job is failed and the job retries are
   * automatically decremented by one. The failed job will contain the exception stacktrace as error
   * message.
   *
   * <p>If the retries reaches zero an incident will be created, which has to be resolved before the
   * job is available again (see {@link KunpengClient#newResolveIncidentCommand(long)}
   */
  void handle(JobClient client, ActivatedJob job) throws Exception;
}
