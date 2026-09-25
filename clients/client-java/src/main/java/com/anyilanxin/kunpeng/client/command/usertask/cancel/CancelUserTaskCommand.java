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
package com.anyilanxin.kunpeng.client.command.usertask.cancel;

import com.anyilanxin.kunpeng.client.command.CommandWithTenantStep;
import com.anyilanxin.kunpeng.client.command.FinalCommandStep;

public interface CancelUserTaskCommand
    extends CommandWithTenantStep<CancelUserTaskCommand>,
        FinalCommandStep<CancelUserTaskCommandResponse> {
  /**
   * Set the BPMN process id of the process to create an instance of. This is the static id of the
   * process in the BPMN XML (i.e. "&#60;bpmn:process id='my-process'&#62;").
   *
   * @param bpmnProcessId the BPMN process id of the process
   * @return the builder for this command
   */
  CancelUserTaskCommand taskId(long userTaskId);
}
