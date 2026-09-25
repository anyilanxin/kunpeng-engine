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
}
