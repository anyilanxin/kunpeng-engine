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

/**
 * 分区引导命令记录契约，描述初始化目标分区组所需的拓扑元数据。
 *
 * @author zxuanhong
 * @since
 */
public interface PartitionBootstrapSourceDataTransferRecordValue
    extends PartitionExecutionRecordValue {
  /** 分区组 */
  String getPartitionGroup();

  /** 分区 id */
  int getPartitionId();

  /** 资源标识符 */
  int getSourceId();

  /** 资源来源分区组 */
  String getSourcePartitionGroup();

  /** 资源来源分区 id */
  int getSourcePartitionId();
}
