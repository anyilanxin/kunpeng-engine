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
package com.anyilanxin.kunpeng.client.command.deployment;

import com.anyilanxin.kunpeng.client.command.FormDefinition;
import com.anyilanxin.kunpeng.client.command.ProcessDefinition;
import java.util.List;

public interface DeployResourceCommandResponse {
  /**
   * @return the unique key of the deployment
   */
  long getDeploymentId();

  /**
   * @return the processes which are deployed
   */
  List<ProcessDefinition> getProcessDefinitions();

  /**
   * @return the decisions which are deployed
   */
  List<DecisionDefinition> getDecisions();

  /**
   * @return the decision requirements which are deployed
   */
  List<DecisionRequirementDefinition> getDecisionRequirements();

  /**
   * @return the deployed form metadata
   */
  List<FormDefinition> getForm();

  /**
   * @return the tenant identifier that owns this deployment
   */
  String getTenantId();
}
