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

public interface DecisionRequirementDefinition {

  /**
   * @return the dmn decision requirements ID, as parsed during deployment; together with the
   *     versions forms a unique identifier for a specific decision
   */
  String getDmnDecisionRequirementsId();

  /**
   * @return the dmn name of the decision requirements, as parsed during deployment
   */
  String getDmnDecisionRequirementsName();

  /**
   * @return the assigned decision requirements version
   */
  int getVersion();

  /**
   * @return the assigned decision requirements key, which acts as a unique identifier for this
   *     decision requirements
   */
  long getDecisionRequirementsKey();

  /**
   * @return the resource name (i.e. filename) from which this decision requirements was parsed
   */
  String getResourceName();

  /**
   * @return the tenant identifier that owns this decision requirements
   */
  String getTenantId();
}
