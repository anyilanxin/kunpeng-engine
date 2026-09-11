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

/** Kunpeng extension element for a called decision. It can be used for business rule tasks. */
public interface KunpengCalledDecision extends BpmnModelElementInstance {

  /**
   * @return the id of the decision that is called
   */
  String getDecisionId();

  /**
   * Sets the id of the decision that is called.
   *
   * @param decisionId the id of the decision
   */
  void setDecisionId(String decisionId);

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

  /**
   * @return the binding type for the decision that is called
   */
  KunpengBindingType getBindingType();

  /**
   * Sets the binding type for the decision that is called.
   *
   * @param bindingType the binding type for the decision
   */
  void setBindingType(KunpengBindingType bindingType);

  /**
   * @return The version tag of the decision that is called
   */
  String getVersionTag();

  /**
   * Sets the version tag of the decision that is called.
   *
   * @param versionTag the version tag of the decision
   */
  void setVersionTag(String versionTag);
}
