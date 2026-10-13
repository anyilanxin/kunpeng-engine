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
package com.anyilanxin.kunpeng.protocol.admin.record.command.common;

import com.anyilanxin.kunpeng.protocol.admin.record.command.PartitionExecutionType;
import com.anyilanxin.kunpeng.protocol.admin.record.command.PartitionType;
import com.anyilanxin.kunpeng.protocol.common.RecordValue;

/**
 * 分区引导命令记录契约，描述初始化目标分区组所需的拓扑元数据。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public interface PartitionExecutionAckRecordValue extends RecordValue {
  /** 调度计划 id */
  long getDispatchPlanId();

  /** 调度计划执行 id */
  long getDispatchPlanExecutionId();

  /** 分区类型 */
  PartitionType getPartitionType();

  /** 执行类型 */
  PartitionExecutionType getExecutionType();

  /** 执行节点 id */
  String executionMemberId();

  /** 是否成功 */
  boolean isSuccess();

  /** 异常信息 */
  String getErrorMessage();
}
