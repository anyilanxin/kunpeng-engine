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
package com.anyilanxin.kunpeng.protocol.admin.record.command.admin;

import com.anyilanxin.kunpeng.protocol.admin.record.command.PartitionInfoMetaRecordValue;
import com.anyilanxin.kunpeng.protocol.common.RecordValue;

/**
 * 集群元数据记录契约，描述集群配置版本号、副本因子、创建与更新时间，以及当前与上一版的分区组拓扑信息。
 *
 * @author zxuanhong
 * @since
 */
public interface AdminClusterMetaRecordValue extends RecordValue {
  int getVersion();

  int getReplicationFactor();

  int getCurrentReplicationFactor();

  long getCreateTime();

  long getUpdateTime();

  PartitionInfoMetaRecordValue getMeta();

  PartitionInfoMetaRecordValue getLastMeta();
}
