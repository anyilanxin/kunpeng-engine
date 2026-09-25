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

import com.anyilanxin.kunpeng.protocol.business.record.command.deployment.ResourceType;

/**
 * @author zxuanhong
 * @since 2026.9.0
 */
public interface DeploymentResourceDefinitionResponseRecordValue {

  /** 资源 id */
  long getResourceDefinitionId();

  /** 资源名称 */
  String getResourceDefinitionName();

  /** 定义版本 */
  int getDefinitionVersion();

  /** 资源类型 */
  ResourceType getResourceType();

  /** 校验信息 */
  byte[] getChecksum();

  /** 部署id */
  long getDeploymentId();
}
