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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author zxuanhong
 * @since
 */
public class SwimPartitionMemberInfo implements Serializable {
  @Serial private static final long serialVersionUID = 1788311783333L;

  private final Map<PartitionId, PartitionMemberInfo> partitionMemberInfos =
      new ConcurrentHashMap<>();

  public SwimPartitionMemberInfo add(final PartitionMemberInfo memberInfo) {
    partitionMemberInfos.put(memberInfo.getPartitionId(), memberInfo);
    return this;
  }

  public SwimPartitionMemberInfo remove(final PartitionId partitionId) {
    partitionMemberInfos.remove(partitionId);
    return this;
  }

  public SwimPartitionMemberInfo clear() {
    partitionMemberInfos.clear();
    return this;
  }

  public List<PartitionMemberInfo> getInfos() {
    return new ArrayList<>(partitionMemberInfos.values());
  }

  public Map<PartitionId, PartitionMemberInfo> getInfoMap() {
    return new HashMap<>(partitionMemberInfos);
  }
}
