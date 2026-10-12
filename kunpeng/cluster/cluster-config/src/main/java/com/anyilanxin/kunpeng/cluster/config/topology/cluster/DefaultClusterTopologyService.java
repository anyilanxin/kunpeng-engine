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
package com.anyilanxin.kunpeng.cluster.config.topology.cluster;

import static com.anyilanxin.kunpeng.protocol.common.ClusterCommonConstant.ADMIN_RAFT_GROUP;

import com.anyilanxin.kunpeng.cluster.cluster.ClusterMembershipEvent;
import com.anyilanxin.kunpeng.cluster.cluster.ClusterMembershipEventListener;
import com.anyilanxin.kunpeng.cluster.cluster.ClusterMembershipService;
import com.anyilanxin.kunpeng.cluster.cluster.Member;
import com.anyilanxin.kunpeng.cluster.cluster.MemberId;
import com.anyilanxin.kunpeng.cluster.cluster.PartitionId;
import com.anyilanxin.kunpeng.cluster.config.topology.PartitionHealth;
import com.anyilanxin.kunpeng.cluster.config.topology.PartitionMemberInfo;
import com.anyilanxin.kunpeng.cluster.config.topology.PartitionRole;
import com.anyilanxin.kunpeng.cluster.config.topology.SwimPartitionMemberInfo;
import com.anyilanxin.kunpeng.cluster.utils.net.Address;
import com.anyilanxin.kunpeng.protocol.common.encoding.BrokerInfo;
import com.anyilanxin.kunpeng.protocol.common.member.CommPortType;
import com.anyilanxin.kunpeng.scheduler.Actor;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.agrona.collections.Int2ObjectHashMap;
import org.agrona.collections.IntHashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 集群分区拓扑收集服务：监听成员事件，读取各成员 SWIM 属性中的 {@link BrokerInfo} 实体并汇总为成员/分区两个维度的只读拓扑视图， 供
 * broker、broker-client 与拓扑端点查询；本节点数据的管理与广播由 broker 侧的 TopologyManager 负责。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class DefaultClusterTopologyService extends Actor
    implements ClusterTopologyService, ClusterMembershipEventListener {
  private static final Logger LOG = LoggerFactory.getLogger(DefaultClusterTopologyService.class);

  private final ClusterMembershipService membershipService;

  /** 成员维度视图：member -> 其广播的全部分区状态 */
  private final Map<MemberId, Set<PartitionMemberInfo>> memberPartitionInfos =
      new ConcurrentHashMap<>();

  /**
   * 分区派生视图快照：五张表单次重建、单一 volatile 引用整体换入。 读者拿到的一组视图永远来自同一次重建——避免 clear-then-refill
   * 期间跨线程读者看到空视图（瞬时报"无 leader 可用"）。
   */
  private volatile PartitionViews views = emptyViews();

  /** 分区派生视图组：分区成员表、Leader 专表、来源映射与来源/分区集合。 发布后只读；agrona 容器经 volatile 写安全发布。 */
  private record PartitionViews(
      Map<PartitionId, Set<PartitionMemberInfo>> partitionMemberInfos,
      Map<PartitionId, PartitionMemberInfo> partitionLeaders,
      Int2ObjectHashMap<PartitionId> partitionSourceIds,
      IntHashSet partitionSources,
      IntHashSet businessPartitions) {}

  private static PartitionViews emptyViews() {
    return new PartitionViews(
        new ConcurrentHashMap<>(),
        new ConcurrentHashMap<>(),
        new Int2ObjectHashMap<>(),
        new IntHashSet(),
        new IntHashSet());
  }

  /** 成员业务面地址视图：member -> 事件到达时解析合成的业务地址（查询零解析） */
  private final Map<MemberId, Address> businessAddresses = new ConcurrentHashMap<>();

  /** 拓扑版本：每次视图重建后在 actor 线程递增，读取方凭它缓存快照 */
  private volatile long topologyVersion;

  public DefaultClusterTopologyService(final ClusterMembershipService membershipService) {
    this.membershipService = membershipService;
  }

  @Override
  protected void onActorStarted() {
    membershipService.addListener(this);
    checkForMissingEvents();
  }

  @Override
  protected void onActorClosing() {
    membershipService.removeListener(this);
  }

  @Override
  public void event(final ClusterMembershipEvent event) {
    actor.submit(
        () -> {
          switch (event.type()) {
            case MEMBER_ADDED, METADATA_CHANGED -> refreshMember(event.subject());
            case MEMBER_REMOVED -> removeMember(event.subject().id());
            default -> {}
          }
        });
  }

  /** 启动时补拉当前已在线成员的拓扑广播，避免依赖后续 gossip 事件 */
  private void checkForMissingEvents() {
    final Set<Member> members = membershipService.getMembers();
    if (members == null || members.isEmpty()) {
      return;
    }
    for (final Member member : members) {
      refreshMember(member);
    }
  }

  /** 读取成员属性中的 broker 实体并刷新视图（缺属性/非法帧静默跳过，兼容滚动升级与未发布成员） */
  private void refreshMember(final Member member) {
    final BrokerInfo brokerInfo = BrokerInfo.fromProperties(member.properties());
    if (brokerInfo == null) {
      return;
    }
    try {
      final Address businessAddress = businessAddressOf(member, brokerInfo);
      if (businessAddress == null) {
        businessAddresses.remove(member.id());
      } else {
        businessAddresses.put(member.id(), businessAddress);
      }
      applyToViews(member.id(), toViews(member.id(), brokerInfo));
    } catch (final Exception e) {
      businessAddresses.remove(member.id());
      LOG.warn("Failed to decode topology broadcast from member {}", member.id(), e);
    }
  }

  /**
   * 事件到达时解析一次：成员集群面地址的主机 + 实体中的业务端口 -> 业务面地址（同主机独立端口；未广播端口为 {@code null}）。 主机取 {@code
   * member.address()}（构造必填），不用可空的 {@code member.host()} 元数据。
   */
  private static Address businessAddressOf(final Member member, final BrokerInfo brokerInfo) {
    final Integer port = brokerInfo.getPorts().get(CommPortType.BUSINESS);
    return port == null ? null : Address.from(member.address().host(), port);
  }

  /** broker 实体 -> 拓扑视图模型（分区条目逐项映射，枚举按名对齐） */
  private SwimPartitionMemberInfo toViews(final MemberId memberId, final BrokerInfo brokerInfo) {
    final SwimPartitionMemberInfo views = new SwimPartitionMemberInfo();
    for (final BrokerInfo.PartitionEntry entry : brokerInfo.getPartitions()) {
      final PartitionMemberInfo info = new PartitionMemberInfo();
      info.setMemberId(memberId);
      info.setPartitionId(PartitionId.from(entry.groupName, entry.partitionId));
      info.setRole(roleFromWire(entry.role));
      info.setHealth(healthFromWire(entry.health));
      info.setTerm(entry.term);
      info.setSourceId(entry.sourceId);
      info.setAgentSourceIds(Set.copyOf(entry.agentSourceIds));
      views.add(info);
    }
    return views;
  }

  /** 线上角色码 -> 业务角色（未知码/缺省归 UNKNOWN，两侧枚举同名对齐） */
  private static PartitionRole roleFromWire(
      final com.anyilanxin.kunpeng.protocol.common.member.PartitionRole wire) {
    if (wire == null) {
      return PartitionRole.UNKNOWN;
    }
    try {
      return PartitionRole.valueOf(wire.name());
    } catch (final IllegalArgumentException e) {
      return PartitionRole.UNKNOWN;
    }
  }

  /** 线上健康码 -> 业务健康（未知码/缺省归 UNKNOWN，两侧枚举同名对齐） */
  private static PartitionHealth healthFromWire(
      final com.anyilanxin.kunpeng.protocol.common.member.PartitionHealth wire) {
    if (wire == null) {
      return PartitionHealth.UNKNOWN;
    }
    try {
      return PartitionHealth.valueOf(wire.name());
    } catch (final IllegalArgumentException e) {
      return PartitionHealth.UNKNOWN;
    }
  }

  /** 以成员最新广播覆盖其视图，并按分区维度重建 */
  private void applyToViews(final MemberId memberId, final SwimPartitionMemberInfo broadcast) {
    final Set<PartitionMemberInfo> infos =
        memberPartitionInfos.computeIfAbsent(memberId, ignored -> ConcurrentHashMap.newKeySet());
    infos.clear();
    infos.addAll(broadcast.getInfos());
    rebuildPartitionViews();
  }

  /** 成员离线时移除其视图并按分区维度重建 */
  private void removeMember(final MemberId memberId) {
    businessAddresses.remove(memberId);
    if (memberPartitionInfos.remove(memberId) != null) {
      rebuildPartitionViews();
    }
  }

  /**
   * 以 memberPartitionInfos 为准重建分区派生视图： 本地构建全新一组容器后经单一 volatile 引用整体换入，
   * 避免残留成员已不再持有的分区旧状态，也避免读者看到清空与回填之间的空窗。
   */
  private void rebuildPartitionViews() {
    final Map<PartitionId, Set<PartitionMemberInfo>> memberInfos = new ConcurrentHashMap<>();
    final Map<PartitionId, PartitionMemberInfo> leaders = new ConcurrentHashMap<>();
    final Int2ObjectHashMap<PartitionId> sourceIds = new Int2ObjectHashMap<>();
    final IntHashSet sources = new IntHashSet();
    final IntHashSet business = new IntHashSet();
    memberPartitionInfos.forEach(
        (memberId, infos) ->
            infos.forEach(
                info -> {
                  memberInfos
                      .computeIfAbsent(
                          info.getPartitionId(), ignored -> ConcurrentHashMap.newKeySet())
                      .add(info);
                  if (info.getRole() == PartitionRole.LEADER) {
                    // 选举窗口期可能多个成员广播 LEADER：保留 term 更大者
                    leaders.merge(
                        info.getPartitionId(),
                        info,
                        (current, candidate) ->
                            candidate.getTerm() > current.getTerm() ? candidate : current);
                    sourceIds.put(info.getSourceId(), info.getPartitionId());
                    sources.add(info.getSourceId());
                    if (!ADMIN_RAFT_GROUP.equalsIgnoreCase(info.getPartitionId().group())) {
                      business.add(info.getPartitionId().id());
                    }
                    if (info.getAgentSourceIds() != null) {
                      // 代理来源集合逐项登记（v 是代理来源 id，不能误加自身 sourceId）
                      info.getAgentSourceIds()
                          .forEach(
                              v -> {
                                sources.add(v);
                                sourceIds.put(v, info.getPartitionId());
                              });
                    }
                  }
                }));
    views = new PartitionViews(memberInfos, leaders, sourceIds, sources, business);
    topologyVersion++;
  }

  @Override
  public long getTopologyVersion() {
    return topologyVersion;
  }

  @Override
  public MemberId getPartitionLeader(final PartitionId partitionId) {
    // 多成员自称 Leader 的选举窗口期取 term 最大者
    final PartitionMemberInfo leader = views.partitionLeaders().get(partitionId);
    return leader == null ? null : leader.getMemberId();
  }

  @Override
  public Address getPartitionBusinessAddress(final PartitionId partitionId) {
    final PartitionMemberInfo leader = views.partitionLeaders().get(partitionId);
    return leader == null ? null : businessAddresses.get(leader.getMemberId());
  }

  @Override
  public List<PartitionMemberInfo> getPartitionMemberInfo(final PartitionId partitionId) {
    final Set<PartitionMemberInfo> infos = views.partitionMemberInfos().get(partitionId);
    return infos == null ? List.of() : List.copyOf(infos);
  }

  @Override
  public Map<PartitionId, List<PartitionMemberInfo>> getRaftGroup(final String groupName) {
    final Map<PartitionId, List<PartitionMemberInfo>> result = new ConcurrentHashMap<>();
    views
        .partitionMemberInfos()
        .forEach(
            (partitionId, infos) -> {
              if (partitionId.group().equals(groupName)) {
                result.put(partitionId, List.copyOf(infos));
              }
            });
    return result;
  }

  @Override
  public Map<MemberId, List<PartitionMemberInfo>> getMemberPartitions() {
    final Map<MemberId, List<PartitionMemberInfo>> snapshot = new ConcurrentHashMap<>();
    memberPartitionInfos.forEach((memberId, infos) -> snapshot.put(memberId, List.copyOf(infos)));
    return snapshot;
  }

  @Override
  public IntHashSet getActivitySourceIds() {
    // 返回快照拷贝：内部容器非线程安全且不暴露给调用方修改
    final IntHashSet copy = new IntHashSet();
    views.partitionSources().forEachInt(copy::add);
    return copy;
  }

  @Override
  public IntHashSet getActivityPartitionIds() {
    final IntHashSet copy = new IntHashSet();
    views.businessPartitions().forEachInt(copy::add);
    return copy;
  }

  @Override
  public PartitionId getPartitionBySourceId(final int sourceId) {
    return views.partitionSourceIds().get(sourceId);
  }

  @Override
  public int getPartitionSource(final PartitionId partitionId) {
    final PartitionMemberInfo leader = views.partitionLeaders().get(partitionId);
    // 无 leader 即前置条件不满足：明确失败，不用 -1 之类的哨兵值污染下游寻址
    if (leader == null) {
      throw new IllegalStateException("No leader known for partition " + partitionId);
    }
    return leader.getSourceId();
  }
}
