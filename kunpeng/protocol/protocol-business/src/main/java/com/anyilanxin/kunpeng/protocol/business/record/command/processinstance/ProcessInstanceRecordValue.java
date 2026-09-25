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
package com.anyilanxin.kunpeng.protocol.business.record.command.processinstance;

import com.anyilanxin.kunpeng.protocol.business.*;
import com.anyilanxin.kunpeng.protocol.common.DurationValue;
import com.anyilanxin.kunpeng.protocol.common.RecordValueWithVariables;
import com.anyilanxin.kunpeng.protocol.common.TenantOwned;
import java.util.Set;

/**
 * @author zxuanhong
 * @since 2026.9.0
 */
public interface ProcessInstanceRecordValue
    extends TenantOwned, AdditionsRecodeValue, RecordValueWithVariables, DurationValue {
  /** 流程实例 id */
  long getProcessInstanceId();

  /** 父级流程实例 id,没有父级是为-1 */
  long getParentProcessInstanceId();

  /** 根流程实例 id */
  long getRootProcessInstanceId();

  int getRev();

  /** 引用的活动实力 id */
  long getReferenceActivityInstanceId();

  /** 业务 id */
  String getBusinessKey();

  /** 流程定义 key */
  String getProcessDefinitionKey();

  /** 流程定义名称 */
  String getProcessDefinitionName();

  /** 流程定义 id */
  long getProcessDefinitionId();

  /** 开始用户 id */
  String getStartUserId();

  /** 开始活动key列表 */
  Set<String> getStartActivityDefinitionKeys();

  Set<Long> getStartActivityInstanceIds();

  /** 结束活动key列表 */
  Set<String> getEndActivityDefinitionKeys();

  Set<Long> getEndActivityInstanceIds();

  /** 流程实例状态 */
  ProcessInstanceState getState();

  ProcessInstanceLifeCycle getLifeCycle();

  /** 当前监听器类型 */
  ProcessInstanceListenerType getListenerType();

  /** 当前监听器下标 */
  int getListenerIndex();
}
