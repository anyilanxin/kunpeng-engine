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
package com.anyilanxin.kunpeng.repository.admin.modules.source;

import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.source.NodeSourceMetaRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.source.NodeSourceRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.source.PartitionSourceMetaRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.source.PartitionSourceRecord;

/**
 * @author zxuanhong
 * @since
 */
public interface MutableRepositorySource extends ImmutableRepositorySource {
  void save(NodeSourceMetaRecord record);

  void update(NodeSourceMetaRecord record);

  void applied(NodeSourceRecord record);

  void save(PartitionSourceMetaRecord record);

  void update(PartitionSourceMetaRecord record);

  void applied(PartitionSourceRecord record);

  void transferred(PartitionSourceRecord record);

  /** 移除分区已被转移出去的代理资源标识，分区记录保留（区别于整分区转移的 {@link #transferred}） */
  void transferredRemove(PartitionSourceRecord record);
}
