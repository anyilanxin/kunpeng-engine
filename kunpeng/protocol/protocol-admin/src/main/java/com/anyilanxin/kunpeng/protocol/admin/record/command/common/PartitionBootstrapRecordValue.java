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

import com.anyilanxin.kunpeng.protocol.admin.record.command.PartitionExecutionRecordValue;
import com.anyilanxin.kunpeng.protocol.admin.record.command.PartitionInfoMetaRecordValue;

/**
 * 分区引导命令记录契约，描述初始化目标分区组所需的拓扑元数据。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public interface PartitionBootstrapRecordValue extends PartitionExecutionRecordValue {
  /** 引导期分区拓扑（主成员单节点起步，其余成员由 JOIN 明细补齐） */
  PartitionInfoMetaRecordValue getPartitionMeta();

  /** 计划制定后该分区的完整最终拓扑元数据（全量成员），执行端据此落地分区元数据 */
  PartitionInfoMetaRecordValue getTargetMeta();

  int getSourceId();

  /** 是否引导镜像：仅分区 1 之外的分区需要置位（从镜像数据引导，分区 1 空引导） */
  boolean isBootstrapSnapshot();
}
