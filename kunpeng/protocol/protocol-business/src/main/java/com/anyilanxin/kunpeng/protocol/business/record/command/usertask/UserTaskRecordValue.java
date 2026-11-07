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
package com.anyilanxin.kunpeng.protocol.business.record.command.usertask;

import com.anyilanxin.kunpeng.protocol.business.AdditionsRecodeValue;
import com.anyilanxin.kunpeng.protocol.common.RecordValueWithVariables;
import com.anyilanxin.kunpeng.protocol.common.TenantOwned;
import java.util.List;

/**
 * @author zxuanhong
 * @since 2026.9.0
 */
public interface UserTaskRecordValue
    extends TenantOwned, AdditionsRecodeValue, RecordValueWithVariables {
  long getTaskId();

  long getParentTaskId();

  int getRev();

  long getActivityInstanceId();

  UserTaskState getState();

  long getProcessInstanceId();

  String getProcessDefinitionKey();

  long getProcessDefinitionId();

  String getBusinessKey();

  String getTaskDefinitionKey();

  String getTaskDefinitionName();

  String getAssignee();

  String getOwner();

  int getPriority();

  long getDueDate();

  long getFollowUpDate();

  UserTaskLifeCycle getLifeCycle();

  List<String> getCandidateGroups();

  List<String> getCandidateUsers();

  UserTaskListenerType getListenerType();

  int getListenerIndex();

  long getStartTime();

  long getEndTime();

  long getDuration();

  /** 加签角色（NONE=原任务，ADD_SIGN=加签任务） */
  AddSignRole getAddSignRole();

  /** 加签根活动实例 id（=E0，全层级级联共用同一根，整棵加签树一查即得）；原任务为 -1 */
  long getRootActivityInstanceId();

  /** 加签阶段（前/后加签） */
  AddSignPhase getAddSignPhase();

  /** 加签模式（并行/串行） */
  AddSignMode getAddSignMode();

  /** 本任务名下在途加签任务数（创建父+1、结束父−1） */
  int getAddSignCount();

  /** 加签层级：原任务/普通任务=0，第 N 级加签任务=N */
  int getAddSignDepth();

  /** 本任务是否允许再加签（创建时按建模固化：原任务=kunpeng:addSign 声明；加签任务=开启 cascade 且深度+1≤maxDepth） */
  boolean isAddSignAllowed();

  /** 串行加签链剩余名单，链上当前任务持有、完成时传下一位；无剩余为空列表 */
  List<String> getRemainingAssignees();

  /** 后加签预登记名单：完成前登记/追加，原任务完成时消费创建加签任务 */
  List<String> getAfterSignAssignees();

  /** 后加签预登记模式 */
  AddSignMode getAfterSignMode();
}
