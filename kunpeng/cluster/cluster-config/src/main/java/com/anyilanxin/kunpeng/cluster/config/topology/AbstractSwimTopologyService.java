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

import static com.anyilanxin.kunpeng.protocol.common.ClusterCommonConstant.TOPOLOGY_PROPERTY_KEY;

import com.anyilanxin.kunpeng.cluster.cluster.*;
import com.anyilanxin.kunpeng.cluster.config.ClusterAdminSerializer;
import com.anyilanxin.kunpeng.scheduler.Actor;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 基于 SWIM member property 广播的拓扑视图服务基类：监听成员事件，读取并解码 {@link SwimPartitionMemberInfo}
 * 广播数据，维护成员/分区两个维度的拓扑视图供查询；子类按需叠加本地广播发布逻辑。
 *
 * @author zxuanhong
 * @since
 */
public abstract class AbstractSwimTopologyService extends Actor
    implements ClusterMembershipEventListener {
  private static final Logger LOG = LoggerFactory.getLogger(AbstractSwimTopologyService.class);
  private static final Base64.Decoder BASE_64_DECODER = Base64.getDecoder();

  protected final Member localMember;

  private final ClusterMembershipService membershipService;

  /** 成员维度视图：member -> 其广播的全部分区状态 */
  private final Map<MemberId, Set<PartitionMemberInfo>> memberPartitionInfos =
      new ConcurrentHashMap<>();

  /** 分区维度视图：partition -> 各成员的分区状态 */
  private final Map<PartitionId, Set<PartitionMemberInfo>> partitionMemberInfos =
      new ConcurrentHashMap<>();

  /** 分区 Leader 专表：partition -> 当前认定的 Leader 状态，高频查询走 O(1) 读取 */
  private final Map<PartitionId, PartitionMemberInfo> partitionLeaders = new ConcurrentHashMap<>();

  protected AbstractSwimTopologyService(final ClusterMembershipService membershipService) {
    this.membershipService = membershipService;
    localMember = membershipService.getLocalMember();
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

  /** 读取成员广播的拓扑数据（Base64 -> Kryo 解码）并刷新视图 */
  protected void refreshMember(final Member member) {
    final String value = member.properties().getProperty(TOPOLOGY_PROPERTY_KEY);
    if (value == null) {
      return;
    }
    try {
      applyToViews(member.id(), decodeBroadcast(value));
    } catch (final Exception e) {
      LOG.warn("Failed to decode topology broadcast from member {}", member.id(), e);
    }
  }

  /** 以成员最新广播覆盖其视图，并按分区维度重建 */
  protected void applyToViews(final MemberId memberId, final SwimPartitionMemberInfo broadcast) {
    final Set<PartitionMemberInfo> infos =
        memberPartitionInfos.computeIfAbsent(memberId, ignored -> ConcurrentHashMap.newKeySet());
    infos.clear();
    infos.addAll(broadcast.getInfos());
    rebuildPartitionViews();
  }

  /** 成员离线时移除其视图并按分区维度重建 */
  protected void removeMember(final MemberId memberId) {
    if (memberPartitionInfos.remove(memberId) != null) {
      rebuildPartitionViews();
    }
  }

  /** 按成员维度的只读拓扑快照：member -> 其广播的分区状态列表 */
  protected Map<MemberId, List<PartitionMemberInfo>> memberPartitionsView() {
    final Map<MemberId, List<PartitionMemberInfo>> snapshot = new ConcurrentHashMap<>();
    memberPartitionInfos.forEach((memberId, infos) -> snapshot.put(memberId, List.copyOf(infos)));
    return snapshot;
  }

  /** 以 memberPartitionInfos 为准重建分区视图与 Leader 专表，避免残留成员已不再持有的分区旧状态 */
  private void rebuildPartitionViews() {
    partitionMemberInfos.clear();
    partitionLeaders.clear();
    memberPartitionInfos.forEach(
        (memberId, infos) ->
            infos.forEach(
                info -> {
                  partitionMemberInfos
                      .computeIfAbsent(
                          info.getPartitionId(), ignored -> ConcurrentHashMap.newKeySet())
                      .add(info);
                  if (info.getRole() == PartitionRole.LEADER) {
                    // 选举窗口期可能多个成员广播 LEADER：保留 term 更大者
                    partitionLeaders.merge(
                        info.getPartitionId(),
                        info,
                        (current, candidate) ->
                            candidate.getTerm() > current.getTerm() ? candidate : current);
                  }
                }));
  }

  private SwimPartitionMemberInfo decodeBroadcast(final String base64) {
    final byte[] bytes = BASE_64_DECODER.decode(base64.getBytes(StandardCharsets.UTF_8));
    return ClusterAdminSerializer.SERIALIZER.decode(bytes);
  }

  /** 查询分区当前的 Leader（主成员），分区未知或暂无主时返回 null；多成员自称 Leader 时取 term 最大者 */
  protected MemberId findPartitionLeader(final PartitionId partitionId) {
    final PartitionMemberInfo leader = partitionLeaders.get(partitionId);
    return leader == null ? null : MemberId.from(leader.getMemberId());
  }

  /** 查询分区各成员的拓扑状态 */
  protected List<PartitionMemberInfo> partitionMemberInfoOf(final PartitionId partitionId) {
    final Set<PartitionMemberInfo> infos = partitionMemberInfos.get(partitionId);
    return infos == null ? List.of() : List.copyOf(infos);
  }

  /** 查询指定分区分组内全部分区及各成员的拓扑状态 */
  protected Map<PartitionId, List<PartitionMemberInfo>> raftGroupOf(final String groupName) {
    final Map<PartitionId, List<PartitionMemberInfo>> result = new ConcurrentHashMap<>();
    partitionMemberInfos.forEach(
        (partitionId, infos) -> {
          if (partitionId.group().equals(groupName)) {
            result.put(partitionId, List.copyOf(infos));
          }
        });
    return result;
  }
}
