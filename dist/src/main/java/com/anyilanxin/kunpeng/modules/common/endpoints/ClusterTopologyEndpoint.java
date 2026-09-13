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

import static com.anyilanxin.kunpeng.protocol.common.ClusterCommonConstant.INITIAL_NODE_SOURCE;
import static com.anyilanxin.kunpeng.protocol.common.ClusterCommonConstant.NODE_SOURCE_PROPERTY_KEY;

import com.anyilanxin.kunpeng.cluster.cluster.ClusterMembershipService;
import com.anyilanxin.kunpeng.cluster.cluster.Member;
import com.anyilanxin.kunpeng.cluster.cluster.MemberId;
import com.anyilanxin.kunpeng.cluster.config.topology.PartitionMemberInfo;
import com.anyilanxin.kunpeng.cluster.config.topology.cluster.ClusterTopologyService;
import com.anyilanxin.kunpeng.configuration.ZoneType;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;

/**
 * 集群分区拓扑 Actuator 端点（/actuator/clusterTopology）：按在线成员维度输出各成员广播的分区状态。
 *
 * @author zxuanhong
 * @since
 */
@Endpoint(id = "clusterTopology")
public class ClusterTopologyEndpoint {
  private final ClusterTopologyService topologyService;
  private final ClusterMembershipService membershipService;

  public ClusterTopologyEndpoint(
      final ClusterTopologyService topologyService,
      final ClusterMembershipService membershipService) {
    this.topologyService = topologyService;
    this.membershipService = membershipService;
  }

  /** 返回在线成员及其分区拓扑列表：节点信息 + 节点下分区信息 */
  @ReadOperation
  public List<MemberPartitions> topology() {
    final Map<MemberId, List<PartitionMemberInfo>> memberPartitions =
        topologyService.getMemberPartitions();
    final List<MemberPartitions> result = new ArrayList<>();
    for (final Member member : membershipService.getMembers()) {
      if (!ZoneType.BROKER.getType().equalsIgnoreCase(member.zone())) {
        continue;
      }
      final List<PartitionMemberInfo> infos = memberPartitions.getOrDefault(member.id(), List.of());
      result.add(
          new MemberPartitions(
              member.id().id(),
              member.address().toString(),
              nodeSourceId(member),
              PartitionState.of(infos)));
    }
    result.sort(Comparator.comparing(MemberPartitions::memberId));
    return result;
  }

  /** 节点 sourceId：读成员广播属性，未初始化或解析失败时返回 0 兜底 */
  private int nodeSourceId(final Member member) {
    final String value = member.properties().getProperty(NODE_SOURCE_PROPERTY_KEY);
    if (value == null) {
      return INITIAL_NODE_SOURCE;
    }
    try {
      return Integer.parseInt(value);
    } catch (final NumberFormatException e) {
      return INITIAL_NODE_SOURCE;
    }
  }

  /** 节点信息与其下分区状态 */
  public record MemberPartitions(
      String memberId, String address, int nodeSourceId, List<PartitionState> partitions) {}

  /** 单个分区的拓扑状态 */
  public record PartitionState(
      String group,
      int id,
      String role,
      String health,
      long term,
      int sourceId,
      List<Integer> agentSourceIds) {

    static List<PartitionState> of(final List<PartitionMemberInfo> infos) {
      final List<PartitionState> states = new ArrayList<>(infos.size());
      for (final PartitionMemberInfo info : infos) {
        states.add(
            new PartitionState(
                info.getPartitionId().group(),
                info.getPartitionId().id(),
                info.getRole().name(),
                info.getHealth().name(),
                info.getTerm(),
                info.getSourceId(),
                info.getAgentSourceIds() == null
                    ? List.of()
                    : List.copyOf(info.getAgentSourceIds())));
      }
      states.sort(Comparator.comparing(PartitionState::group).thenComparingInt(PartitionState::id));
      return states;
    }
  }
}
