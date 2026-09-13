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
import java.util.List;

/**
 * 分区配置变更命令记录契约，描述变更后目标分区的拓扑元数据。
 *
 * @author zxuanhong
 * @since
 */
public interface PartitionConfigChangeRecordValue extends PartitionExecutionRecordValue {
  List<String> getMembers();

  String getPartitionGroup();

  int getPartitionId();

  /** 计划制定后该分区的完整最终拓扑元数据（全量成员），执行端据此落地分区元数据 */
  PartitionInfoMetaRecordValue getTargetMeta();
}
