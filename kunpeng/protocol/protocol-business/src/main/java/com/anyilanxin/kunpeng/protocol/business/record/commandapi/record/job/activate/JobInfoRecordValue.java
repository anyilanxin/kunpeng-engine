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
package com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.job.activate;

import com.anyilanxin.kunpeng.protocol.business.record.command.job.JobKindType;
import com.anyilanxin.kunpeng.protocol.common.TenantOwned;
import com.anyilanxin.kunpeng.structpack.JsonSerializable;
import java.util.Map;
import org.agrona.DirectBuffer;

/**
 * @author zxuanhong
 * @since 2026.9.0
 */
public interface JobInfoRecordValue extends TenantOwned, JsonSerializable {

  long getJobId();

  String getJobType();

  int getRetries();

  long getDeadline();

  JobKindType getJobKind();

  String getProcessDefinitionKey();

  long getProcessDefinitionId();

  long getProcessInstanceId();

  String getActivityDefinitionKey();

  long getActivityInstanceId();

  long getTaskId();

  Map<String, Object> getVariables();

  /** 激活该 job 的拉取方/流 worker（随投递外发） */
  String getWorker();

  DirectBuffer getVariablesBuffer();
}
