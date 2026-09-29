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
package com.anyilanxin.kunpeng.broker.topology;

import static com.anyilanxin.kunpeng.cluster.config.BusinessSourceMetaUtils.getSourceMeta;

import com.anyilanxin.kunpeng.cluster.cluster.ClusterMembershipService;
import com.anyilanxin.kunpeng.cluster.cluster.Member;
import com.anyilanxin.kunpeng.cluster.cluster.PartitionId;
import com.anyilanxin.kunpeng.cluster.config.ClusterConfigLoggers;
import com.anyilanxin.kunpeng.cluster.config.PartitionSourceMeta;
import com.anyilanxin.kunpeng.cluster.config.topology.PartitionHealth;
import com.anyilanxin.kunpeng.cluster.config.topology.PartitionMemberInfo;
import com.anyilanxin.kunpeng.cluster.config.topology.PartitionRole;
import com.anyilanxin.kunpeng.cluster.config.topology.SwimPartitionMemberInfo;
import com.anyilanxin.kunpeng.cluster.raft.RaftServer;
import com.anyilanxin.kunpeng.cluster.utils.health.HealthReport;
import com.anyilanxin.kunpeng.protocol.common.encoding.BrokerInfo;
import com.anyilanxin.kunpeng.scheduler.Actor;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;

