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
package com.anyilanxin.kunpeng.protocol.business.record.command.job;

import com.anyilanxin.kunpeng.protocol.common.RecordValue;
import com.anyilanxin.kunpeng.protocol.common.TenantOwned;
import java.util.Map;

/**
 * @author zxuanhong
 * @since 2026.9.0
 */
public interface JobRecordValue extends TenantOwned, RecordValue {

  long getJobId();

  String getJobType();

  JobKindType getJobKind();

  int getRetries();

  int getRetryBackOff();

  int getPriority();

  int getRev();

  long getDueDate();

  int getLockExpireTime();

  String getLockOwner();

  String getProcessDefinitionKey();

  long getProcessDefinitionId();

  long getProcessInstanceId();

  long getActivityInstanceId();

  String getActivityDefinitionKey();

  long getTaskId();

  JobLifeCycle getLifeCycle();

  JobState getState();

  long getIncidentId();

  boolean isDenied();

  String getDeniedReason();

  /** 开始时间 */
  long getStartTime();

  /** 结束时间 */
  long getEndTime();

  /** 耗时 */
  long getDuration();

  Map<String, Object> getVariables();

  Map<String, Object> getLocalVariables();
}
