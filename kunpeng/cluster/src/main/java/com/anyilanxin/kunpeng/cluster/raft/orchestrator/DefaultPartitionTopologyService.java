/*
 * Copyright © 2026 anyilanxin zxh (anyilanxin@aliyun.com)
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.cluster.raft.orchestrator;

import com.anyilanxin.kunpeng.cluster.cluster.ClusterMembershipEvent;
import com.anyilanxin.kunpeng.cluster.cluster.ClusterMembershipEventListener;
import com.anyilanxin.kunpeng.cluster.cluster.ClusterMembershipService;
import com.anyilanxin.kunpeng.cluster.cluster.Member;
import com.anyilanxin.kunpeng.cluster.cluster.MemberId;
import com.anyilanxin.kunpeng.cluster.raft.RaftServer.Role;
import com.anyilanxin.kunpeng.cluster.raft.journal.util.health.HealthReport;
import com.anyilanxin.kunpeng.cluster.raft.partition.PartitionId;
import com.anyilanxin.kunpeng.cluster.raft.partition.PartitionMetadata;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 默认分区拓扑服务实现：直接实现三个监听接口。
 *
 * <ul>
 *   <li>{@code RaftRoleChangeListener}（经 {@link ManagedPartitionTopologyService} 继承）——
 *       本地 raft 角色变化：记录本地角色、写入 member property 广播，并更新本地全局拓扑</li>
 *   <li>{@code FailureListener}（经 {@link ManagedPartitionTopologyService} 继承）——
 *       本地分区故障/恢复：维护分区健康标记</li>
 *   <li>{@link ClusterMembershipEventListener}——其他节点元数据变动：解析远端节点的分区
 *       角色并合并到本地全局拓扑；节点移除时清掉其拓扑数据</li>
 * </ul>
 */
