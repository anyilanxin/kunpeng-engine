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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.broker.topology;

import com.anyilanxin.kunpeng.cluster.config.topology.PartitionMemberInfo;
import com.anyilanxin.kunpeng.cluster.config.topology.PartitionRole;
import com.anyilanxin.kunpeng.cluster.config.topology.broker.ClusterSwimTopologyService;
import com.anyilanxin.kunpeng.protocol.common.ClusterCommonConstant;
import com.anyilanxin.kunpeng.protocol.common.PartitionSourceMetadata;
import com.anyilanxin.kunpeng.scheduler.Actor;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 分区拓扑通知器：周期比对 SWIM 拓扑视图中各业务分区的 leader，变化时通知注册的 {@link TopologyPartitionListener} 刷新路由（如分区间命令发送器的
 * setCurrentLeader）。
 *
 * <p>统一 actor 调度（单发定时 + 回调重排，不持独立线程池）。周期快照比对而非事件挂接：SWIM 视图本身是最终一致的汇聚结果，
 * 比对自愈且无侵入——漏一轮下一轮补上，重复通知幂等（listener 侧覆盖写）。 通知在 actor 线程执行，listener 实现方自行保证落点线程安全（如经自身 actor 转发）。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class PartitionTopologyNotifier extends Actor {
  private static final Logger LOG = LoggerFactory.getLogger(PartitionTopologyNotifier.class);

  private final ClusterSwimTopologyService topologyService;
  private final Duration pollInterval;
  private final Map<Integer, String> lastLeaders = new ConcurrentHashMap<>();
  private final CopyOnWriteArraySet<TopologyPartitionListener> listeners =
      new CopyOnWriteArraySet<>();

  public PartitionTopologyNotifier(
      final ClusterSwimTopologyService topologyService, final Duration pollInterval) {
    this.topologyService = topologyService;
    this.pollInterval = pollInterval;
  }

  public void addListener(final TopologyPartitionListener listener) {
    listeners.add(listener);
  }

  public void removeListener(final TopologyPartitionListener listener) {
    listeners.remove(listener);
  }

  @Override
  public String getName() {
    return "partition-topology-notifier";
  }

  @Override
  protected void onActorStarted() {
    schedulePoll();
  }

  private void schedulePoll() {
    actor.schedule(
        pollInterval,
        () -> {
          try {
            poll();
          } catch (final Throwable t) {
            LOG.error("Partition topology poll failed, will retry next cycle", t);
          }
          schedulePoll();
        });
  }

  private void poll() {
    final var raftGroup = topologyService.getRaftGroup(ClusterCommonConstant.BUSINESS_RAFT_GROUP);
    if (raftGroup == null || raftGroup.isEmpty()) {
      return;
    }
    for (final var entry : raftGroup.entrySet()) {
      final int partitionId = entry.getKey().id();
      PartitionMemberInfo leader = null;
      for (final PartitionMemberInfo member : entry.getValue()) {
        if (member.getRole() == PartitionRole.LEADER) {
          leader = member;
          break;
        }
      }
      if (leader == null) {
        continue; // 该分区暂无 leader（选举中）：保留旧路由，下轮再试
      }
      final String leaderMemberId = leader.getMemberId();
      final String previous = lastLeaders.put(partitionId, leaderMemberId);
      if (leaderMemberId.equals(previous)) {
        continue;
      }
      LOG.debug(
          "Partition leader changed, notifying route listeners [partition: {}, leader: {}]",
          partitionId,
          leaderMemberId);
      final var sourceMetadata =
          new PartitionSourceMetadata(
              partitionId,
              leader.getSourceId(),
              leader.getAgentSourceIds() == null
                  ? com.google.common.collect.ImmutableSet.of()
                  : com.google.common.collect.ImmutableSet.copyOf(leader.getAgentSourceIds()));
      for (final TopologyPartitionListener listener : listeners) {
        try {
          listener.onPartitionLeaderUpdated(sourceMetadata, leader);
        } catch (final Throwable t) {
          LOG.error(
              "Topology listener failed [partition: {}, listener: {}]",
              partitionId,
              listener.getClass().getSimpleName(),
              t);
        }
      }
    }
  }
}
