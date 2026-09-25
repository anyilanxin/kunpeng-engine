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

public interface DecisionDefinition {

  /**
   * @return the dmn decision ID, as parsed during deployment; together with the versions forms a
   *     unique identifier for a specific decision
   */
  String getDmnDecisionId();

  /**
   * @return the dmn name of the decision, as parsed during deployment
   */
  String getDmnDecisionName();

  /**
   * @return the assigned decision version
   */
  int getVersion();

  /**
   * @return the assigned decision key, which acts as a unique identifier for this decision
   */
  long getDecisionKey();

  /**
   * @return the dmn ID of the decision requirements graph that this decision is part of, as parsed
   *     during deployment
   */
  String getDmnDecisionRequirementsId();

  /**
   * @return the assigned key of the decision requirements graph that this decision is part of
   */
  long getDecisionRequirementsKey();

  /**
   * @return the tenant identifier that owns this decision
   */
  String getTenantId();
}
