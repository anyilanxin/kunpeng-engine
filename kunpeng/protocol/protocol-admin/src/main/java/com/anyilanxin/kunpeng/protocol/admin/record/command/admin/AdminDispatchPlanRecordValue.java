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

import com.anyilanxin.kunpeng.protocol.admin.record.command.DispatchPlanState;
import com.anyilanxin.kunpeng.protocol.admin.record.command.PartitionInfoMetaRecordValue;
import com.anyilanxin.kunpeng.protocol.common.RecordValue;
import java.util.List;

/**
 * 调度计划记录契约，描述计划 ID、依次执行的调度明细列表以及计划所基于的分区组拓扑快照。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public interface AdminDispatchPlanRecordValue extends RecordValue {
  long getDispatchPlanId();

  DispatchPlanState getDispatchState();

  AdminDispatchType getDispatchPlanType();

  boolean isApplyPlan();

  boolean isInitialize();

  int getOldReplicationFactor();

  int getExpectReplicationFactor();

  List<AdminDispatchPlanExecutionRecordValue> getExecutionPlan();

  PartitionInfoMetaRecordValue getOldMeta();

  PartitionInfoMetaRecordValue getMeta();
}
