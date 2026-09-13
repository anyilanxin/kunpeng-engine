/*
 * Copyright © 2026 anyilanxin zxh(anyilanxin@aliyun.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.protocol.admin.record.command.common;

import com.anyilanxin.kunpeng.protocol.admin.record.command.PartitionExecutionRecordValue;
import com.anyilanxin.kunpeng.protocol.admin.record.command.PartitionInfoMetaRecordValue;

/**
 * 分区引导命令记录契约，描述初始化目标分区组所需的拓扑元数据。
 *
 * @author zxuanhong
 * @since
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
