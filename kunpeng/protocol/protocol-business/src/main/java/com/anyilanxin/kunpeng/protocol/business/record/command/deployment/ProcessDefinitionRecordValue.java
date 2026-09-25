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
import java.util.List;
import java.util.Set;

/**
 * @author zxuanhong
 * @since 2026.9.0
 */
public interface ProcessDefinitionRecordValue extends TenantOwned, RecordValue {
  /** 流程定义 id */
  long getProcessDefinitionId();

  /** 流程定义名称 */
  String getProcessDefinitionName();

  /** 流程定义 key */
  String getProcessDefinitionKey();

  /**
   * @return the version of the deployed decision
   */
  int getProcessDefinitionVersion();

  /** 是否挂起 */
  boolean isSuspension();

  /** 是否允许启动新实例 */
  boolean isStartable();

  /** 历史生存时间，单位天 */
  int getHistoryTimeToLive();

  /** 候选启动组 */
  Set<String> getCandidateStarterGroups();

  /** 候选启动用户类表 */
  Set<String> getCandidateStarterUsers();

  /**
   * @return the custom version tag of the deployed decision
   */
  String getVersionTag();

  /** 部署id */
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

  List<StarterEventRecordValue> getStarterEvents();
}
