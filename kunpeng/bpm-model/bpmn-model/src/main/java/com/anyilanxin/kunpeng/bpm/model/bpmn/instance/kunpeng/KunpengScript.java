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
package com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng;

import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.BpmnModelElementInstance;

/** Kunpeng extension element for feel script. It can be used for script tasks. */
public interface KunpengScript extends BpmnModelElementInstance {

  /**
   * @return the feel script expression of the script task
   */
  String getExpression();

  /**
   * Sets the feel script expression of the script task.
   *
   * @param expression the expression of the script task
   */
  void setExpression(String expression);

  /**
   * @return the name of the result variable
   */
  String getResultVariable();

  /**
   * Sets the name of the result variable.
   *
   * @param resultVariable the name of the result variable
   */
  void setResultVariable(String resultVariable);
}
