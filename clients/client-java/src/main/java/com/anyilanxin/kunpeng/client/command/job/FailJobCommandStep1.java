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

import com.anyilanxin.kunpeng.client.command.CommandWithVariables;
import com.anyilanxin.kunpeng.client.command.FinalCommandStep;
import java.io.InputStream;
import java.time.Duration;
import java.util.Map;

public interface FailJobCommandStep1 {

  /**
   * Set the remaining retries of this job.
   *
   * <p>If the retries are greater than zero then this job will be picked up again by a job
   * subscription. Otherwise, an incident is created for this job.
   *
   * @param remainingRetries the remaining retries of this job (e.g. "jobEvent.getRetries() - 1")
   * @return the builder for this command. Call {@link #send()} to complete the command and send it
   *     to the broker.
   */
  FailJobCommandStep2 retries(int remainingRetries);

  interface FailJobCommandStep2
      extends FinalCommandStep<FailJobResponse>, CommandWithVariables<FailJobCommandStep2> {
    // the place for new optional parameters

    /**
     * Set the backoff timeout for failing this job.
     *
     * <p>If the backoff timeout is greater than zero and retries are greater than zero then this
     * job will be picked up again after the given backoff timeout is expired.
     *
     * @param backoffTimeout the backoff timeout of this job
     * @return the builder for this command. Call {@link #send()} to complete the command and send *
     *     it to the broker.
     */
    FailJobCommandStep2 retryBackoff(final Duration backoffTimeout);

    /**
     * Provide an error message describing the reason for the job failure. If failing the job
     * creates an incident, this error message will be used as incident message.
     *
     * @param errorMsg error message to be attached to the failed job
     * @return the builder for this command. Call {@link #send()} to complete the command and send
     *     it to the broker.
     */
    FailJobCommandStep2 errorMessage(String errorMsg);

    /**
     * Set the variables of this job.
     *
     * @param variables the variables (JSON) as String
     * @return the builder for this command. Call {@link #send()} to complete the command and send
     *     it to the broker.
     */
    @Override
    FailJobCommandStep2 variables(String variables);

    /**
     * Set the variables of this job.
     *
     * @param variables the variables as object
     * @return the builder for this command. Call {@link #send()} to complete the command and send
     *     it to the broker.
     */
    @Override
    FailJobCommandStep2 variables(Object variables);

    /**
     * Set the variables of this job.
     *
     * @param variables the variables (JSON) as stream
     * @return the builder for this command. Call {@link #send()} to complete the command and send
     *     it to the broker.
     */
    @Override
    FailJobCommandStep2 variables(InputStream variables);

    /**
     * Set the variables of this job.
     *
     * @param variables the variables as map
     * @return the builder for this command. Call {@link #send()} to complete the command and send
     *     it to the broker.
     */
    @Override
    FailJobCommandStep2 variables(Map<String, Object> variables);

    /**
     * Set a single variable of this job.
     *
     * @param key the key of the variable as string
     * @param value the value of the variable as object
     * @return the builder for this command. Call {@link #send()} to complete the command and send
     *     it to the broker.
     */
    @Override
    FailJobCommandStep2 variable(String key, Object value);
  }
}
