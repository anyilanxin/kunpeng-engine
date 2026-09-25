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
package com.anyilanxin.kunpeng.client.command.job;

import com.anyilanxin.kunpeng.client.command.FinalCommandStep;
import java.time.Duration;

public interface UpdateJobCommandStep1 {

  /**
   * Update the retries and/or the timeout of this job.
   *
   * <p>If the given retries are greater than zero then this job will be picked up again by a job
   * subscription and a related incident will be marked as resolved.
   *
   * <p>Timeout value passed as a duration is used to calculate a new job deadline. This will happen
   * when the command is processed. The timeout value will be added to the current time then.
   *
   * @param jobChangeset the changeset of this job, if a field is not provided (equals to null) it
   *     will be not updated
   * @return the builder for this command. Call {@link #send()} to complete the command and send it
   *     to the broker.
   */
  UpdateJobCommandStep2 update(JobChangeset jobChangeset);

  /**
   * Update the retries and/or the timeout of this job.
   *
   * <p>If the given retries are greater than zero then this job will be picked up again by a job
   * subscription and a related incident will be marked as resolved.
   *
   * <p>Timeout value passed as a duration is used to calculate a new job deadline. This will happen
   * when the command is processed. The timeout value will be added to the current time then.
   *
   * @param retries retries the retries of this job, if null it won't be updated
   * @param timeout the time the timeout of this job, if null it won't be updated
   * @return the builder for this command. Call {@link #send()} to complete the command and send it
   *     to the broker.
   */
  UpdateJobCommandStep2 update(Integer retries, Long timeout);

  /**
   * Set the retries of this job.
   *
   * <p>If the given retries are greater than zero then this job will be picked up again by a job
   * subscription and a related incident will be marked as resolved.
   *
   * @param retries the retries of this job
   * @return the builder for this command. Call {@link #send()} to complete the command and send it
   *     to the broker.
   */
  UpdateJobCommandStep2 updateRetries(int retries);

  /**
   * Set the timeout of this job.
   *
   * <p>Timeout value in millis is used to calculate a new job deadline. This will happen when the
   * command is processed. The timeout value will be added to the current time then.
   *
   * @param timeout the timeout of this job
   * @return the builder for this command. Call {@link #send()} to complete the command and send it
   *     to the broker.
   */
  UpdateJobCommandStep2 updateTimeout(long timeout);

  /**
   * Set the timeout of this job.
   *
   * <p>Timeout value passed as a duration is used to calculate a new job deadline. This will happen
   * when the command is processed. The timeout value will be added to the current time then.
   *
   * @param timeout the time as duration (e.g. "Duration.ofMinutes(5)")
   * @return the builder for this command. Call {@link #send()} to complete the command and send it
   *     to the broker.
   */
  UpdateJobCommandStep2 updateTimeout(Duration timeout);

  interface UpdateJobCommandStep2 extends FinalCommandStep<UpdateJobResponse> {
    // the place for new optional parameters
  }
}
