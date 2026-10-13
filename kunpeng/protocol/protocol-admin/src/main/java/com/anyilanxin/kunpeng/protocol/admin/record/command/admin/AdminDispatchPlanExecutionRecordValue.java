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
package com.anyilanxin.kunpeng.protocol.admin.record.command.admin;

import com.anyilanxin.kunpeng.protocol.admin.record.command.DispatchExecutionState;
import com.anyilanxin.kunpeng.protocol.admin.record.command.PartitionExecutionRecordValue;

/**
 * 调度计划执行明细记录契约，描述所属计划执行 ID、执行顺序、调度起止时间、执行状态与补充说明。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public interface AdminDispatchPlanExecutionRecordValue extends PartitionExecutionRecordValue {
  long getDueDate();

  int getExecutionOrder();

  byte[] getPlanData();

  long getDispatchStartTime();

  long getDispatchEndTime();

  DispatchExecutionState getDispatchExecutionState();

  String getDispatchMessage();

  /** 本条明细的目标执行成员 */
  String getDispatchMemberId();
}
