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
package com.anyilanxin.kunpeng.cluster.config.topology.broker;

import static com.anyilanxin.kunpeng.cluster.config.BusinessSourceMetaUtils.getSourceMeta;
import static com.anyilanxin.kunpeng.protocol.common.ClusterCommonConstant.TOPOLOGY_PROPERTY_KEY;

import com.anyilanxin.kunpeng.cluster.cluster.ClusterMembershipService;
import com.anyilanxin.kunpeng.cluster.cluster.MemberId;
import com.anyilanxin.kunpeng.cluster.cluster.PartitionId;
import com.anyilanxin.kunpeng.cluster.config.ClusterAdminSerializer;
import com.anyilanxin.kunpeng.cluster.config.ClusterConfigLoggers;
import com.anyilanxin.kunpeng.cluster.config.topology.*;
import com.anyilanxin.kunpeng.cluster.raft.PartitionTopologyListener;
import com.anyilanxin.kunpeng.cluster.raft.RaftBusinessMetaListener;
import com.anyilanxin.kunpeng.cluster.raft.RaftServer;
import com.anyilanxin.kunpeng.cluster.utils.health.HealthReport;
import com.anyilanxin.kunpeng.protocol.common.PartitionSourceMeta;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;

/**
 * Broker 端集群分区拓扑服务：维护本成员全部分区的角色与健康状态，序列化为 {@link SwimPartitionMemberInfo} 后经 SWIM member property
 * 广播，同时汇聚全集群广播数据供查询。
 *
 * @author zxuanhong
 * @since
 */
public class DefaultClusterSwimTopologyService extends AbstractSwimTopologyService
    implements ClusterSwimTopologyService, PartitionTopologyListener, RaftBusinessMetaListener {
  private static final Base64.Encoder BASE_64_ENCODER = Base64.getEncoder();
  public static final Logger LOG = ClusterConfigLoggers.CLUSTER_CONFIG;

  /** 分区健康指标：值越大越健康（HEALTHY=2），<= 0 需要告警关注 */
  private static final String PARTITION_HEALTH_METRIC = "kunpeng.engine.partition.health";

  /** 本成员的分区拓扑广播数据 */
  private final SwimPartitionMemberInfo swimPartitionMemberInfo = new SwimPartitionMemberInfo();

  private final MeterRegistry meterRegistry;

  /** 各分区最新 source 元数据（busimeta 安装后写入；写入与读取均经 actor 线程） */
  private final Map<PartitionId, PartitionSourceMeta> partitionSourceMetas =
      new ConcurrentHashMap<>();

  public DefaultClusterSwimTopologyService(
      final ClusterMembershipService membershipService, final MeterRegistry meterRegistry) {
    super(membershipService);
    this.meterRegistry = meterRegistry;
  }

  @Override
  public MemberId getPartitionLeader(final PartitionId partitionId) {
    return findPartitionLeader(partitionId);
  }

  @Override
  public List<PartitionMemberInfo> getPartitionMemberInfo(final PartitionId partitionId) {
    return partitionMemberInfoOf(partitionId);
  }

  @Override
  public Map<PartitionId, List<PartitionMemberInfo>> getRaftGroup(final String groupName) {
    return raftGroupOf(groupName);
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
          applyToViews(localMember.id(), swimPartitionMemberInfo);
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
          applyToViews(localMember.id(), swimPartitionMemberInfo);
          publishBroadcastInternal();
        });
  }

  /** 移除本成员的分区拓扑条目并重新广播（节点离开分区分组后调用，分区不存在时无副作用）。 */
  public void removePartition(final PartitionId partitionId) {
    actor.submit(
        () -> {
          swimPartitionMemberInfo.remove(partitionId);
          applyToViews(localMember.id(), swimPartitionMemberInfo);
          publishBroadcastInternal();
        });
  }

  /** 获取本成员分区在广播数据中的条目，首次出现时创建，角色与健康由 PartitionMemberInfo 默认 UNKNOWN 兜底。 */
  private PartitionMemberInfo localPartitionInfo(final PartitionId partitionId) {
    final PartitionMemberInfo existing = swimPartitionMemberInfo.getInfoMap().get(partitionId);
    if (existing != null) {
      return existing;
    }
    final PartitionMemberInfo created = new PartitionMemberInfo();
    created.setMemberId(localMember.id().id());
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

  /** 触发广播刷新（外部线程安全入口：投递到 actor 线程执行）。 */
  public void publishBroadcast() {
    actor.submit(this::publishBroadcastInternal);
  }

  /** 刷新本成员各分区 source 信息后，将广播数据序列化以 Base64 写入 member property，随 SWIM gossip 传播 */
  private void publishBroadcastInternal() {
    refreshPartitionSourceInfo();
    final byte[] bytes = ClusterAdminSerializer.SERIALIZER.encode(swimPartitionMemberInfo);
    localMember
        .properties()
        .setProperty(TOPOLOGY_PROPERTY_KEY, BASE_64_ENCODER.encodeToString(bytes));
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
          // 旧元数据即将失效：注销该分区在 swim 广播中的 source 信息
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
          // 读取最新 source 元数据并随 SWIM 广播集群
          partitionSourceMetas.put(partitionId, getSourceMeta(entries));
          publishBroadcastInternal();
        });
  }
}
