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
package com.anyilanxin.kunpeng.protocol.business.record.command.deployment;

import com.anyilanxin.kunpeng.protocol.common.RecordValue;
import com.anyilanxin.kunpeng.protocol.common.TenantOwned;

/**
 * @author zxuanhong
 * @since 2026.9.0
 */
public interface DecisionRequirementDefinitionRecordValue extends TenantOwned, RecordValue {
  /**
   * @return the key of the deployed DRG
   */
  long getDecisionRequirementDefinitionId();

  /**
   * @return the ID of the DRG in the DMN
   */
  String getDecisionRequirementDefinitionKey();

  /**
   * @return the name of the DRG in the DMN
   */
  String getDecisionRequirementDefinitionName();

  /**
   * @return the version of the deployed DRG
   */
  int getDecisionRequirementsVersion();

  /** 部署 id */
  long getDeploymentId();

  /** 资源 id */
  long getResourceDefinitionId();

  /** 资源名称 */
  String getResourceDefinitionName();

  /**
   * @return the checksum of the process (MD5)
   */
  byte[] getChecksum();

  /**
   * @return returns the corresponding binary resource
   */
  byte[] getResource();
}
