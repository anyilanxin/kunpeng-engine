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
package com.anyilanxin.kunpeng.cluster.config.topology;

import com.anyilanxin.kunpeng.cluster.cluster.PartitionId;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author zxuanhong
 * @since
 */
@Getter
@Setter
@ToString
public class PartitionMemberInfo implements Serializable {
  @Serial private static final long serialVersionUID = 1788311783333L;
  private String MemberId;
  private PartitionHealth health = PartitionHealth.UNKNOWN;
  private PartitionRole role = PartitionRole.UNKNOWN;
  private PartitionId partitionId;
  private int sourceId;
  private Set<Integer> agentSourceIds;

  /** 与 Raft 任期保持一致的 long 类型 */
  private long term;

  /** 身份 = 成员×分区：仅按 partitionId 判等会让分区维度视图 Set 塌缩为单成员状态（首个加入者胜出） */
  @Override
  public boolean equals(final Object o) {
    if (!(o instanceof final PartitionMemberInfo that)) {
      return false;
    }
    return Objects.equals(partitionId, that.partitionId) && Objects.equals(MemberId, that.MemberId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(partitionId, MemberId);
  }
}