public final class DefaultPartitionTopologyService
    implements ManagedPartitionTopologyService, ClusterMembershipEventListener {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultPartitionTopologyService.class);
  static final String TOPOLOGY_PROPERTY_KEY = "raft-partition-topology";

  private final ClusterMembershipService membershipService;
  /** 本节点的分区角色：groupName → partitionId → Role */
  private final Map<String, Map<Integer, Role>> localRoles = new ConcurrentHashMap<>();
  /** 本节点的故障分区：group/id */
  private final Set<String> unhealthyPartitions = ConcurrentHashMap.newKeySet();
  /** 全局拓扑：groupName → 分区组下的全部分区信息 */
  private final Map<String, List<PartitionInfo>> globalTopology =
      new ConcurrentHashMap<>();
  private volatile boolean started;

  public DefaultPartitionTopologyService(final ClusterMembershipService membershipService) {
    this.membershipService = membershipService;
  }

  // ===== Managed 生命周期 =====

  @Override
  public CompletableFuture<PartitionTopologyService> start() {
    membershipService.addListener(this);
    // 合并当前已知的全部成员
    for (final Member member : membershipService.getMembers()) {
      mergeRemoteTopology(member);
    }
    started = true;
    LOG.info("分区拓扑服务已启动");
    return CompletableFuture.completedFuture(this);
  }

  @Override
  public boolean isRunning() {
    return started;
  }

  @Override
  public CompletableFuture<Void> stop() {
    membershipService.removeListener(this);
    started = false;
    LOG.info("分区拓扑服务已停止");
    return CompletableFuture.completedFuture(null);
  }

  // ===== RaftRoleChangeListener：本地 raft 角色变化 =====

  @Override
  public void onNewRole(final PartitionMetadata metadata, final Role newRole, final long newTerm) {
    final PartitionId partitionId = metadata.id();
    LOG.debug("分区角色变化: {} → {} (term={})", partitionId, newRole, newTerm);
    localRoles
        .computeIfAbsent(partitionId.group(), k -> new ConcurrentHashMap<>())
        .put(partitionId.id(), newRole);
    updateGlobalTopology(
        membershipService.getLocalMember().id(), partitionId.group(), partitionId.id(), newRole);
    broadcastLocalTopology();
  }

  // ===== FailureListener：本地分区故障/恢复 =====

  @Override
  public void onFailure(final PartitionMetadata metadata, final HealthReport report) {
    LOG.warn("分区故障: {} — {}", metadata.id(), report);
    unhealthyPartitions.add(partitionKey(metadata.id()));
  }

  @Override
  public void onRecovered(final PartitionMetadata metadata) {
    LOG.info("分区恢复: {}", metadata.id());
    unhealthyPartitions.remove(partitionKey(metadata.id()));
  }

  // ===== ClusterMembershipEventListener：远端节点元数据/成员变动 =====

  @Override
  public void event(final ClusterMembershipEvent event) {
    switch (event.type()) {
      case METADATA_CHANGED -> mergeRemoteTopology(event.subject());
      case MEMBER_REMOVED -> removeFromGlobalTopology(event.subject().id());
      default -> {
        // 其他事件与分区拓扑无关
      }
    }
  }

  /** 广播本节点拓扑到 member property（触发其他节点的 METADATA_CHANGED 事件） */
  private void broadcastLocalTopology() {
    if (!started) {
      return;
    }
    final String serialized = serializeLocalRoles();
    membershipService.getLocalMember().properties().setProperty(TOPOLOGY_PROPERTY_KEY, serialized);
    LOG.debug("已广播本地分区拓扑: {}", serialized);
  }

  /** 合并远端节点的分区拓扑（从其 member property 中提取） */
  private void mergeRemoteTopology(final Member member) {
    final String topologyStr = member.properties().getProperty(TOPOLOGY_PROPERTY_KEY);
    if (topologyStr == null || topologyStr.isBlank()) {
      return;
    }
    final MemberId nodeId = member.id();
    deserializeRoles(topologyStr).forEach((group, partitions) ->
        partitions.forEach((partitionId, role) ->
            updateGlobalTopology(nodeId, group, partitionId, role)));
    LOG.debug("已合并节点 {} 的分区拓扑", nodeId);
  }

  /** 更新全局拓扑：替换（或新增）指定分区在指定节点上的条目 */
  private void updateGlobalTopology(
      final MemberId nodeId, final String group, final int partitionId, final Role role) {
    globalTopology.compute(group, (g, list) -> {
      final List<PartitionInfo> current = list != null ? list : List.of();
      final var updated = new ArrayList<PartitionInfo>(current.size() + 1);
      for (final PartitionInfo info : current) {
        if (info.partitionId() != partitionId || !info.nodeId().equals(nodeId)) {
          updated.add(info);
        }
      }
      updated.add(buildInfo(group, partitionId, nodeId, role));
      return List.copyOf(updated);
    });
  }

  private void removeFromGlobalTopology(final MemberId nodeId) {
    globalTopology.replaceAll((group, list) ->
        list.stream().filter(info -> !info.nodeId().equals(nodeId)).toList());
    globalTopology.values().removeIf(List::isEmpty);
  }

  /** 构造分区信息条目（健康状态与地址实时计算） */
  private PartitionInfo buildInfo(
      final String group, final int partitionId, final MemberId nodeId, final Role role) {
    final Member member = membershipService.getMember(nodeId);
    final MemberId localNodeId = membershipService.getLocalMember().id();
    final boolean healthy = member != null
        && member.isReachable()
        && !(nodeId.equals(localNodeId)
            && unhealthyPartitions.contains(group + "/" + partitionId));
    return new PartitionInfo(
        group,
        partitionId,
        nodeId,
        role,
        healthy,
        member != null ? member.address().toString() : null);
  }

  // ===== 查询 =====

  @Override
  public Optional<MemberId> findLeader(final String groupName, final int partitionId) {
    final List<PartitionInfo> members = getPartitionMembers(groupName, partitionId);
    return members.stream()
        .filter(PartitionInfo::isLeader)
        .filter(PartitionInfo::healthy)
        .map(PartitionInfo::nodeId)
        .findFirst()
        .or(() -> members.stream()
            .filter(PartitionInfo::isLeader)
            .map(PartitionInfo::nodeId)
            .findFirst());
  }

  @Override
  public Optional<MemberId> findLeader(final PartitionId partitionId) {
    return findLeader(partitionId.group(), partitionId.id());
  }

  @Override
  public List<PartitionInfo> getPartitionMembers(final String groupName, final int partitionId) {
    final List<PartitionInfo> partitions = globalTopology.get(groupName);
    if (partitions == null) {
      return List.of();
    }
    return partitions.stream()
        .filter(info -> info.partitionId() == partitionId)
        .map(info -> buildInfo(groupName, partitionId, info.nodeId(), info.role()))
        .toList();
  }

  @Override
  public List<PartitionInfo> getGroupTopology(final String groupName) {
    final List<PartitionInfo> partitions = globalTopology.get(groupName);
    if (partitions == null) {
      return List.of();
    }
    return partitions.stream()
        .map(info -> buildInfo(groupName, info.partitionId(), info.nodeId(), info.role()))
        .toList();
  }

  @Override
  public Collection<String> getGroupNames() {
    return List.copyOf(globalTopology.keySet());
  }

  @Override
  public Collection<Integer> getPartitionIds(final String groupName) {
    final List<PartitionInfo> partitions = globalTopology.get(groupName);
    return partitions != null
        ? partitions.stream().map(PartitionInfo::partitionId).distinct().sorted().toList()
        : List.of();
  }

  // ===== 序列化 =====

  /** 格式: "group1:1=LEADER,2=FOLLOWER|group2:1=FOLLOWER" */
  private String serializeLocalRoles() {
    final StringBuilder sb = new StringBuilder();
    localRoles.forEach((group, partitions) -> {
      if (sb.length() > 0) {
        sb.append('|');
      }
      sb.append(group).append(':');
      final String entries = partitions.entrySet().stream()
          .map(e -> e.getKey() + "=" + e.getValue().name())
          .reduce((a, b) -> a + "," + b)
          .orElse("");
      sb.append(entries);
    });
    return sb.toString();
  }

  private Map<String, Map<Integer, Role>> deserializeRoles(final String serialized) {
    final Map<String, Map<Integer, Role>> result = new ConcurrentHashMap<>();
    for (final String groupPart : serialized.split("\\|")) {
      final int colonIdx = groupPart.indexOf(':');
      if (colonIdx < 0) {
        continue;
      }
      final String group = groupPart.substring(0, colonIdx);
      final String partitionsStr = groupPart.substring(colonIdx + 1);
      for (final String entry : partitionsStr.split(",")) {
        final int eqIdx = entry.indexOf('=');
        if (eqIdx < 0) {
          continue;
        }
        try {
          final int partitionId = Integer.parseInt(entry.substring(0, eqIdx));
          final Role role = Role.valueOf(entry.substring(eqIdx + 1));
          result.computeIfAbsent(group, k -> new ConcurrentHashMap<>()).put(partitionId, role);
        } catch (final IllegalArgumentException e) {
          LOG.warn("拓扑数据解析失败: {}", entry);
        }
      }
    }
    return result;
  }

  private static String partitionKey(final PartitionId partitionId) {
    return partitionId.group() + "/" + partitionId.id();
  }
}
