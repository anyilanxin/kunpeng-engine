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
package com.anyilanxin.kunpeng.client.command.usertask.complete;

import com.anyilanxin.kunpeng.client.command.CommandWithTenantStep;
import com.anyilanxin.kunpeng.client.command.CommandWithVariables;
import com.anyilanxin.kunpeng.client.command.FinalCommandStep;
import java.io.InputStream;
import java.util.Map;

public interface CompleteUserTaskCommand {
  /**
   * Set the BPMN process id of the process to create an instance of. This is the static id of the
   * process in the BPMN XML (i.e. "&#60;bpmn:process id='my-process'&#62;").
   *
   * @param bpmnProcessId the BPMN process id of the process
   * @return the builder for this command
   */
  CompleteUserTaskCommandStep1 taskId(long userTaskId);

  interface CompleteUserTaskCommandStep1
      extends CommandWithTenantStep<CompleteUserTaskCommandStep1>,
          FinalCommandStep<CompleteUserTaskCommandResponse>,
          CommandWithVariables<CompleteUserTaskCommandStep1> {
    /**
     * Set the initial variables of the process instance.
     *
     * @param variables the variables JSON document as String
     * @return the builder for this command. Call {@link #send()} to complete the command and send
     *     it to the broker.
     */
    @Override
    CompleteUserTaskCommandStep1 variables(String variables);

    /**
     * Set the initial variables of the process instance.
     *
     * @param variables the variables document as object to be serialized to JSON
     * @return the builder for this command. Call {@link #send()} to complete the command and send
     *     it to the broker.
     */
    @Override
    CompleteUserTaskCommandStep1 variables(Object variables);

    /**
     * Set the initial variables of the process instance.
     *
     * @param variables the variables JSON document as stream
     * @return the builder for this command. Call {@link #send()} to complete the command and send
     *     it to the broker.
     */
    @Override
    CompleteUserTaskCommandStep1 variables(InputStream variables);

    /**
     * Set the initial variables of the process instance.
     *
     * @param variables the variables document as map
     * @return the builder for this command. Call {@link #send()} to complete the command and send
     *     it to the broker.
     */
    @Override
    CompleteUserTaskCommandStep1 variables(Map<String, Object> variables);

    /**
     * Set a single initial variable of the process instance.
     *
     * @param key the key of the variable as string
     * @param value the value of the variable as object
     * @return the builder for this command. Call {@link #send()} to complete the command and send
     *     it to the broker.
     */
    @Override
    CompleteUserTaskCommandStep1 variable(String key, Object value);
  }
}
