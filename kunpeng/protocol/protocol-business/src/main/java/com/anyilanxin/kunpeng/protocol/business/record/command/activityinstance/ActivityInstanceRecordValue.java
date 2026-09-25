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
package com.anyilanxin.kunpeng.protocol.business.record.command.activityinstance;

import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnElementType;
import com.anyilanxin.kunpeng.protocol.business.*;
import com.anyilanxin.kunpeng.protocol.common.RecordValueWithVariables;
import com.anyilanxin.kunpeng.protocol.common.TenantOwned;
import com.anyilanxin.kunpeng.structpack.JsonSerializable;

/**
 * @author zxuanhong
 * @since 2026.9.0
 */
public interface ActivityInstanceRecordValue
    extends TenantOwned, AdditionsRecodeValue, RecordValueWithVariables, JsonSerializable {
  /** 流程实例 id */
  long getActivityInstanceId();

  /** 父级流程实例 id,没有父级是为-1 */
  long getParentActivityInstanceId();

  int getRev();

  long getProcessInstanceId();

  long getRootProcessInstanceId();

  /** 流程定义 key */
  String getProcessDefinitionKey();

  /** 流程定义 id */
  long getProcessDefinitionId();

  long getCallProcessInstanceId();

  String getActivityDefinitionKey();

  String getActivityDefinitionName();

  BpmnElementType getActivityDefinitionType();

  long getTaskId();

  String getAssignee();

  String getStartActivityDefinitionKey();

  long getStartActivityInstanceId();

  ActivityInstanceState getState();

  long getSequenceCounter();

  long getIncidentId();

  /** 开始时间 */
  long getStartTime();

  /** 结束时间 */
  long getEndTime();

  /** 耗时 */
  long getDuration();

  ActivityInstanceLifeCycle getLifeCycle();

  /** 当前监听器类型 */
  ActivityInstanceListenerType getListenerType();

  /** 当前监听器下标 */
  int getListenerIndex();
}
