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

import com.anyilanxin.kunpeng.client.command.CommandWithOperationReferenceStep;
import com.anyilanxin.kunpeng.client.command.FinalCommandStep;

public interface UpdateRetriesJobCommandStep1 {
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
  UpdateRetriesJobCommandStep2 retries(int retries);

  interface UpdateRetriesJobCommandStep2
      extends CommandWithOperationReferenceStep<UpdateRetriesJobCommandStep2>,
          FinalCommandStep<UpdateRetriesJobResponse> {
    // the place for new optional parameters
  }
}