/**
 * 拓扑管理服务实现：只做本节点侧——监听分区角色/健康/source 变化，把构造注入的进程唯一 {@link BrokerInfo} 实例写入本地成员属性，传播由 SWIM
 * 底层自行决定。端口等其他子系统数据由其直接 set
 * 该实例（本类是成员属性的唯一写入者）。不做集群收集；集群侧解析汇总由拓扑收集服务（DefaultClusterTopologyService）承担。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class TopologyManagerImpl extends Actor implements TopologyManager {
  private static final Logger LOG = ClusterConfigLoggers.CLUSTER_CONFIG;

  /** 分区健康指标：值越大越健康（HEALTHY=2），<= 0 需要告警关注 */
  private static final String PARTITION_HEALTH_METRIC = "kunpeng.engine.partition.health";

  private final Member localMember;
  private final MeterRegistry meterRegistry;

  /** 本进程唯一的 broker 传播实体实例（分区条目由视图派生重建，端口等字段由各子系统 set 后保留） */
  private final BrokerInfo localBroker;

  /** 本成员的分区拓扑视图模型（本地状态收集的事实源，实体分区条目由其派生） */
  private final SwimPartitionMemberInfo swimPartitionMemberInfo = new SwimPartitionMemberInfo();

  /** 各分区最新 source 元数据（busimeta 安装后写入；写入与读取均经 actor 线程） */
  private final Map<PartitionId, PartitionSourceMeta> partitionSourceMetas =
      new ConcurrentHashMap<>();

  public TopologyManagerImpl(
      final ClusterMembershipService membershipService,
      final MeterRegistry meterRegistry,
      final BrokerInfo localBroker) {
    this.meterRegistry = meterRegistry;
    this.localBroker = localBroker;
    localMember = membershipService.getLocalMember();
  }

  @Override
  public BrokerInfo localBroker() {
    return localBroker;
  }

  @Override
  public void onPartitionRoleChanged(
      final PartitionId partitionId, final RaftServer.Role newRole, final long newTerm) {
    actor.submit(
        () -> {
          final PartitionMemberInfo info = localPartitionInfo(partitionId);
          info.setRole(PartitionRole.of(newRole));
          info.setHealth(PartitionHealth.of(newRole));
          info.setTerm(newTerm);
          publishBroadcastInternal();
        });
  }

  @Override
  public void onPartitionHealthChanged(
      final PartitionId partitionId, final HealthReport healthReport) {
    actor.submit(
        () -> {
          final PartitionMemberInfo info = localPartitionInfo(partitionId);
          info.setHealth(PartitionHealth.of(healthReport.getStatus()));
          publishBroadcastInternal();
        });
  }

  @Override
  public void removePartition(final PartitionId partitionId) {
    actor.submit(
        () -> {
          swimPartitionMemberInfo.remove(partitionId);
          publishBroadcastInternal();
        });
  }

  /** 获取本成员分区在视图模型中的条目，首次出现时创建，角色与健康由 PartitionMemberInfo 默认 UNKNOWN 兜底。 */
  private PartitionMemberInfo localPartitionInfo(final PartitionId partitionId) {
    final PartitionMemberInfo existing = swimPartitionMemberInfo.getInfoMap().get(partitionId);
    if (existing != null) {
      return existing;
    }
    final PartitionMemberInfo created = new PartitionMemberInfo();
    created.setMemberId(localMember.id());
    created.setPartitionId(partitionId);
    swimPartitionMemberInfo.add(created);
    registerHealthGauge(partitionId, created);
    return created;
  }

  /** 分区首次出现时注册健康 gauge：gauge 持条目引用，后续角色/健康更新自动反映 */
  private void registerHealthGauge(final PartitionId partitionId, final PartitionMemberInfo info) {
    Gauge.builder(PARTITION_HEALTH_METRIC, info, memberInfo -> memberInfo.getHealth().getValue())
        .tag("group", partitionId.group())
        .tag("partition", String.valueOf(partitionId.id()))
        .tag("member", localMember.id().id())
        .register(meterRegistry);
  }

  @Override
  public void publishBroadcast() {
    actor.submit(this::publishBroadcastInternal);
  }

  /** 刷新本成员各分区 source 信息后，把视图重建为实体分区条目并写入 member property（端口等字段留在实例上不动） */
  private void publishBroadcastInternal() {
    refreshPartitionSourceInfo();
    localBroker.getPartitions().clear();
    for (final PartitionMemberInfo info : swimPartitionMemberInfo.getInfos()) {
      localBroker.addPartition(toEntry(info));
    }
    localBroker.writeIntoProperties(localMember.properties());
  }

  /** 视图条目 -> broker 实体分区条目 */
  private BrokerInfo.PartitionEntry toEntry(final PartitionMemberInfo info) {
    final BrokerInfo.PartitionEntry entry = new BrokerInfo.PartitionEntry();
    entry.partitionId = info.getPartitionId().id();
    entry.groupName = info.getPartitionId().group();
    entry.role = roleToWire(info.getRole());
    entry.health = healthToWire(info.getHealth());
    entry.term = info.getTerm();
    entry.sourceId = info.getSourceId();
    entry.agentSourceIds =
        List.copyOf(info.getAgentSourceIds() == null ? List.of() : info.getAgentSourceIds());
    return entry;
  }

  /** 线上角色码：业务角色 -> 线上编码（两侧枚举同名对齐） */
  private static com.anyilanxin.kunpeng.protocol.common.member.PartitionRole roleToWire(
      final PartitionRole role) {
    return com.anyilanxin.kunpeng.protocol.common.member.PartitionRole.valueOf(role.name());
  }

  /** 线上健康码：业务健康 -> 线上编码 */
  private static com.anyilanxin.kunpeng.protocol.common.member.PartitionHealth healthToWire(
      final PartitionHealth health) {
    return com.anyilanxin.kunpeng.protocol.common.member.PartitionHealth.valueOf(health.name());
  }

  /** 从 busimeta 缓存读取本成员各分区的 sourceId 与 agentSourceIds；未安装元数据的分区保留现值 */
  private void refreshPartitionSourceInfo() {
    for (final PartitionMemberInfo info : swimPartitionMemberInfo.getInfos()) {
      final PartitionSourceMeta sourceMeta = partitionSourceMetas.get(info.getPartitionId());
      if (sourceMeta == null) {
        continue;
      }
      info.setSourceId(sourceMeta.sourceId());
      info.setAgentSourceIds(Set.copyOf(sourceMeta.agentSourceIds()));
    }
  }

  @Override
  public void onStarted(
      final PartitionId partitionId,
      final Map<String, String> entries,
      final long currentTerm,
      final RaftServer.Role role) {
    actor.submit(
        () -> {
          // 旧元数据即将失效：注销该分区在广播中的 source 信息
          partitionSourceMetas.remove(partitionId);
          final PartitionMemberInfo info = swimPartitionMemberInfo.getInfoMap().get(partitionId);
          if (info != null) {
            info.setSourceId(-1);
            info.setAgentSourceIds(Set.of());
          }
        });
  }

  @Override
  public void onCompleted(
      final PartitionId partitionId,
      final Map<String, String> entries,
      final long currentTerm,
      final RaftServer.Role role) {
    actor.submit(
        () -> {
          // 读取最新 source 元数据并写入实体属性
          partitionSourceMetas.put(partitionId, getSourceMeta(entries));
          publishBroadcastInternal();
        });
  }
}
