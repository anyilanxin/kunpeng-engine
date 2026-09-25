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
package com.anyilanxin.kunpeng.client.command.variable.remove;

import com.anyilanxin.kunpeng.client.command.FinalCommandStep;
import java.util.List;

public interface RemoveVariableCommand {
  RemoveVariableCommandStep1 taskId(long userTaskId);

  RemoveVariableCommandStep1 processInstanceId(long processInstanceId);

  RemoveVariableCommandStep1 activityInstanceId(long activityInstanceId);

  interface RemoveVariableCommandStep1 {

    RemoveVariableCommandStep2 removeAll();

    RemoveVariableCommandStep3 remove(String variableKey);

    RemoveVariableCommandStep3 removes(List<String> variableKey);
  }

  interface RemoveVariableCommandStep2 extends FinalCommandStep<RemoveVariableCommandResponse> {}

  interface RemoveVariableCommandStep3 extends FinalCommandStep<RemoveVariableCommandResponse> {

    RemoveVariableCommandStep3 remove(String variableKey);

    RemoveVariableCommandStep3 removes(List<String> variableKey);
  }
}
