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
package com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.deployment.create;

import com.anyilanxin.kunpeng.protocol.business.record.commandapi.ResponseRecordValue;
import com.anyilanxin.kunpeng.protocol.common.TenantOwned;
import com.anyilanxin.kunpeng.structpack.JsonSerializable;
import java.util.List;

/**
 * @author zxuanhong
 * @since 2026.9.0
 */
public interface DeploymentCreateResponseRecordValue
    extends TenantOwned, ResponseRecordValue, JsonSerializable {

  /** 部署 id */
  long getDeploymentId();

  /** 部署名称 */
  String getDeploymentName();

  /** 部署时间 */
  long getDeploymentTime();

  /**
   * Sets the date on which the process definitions contained in this deployment will be activated.
   * This means that all process definitions will be deployed as usual, but they will be suspended
   * from the start until the given activation date.
   */
  long getActivateProcessDefinitionsOn();

  /** 资源定义 */
  List<DeploymentResourceDefinitionResponseRecordValue> getResourceDefinitions();

  /** 流程定义 */
  List<DeploymentProcessDefinitionResponseRecordValue> getProcessDefinitions();

  /** 决策需求定义 */
  List<DecisionRequirementDefinitionResponseRecordValue> getDecisionRequirementDefinitions();

  /** 决策定义 */
  List<DeploymentDecisionDefinitionResponseRecordValue> getDecisionDefinitions();
}
