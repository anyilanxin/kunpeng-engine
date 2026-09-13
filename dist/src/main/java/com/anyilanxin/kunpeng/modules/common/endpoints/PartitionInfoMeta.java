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
package com.anyilanxin.kunpeng.modules.common.endpoints;

import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.common.PartitionInfoMetaRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.common.PartitionMemberMetaRecord;
import java.util.ArrayList;
import java.util.List;

/**
 * 分区组拓扑信息。
 *
 * @author zxuanhong
 * @since
 */
public record PartitionInfoMeta(
    String partitionGroup,
    int partitionId,
    int targetPriority,
    String primaryMemberId,
    List<PartitionMemberMeta> members) {

  public static PartitionInfoMeta of(final PartitionInfoMetaRecord record) {
    final List<PartitionMemberMeta> members = new ArrayList<>(record.members().size());
    for (final PartitionMemberMetaRecord member : record.members()) {
      members.add(new PartitionMemberMeta(member.getMemberId(), member.getPriority()));
    }
    return new PartitionInfoMeta(
        record.getPartitionGroup(),
        record.getPartitionId(),
        record.getTargetPriority(),
        record.getPrimaryMemberId(),
        members);
  }

  /** 分区组成员 */
  public record PartitionMemberMeta(String memberId, int priority) {}
}
