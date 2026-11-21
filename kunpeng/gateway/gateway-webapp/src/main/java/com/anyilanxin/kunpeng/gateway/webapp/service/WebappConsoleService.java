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
package com.anyilanxin.kunpeng.gateway.webapp.service;

import static com.anyilanxin.kunpeng.protocol.common.ClusterCommonConstant.NODE_SOURCE_PROPERTY_KEY;

import com.anyilanxin.kunpeng.broker.client.admin.ClusterDispatchClient;
import com.anyilanxin.kunpeng.cluster.cluster.AtomixCluster;
import com.anyilanxin.kunpeng.cluster.cluster.Member;
import com.anyilanxin.kunpeng.cluster.cluster.PartitionId;
import com.anyilanxin.kunpeng.cluster.config.topology.PartitionMemberInfo;
import com.anyilanxin.kunpeng.cluster.config.topology.cluster.ClusterTopologyService;
import com.anyilanxin.kunpeng.gateway.grpc.gatewaydiscover.GatewayTopologyManager;
import com.anyilanxin.kunpeng.gateway.job.GatewayJobHub;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.admin.AdminClusterMetaRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.admin.AdminDispatchPlanRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.business.BusinessClusterMetaRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.business.BusinessDispatchPlanExecutionRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.business.BusinessDispatchPlanRecord;
import com.anyilanxin.kunpeng.protocol.admin.record.command.PartitionInfoMetaRecordValue;
import com.anyilanxin.kunpeng.utils.VersionUtil;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import org.springframework.stereotype.Service;

/**
 * 控制台数据聚合服务：拓扑/分区（含 sourceId 实时视图）/job 流/网关/调度查询，全部读本进程运行时视图与既有管理面查询链路， 不新建任何写路径。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@Service
public class WebappConsoleService {
  private final ClusterTopologyService topologyService;
  private final GatewayTopologyManager gatewayTopologyManager;
  private final GatewayJobHub jobHub;
  private final ClusterDispatchClient dispatchClient;
  private final AtomixCluster atomixCluster;

  public WebappConsoleService(
      final ClusterTopologyService topologyService,
      final GatewayTopologyManager gatewayTopologyManager,
      final GatewayJobHub jobHub,
      final ClusterDispatchClient dispatchClient,
      final AtomixCluster atomixCluster) {
    this.topologyService = topologyService;
    this.gatewayTopologyManager = gatewayTopologyManager;
    this.jobHub = jobHub;
    this.dispatchClient = dispatchClient;
    this.atomixCluster = atomixCluster;
  }

  /** 控制台快照模型：broker 全量节点（含服务地址、Node Source ID 与逐副本明细），供分区矩阵与节点页使用 */
  public List<BrokerSnapshotVo> brokerSnapshots() {
    final Map<String, Member> members = new TreeMap<>();
    for (final Member member : atomixCluster.getMembershipService().getMembers()) {
      members.put(member.id().id(), member);
    }
    final List<BrokerSnapshotVo> snapshots = new ArrayList<>();
    for (final var entry : topologyService.getMemberPartitions().entrySet()) {
      final String memberId = entry.getKey().id();
      final Member member = members.get(memberId);
      final List<PartitionSnapshotVo> partitions = new ArrayList<>();
      for (final PartitionMemberInfo info : entry.getValue()) {
        partitions.add(
            new PartitionSnapshotVo(
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
      partitions.sort(
          Comparator.comparing(PartitionSnapshotVo::group)
              .thenComparingInt(PartitionSnapshotVo::id));
      snapshots.add(
          new BrokerSnapshotVo(
              memberId, member.address().toString(), nodeSourceId(member), partitions));
    }
    return snapshots;
  }

  /** 控制台快照模型：本网关 Worker 连接与订阅关系（单网关进程视角；broker 内嵌网关即本节点网关） */
  public List<JobWorkerSnapshotVo> workerSnapshots() {
    final Member local = atomixCluster.getMembershipService().getLocalMember();
    final String memberId = local.id().id();
    final String address = local.address().toString();
    final Map<String, List<GatewayJobHub.StreamInfo>> streamsByType = new TreeMap<>();
    for (final var info : jobHub.describe()) {
      streamsByType.computeIfAbsent(info.jobType(), key -> new ArrayList<>()).add(info);
    }
    final List<GatewayConnectionVo> connections = new ArrayList<>();
    final List<JobSubscriptionVo> subscriptions = new ArrayList<>();
    for (final var entry : streamsByType.entrySet()) {
      final List<WorkerSessionVo> sessions = new ArrayList<>();
      final List<SubscriptionSessionVo> subscriptionSessions = new ArrayList<>();
      for (final var info : entry.getValue()) {
        sessions.add(
            new WorkerSessionVo(
                info.streamId(),
                info.worker(),
                info.capacity(),
                info.tenantIds(),
                info.fetchVariables()));
        subscriptionSessions.add(
            new SubscriptionSessionVo(memberId, address, info.streamId(), info.worker()));
      }
      connections.add(new GatewayConnectionVo(entry.getKey(), sessions));
      subscriptions.add(new JobSubscriptionVo(entry.getKey(), subscriptionSessions));
    }
    return List.of(
        new JobWorkerSnapshotVo(
            new GatewaySnapshotVo(memberId, address, nodeSourceId(local), connections),
            subscriptions));
  }

  private static int nodeSourceId(final Member member) {
    final String value = member.properties().getProperty(NODE_SOURCE_PROPERTY_KEY);
    if (value == null) {
      throw new IllegalStateException("Node source id not initialized for member " + member.id());
    }
    return Integer.parseInt(value);
  }

  /** 总览关键数字 */
  public OverviewVo overview() {
    final var partitions = distinctPartitions();
    int frozen = 0;
    for (final PartitionId id : partitions) {
      if (topologyService.isPartitionFrozen(id)) {
        frozen++;
      }
    }
    return new OverviewVo(
        topologyService.getMemberPartitions().size(),
        VersionUtil.getVersion(),
        partitions.size(),
        topologyService.getActivityPartitionIds().size(),
        gatewayTopologyManager.getGateways().size(),
        topologyService.getTopologyVersion(),
        frozen);
  }

  /** 拓扑矩阵：broker 维度 + 分区维度（含 raft 成员、leader、sourceId） */
  public TopologyVo topology() {
    final Map<String, List<BrokerPartitionRef>> brokers = new TreeMap<>();
    for (final var entry : topologyService.getMemberPartitions().entrySet()) {
      final List<PartitionMemberInfo> infos = entry.getValue();
      if (infos == null || infos.isEmpty()) {
        continue;
      }
      final List<BrokerPartitionRef> refs = new ArrayList<>();
      for (final PartitionMemberInfo info : infos) {
        refs.add(
            new BrokerPartitionRef(
                info.getPartitionId().group(),
                info.getPartitionId().id(),
                info.getRole().name(),
                info.getHealth().name()));
      }
      refs.sort(
          Comparator.comparing(BrokerPartitionRef::group)
              .thenComparingInt(BrokerPartitionRef::partitionId));
      brokers.put(entry.getKey().id(), refs);
    }
    final List<PartitionVo> partitions =
        distinctPartitions().stream().map(this::partitionDetail).toList();
    return new TopologyVo(
        brokers.entrySet().stream().map(e -> new BrokerVo(e.getKey(), e.getValue())).toList(),
        partitions);
  }

  /** 分区详情：raft 成员/leader/业务地址/sourceId/agentSourceIds/冻结态/任期 */
  public PartitionVo partition(final String group, final int id) {
    return partitionDetail(PartitionId.from(group, id));
  }

  /** 网关发现列表 */
  public List<GatewayVo> gateways() {
    return gatewayTopologyManager.getGateways().stream()
        .map(
            info ->
                new GatewayVo(
                    info.getNodeId(),
                    info.getVersion(),
                    info.getGrpcAddress(),
                    info.isAllowClientDiscovery()))
        .toList();
  }

  /** 网关负载查询 */
  public CompletableFuture<GatewayLoadVo> gatewayLoad(final String grpcAddress) {
    return gatewayTopologyManager
        .getGatewayLoad(grpcAddress)
        .thenApply(record -> new GatewayLoadVo(record.nodeId(), record.connectorNum()));
  }

  /** 本网关 job 流连接视图（按 jobType 分组，会话按 streamId 升序） */
  public List<JobTypeVo> jobStreams() {
    final Map<String, List<JobStreamVo>> byType = new TreeMap<>();
    for (final var info : jobHub.describe()) {
      byType
          .computeIfAbsent(info.jobType(), key -> new ArrayList<>())
          .add(
              new JobStreamVo(
                  info.streamId(),
                  info.worker(),
                  info.capacity(),
                  info.timeoutMillis(),
                  info.tenantIds(),
                  info.fetchVariables()));
    }
    return byType.entrySet().stream().map(e -> new JobTypeVo(e.getKey(), e.getValue())).toList();
  }

  /** 调度查询聚合：业务面（clusterMeta+最新计划+执行明细）+ 管理面（admin 元数据+计划） */
  public DispatchVo dispatch() {
    final var business = dispatchClient.businessDispatchQuery().join();
    final var admin = dispatchClient.adminDispatchQuery().join();
    return new DispatchVo(
        business.isSuccess() && business.getValue() != null
            ? BusinessDispatch.of(business.getValue())
            : null,
        admin.isSuccess() && admin.getValue() != null ? AdminDispatch.of(admin.getValue()) : null);
  }

  private PartitionVo partitionDetail(final PartitionId id) {
    final List<PartitionMemberVo> members =
        topologyService.getPartitionMemberInfo(id).stream()
            .map(
                info ->
                    new PartitionMemberVo(
                        info.getMemberId().id(),
                        info.getRole().name(),
                        info.getHealth().name(),
                        info.getTerm(),
                        info.getSourceId(),
                        info.getAgentSourceIds() == null
                            ? List.of()
                            : List.copyOf(info.getAgentSourceIds())))
            .sorted(Comparator.comparing(PartitionMemberVo::memberId))
            .toList();
    final var leader = topologyService.getPartitionLeader(id);
    final var address = topologyService.getPartitionBusinessAddress(id);
    return new PartitionVo(
        id.group(),
        id.id(),
        leader == null ? null : leader.id(),
        members,
        topologyService.getPartitionSource(id),
        topologyService.getActivityPartitionIds().contains(id.id()),
        topologyService.isPartitionFrozen(id),
        address == null ? null : address.toString());
  }

  private List<PartitionId> distinctPartitions() {
    final Map<String, PartitionId> ids = new TreeMap<>();
    for (final var entry : topologyService.getMemberPartitions().entrySet()) {
      final List<PartitionMemberInfo> infos = entry.getValue();
      if (infos == null) {
        continue;
      }
      for (final PartitionMemberInfo info : infos) {
        final PartitionId pid = info.getPartitionId();
        ids.putIfAbsent(pid.group() + ":" + pid.id(), pid);
      }
    }
    return List.copyOf(ids.values());
  }

  // ===== 视图记录 =====

  /** 前端节点/分区矩阵快照：broker 节点 */
  public record BrokerSnapshotVo(
      String memberId, String address, int nodeSourceId, List<PartitionSnapshotVo> partitions) {}

  /** 前端节点/分区矩阵快照：单个分区副本 */
  public record PartitionSnapshotVo(
      String group,
      int id,
      String role,
      String health,
      long term,
      int sourceId,
      List<Integer> agentSourceIds) {}

  /** 前端 Worker 页快照：网关与其订阅镜像 */
  public record JobWorkerSnapshotVo(
      GatewaySnapshotVo gateway, List<JobSubscriptionVo> subscriptions) {}

  /** 前端 Worker 页快照：网关 */
  public record GatewaySnapshotVo(
      String memberId, String address, int nodeSourceId, List<GatewayConnectionVo> connections) {}

  /** 前端 Worker 页快照：按 jobType 分组的连接 */
  public record GatewayConnectionVo(String jobType, List<WorkerSessionVo> sessions) {}

  /** 前端 Worker 页快照：Worker 会话 */
  public record WorkerSessionVo(
      long sessionId,
      String worker,
      int capacity,
      List<String> tenantIds,
      List<String> fetchVariables) {}

  /** 前端 Worker 页快照：jobType 订阅关系 */
  public record JobSubscriptionVo(String jobType, List<SubscriptionSessionVo> sessions) {}

  /** 前端 Worker 页快照：订阅会话 */
  public record SubscriptionSessionVo(
      String gateway, String address, long sessionId, String worker) {}

  /** 总览：集群规模/版本/分区数/活动分区/网关数/拓扑版本/冻结分区数 */
  public record OverviewVo(
      int clusterSize,
      String version,
      int partitionsCount,
      int activePartitions,
      int gateways,
      long topologyVersion,
      int frozenPartitions) {}

  /** broker 节点：成员 id + 其承载分区角色/健康引用 */
  public record BrokerVo(String memberId, List<BrokerPartitionRef> partitions) {}

  /** 拓扑矩阵：broker 维度视图 + 分区维度详情列表 */
  public record TopologyVo(List<BrokerVo> brokers, List<PartitionVo> partitions) {}

  /** broker 维度分区引用 */
  public record BrokerPartitionRef(String group, int partitionId, String role, String health) {}

  /** 分区详情 */
  public record PartitionVo(
      String group,
      int partitionId,
      String leader,
      List<PartitionMemberVo> members,
      int sourceId,
      boolean active,
      boolean frozen,
      String businessAddress) {}

  /** 分区 raft 成员明细（含成员侧 sourceId/agentSourceIds 视角） */
  public record PartitionMemberVo(
      String memberId,
      String role,
      String health,
      long term,
      int sourceId,
      List<Integer> agentSourceIds) {}

  /** 网关接入信息 */
  public record GatewayVo(
      String nodeId, String version, String grpcAddress, boolean allowClientDiscovery) {}

  /** 网关负载 */
  public record GatewayLoadVo(String nodeId, int activeConnections) {}

  /** jobType 分组连接视图 */
  public record JobTypeVo(String jobType, List<JobStreamVo> streams) {}

  /** 单条 job 流连接会话 */
  public record JobStreamVo(
      long streamId,
      String worker,
      int capacity,
      long timeoutMillis,
      List<String> tenantIds,
      List<String> fetchVariables) {}

  /** 调度查询聚合结果 */
  public record DispatchVo(BusinessDispatch business, AdminDispatch admin) {}

  /** 业务面调度查询：元数据 + 最新计划（含执行明细） */
  public record BusinessDispatch(ClusterMetaVo clusterMeta, DispatchPlanVo dispatchPlan) {
    static BusinessDispatch of(
        final com.anyilanxin.kunpeng.protocol.admin.impl.record.commandapi.business.query
                .BusinessDispatchQueryResponseRecord
            record) {
      return new BusinessDispatch(
          ClusterMetaVo.of(record.getClusterMeta()),
          record.getDispatchPlan() == null ? null : DispatchPlanVo.of(record.getDispatchPlan()));
    }
  }

  /** 管理面调度查询：元数据 + 计划 */
  public record AdminDispatch(AdminClusterMetaVo clusterMeta, AdminDispatchPlanVo dispatchPlan) {
    static AdminDispatch of(
        final com.anyilanxin.kunpeng.protocol.admin.impl.record.commandapi.admin.query
                .AdminDispatchQueryResponseRecord
            record) {
      return new AdminDispatch(
          AdminClusterMetaVo.of(record.getClusterMeta()),
          record.getDispatchPlan() == null
              ? null
              : AdminDispatchPlanVo.of(record.getDispatchPlan()));
    }
  }

  /** 业务面集群元数据 */
  public record ClusterMetaVo(
      int version,
      int replicationFactor,
      int currentReplicationFactor,
      int currentPartitionCount,
      long createTime,
      long updateTime,
      List<PartitionInfoMetaVo> meta,
      List<PartitionInfoMetaVo> lastMeta) {
    static ClusterMetaVo of(final BusinessClusterMetaRecord record) {
      if (record == null) {
        return null;
      }
      return new ClusterMetaVo(
          record.getVersion(),
          record.getReplicationFactor(),
          record.getCurrentReplicationFactor(),
          record.getCurrentPartitionCount(),
          record.getCreateTime(),
          record.getUpdateTime(),
          record.meta().stream().map(PartitionInfoMetaVo::of).toList(),
          record.getLastMeta().stream().map(PartitionInfoMetaVo::of).toList());
    }
  }

  /** 管理面集群元数据（管理面单分区） */
  public record AdminClusterMetaVo(
      int version,
      int replicationFactor,
      int currentReplicationFactor,
      long createTime,
      long updateTime,
      PartitionInfoMetaVo meta,
      PartitionInfoMetaVo lastMeta) {
    static AdminClusterMetaVo of(final AdminClusterMetaRecord record) {
      if (record == null) {
        return null;
      }
      return new AdminClusterMetaVo(
          record.getVersion(),
          record.getReplicationFactor(),
          record.getCurrentReplicationFactor(),
          record.getCreateTime(),
          record.getUpdateTime(),
          PartitionInfoMetaVo.of(record.getMeta()),
          PartitionInfoMetaVo.of(record.getLastMeta()));
    }
  }

  /** 调度计划（业务面） */
  public record DispatchPlanVo(
      long dispatchPlanId,
      String dispatchPlanType,
      String dispatchState,
      boolean applyPlan,
      boolean initialize,
      int oldPartitionsCount,
      int expectPartitionsCount,
      int oldReplicationFactor,
      int expectReplicationFactor,
      List<DispatchExecutionVo> executionPlan,
      List<PartitionInfoMetaVo> meta,
      List<PartitionInfoMetaVo> oldMeta,
      List<Integer> transferSourceId) {
    static DispatchPlanVo of(final BusinessDispatchPlanRecord record) {
      return new DispatchPlanVo(
          record.getDispatchPlanId(),
          enumName(record.getDispatchPlanType()),
          enumName(record.getDispatchState()),
          record.isApplyPlan(),
          record.isInitialize(),
          record.getOldPartitionsCount(),
          record.getExpectPartitionsCount(),
          record.getOldReplicationFactor(),
          record.getExpectReplicationFactor(),
          record.executionPlan().stream().map(DispatchExecutionVo::of).toList(),
          record.meta().stream().map(PartitionInfoMetaVo::of).toList(),
          record.getOldMeta().stream().map(PartitionInfoMetaVo::of).toList(),
          record.getTransferSourceId());
    }
  }

  /** 调度计划（管理面） */
  public record AdminDispatchPlanVo(
      long dispatchPlanId,
      String dispatchPlanType,
      String dispatchState,
      boolean applyPlan,
      boolean initialize,
      int oldReplicationFactor,
      int expectReplicationFactor,
      List<DispatchExecutionVo> executionPlan,
      PartitionInfoMetaVo meta,
      PartitionInfoMetaVo oldMeta) {
    static AdminDispatchPlanVo of(final AdminDispatchPlanRecord record) {
      return new AdminDispatchPlanVo(
          record.getDispatchPlanId(),
          enumName(record.getDispatchPlanType()),
          enumName(record.getDispatchState()),
          record.isApplyPlan(),
          record.isInitialize(),
          record.getOldReplicationFactor(),
          record.getExpectReplicationFactor(),
          record.executionPlan().stream().map(DispatchExecutionVo::ofAdmin).toList(),
          PartitionInfoMetaVo.of(record.getMeta()),
          PartitionInfoMetaVo.of(record.getOldMeta()));
    }
  }

  /** 执行明细 */
  public record DispatchExecutionVo(
      long dispatchPlanExecutionId,
      int executionOrder,
      String state,
      String memberId,
      String message,
      long startTime,
      long endTime,
      long dueDate,
      long remaining,
      String planType) {
    static DispatchExecutionVo of(final BusinessDispatchPlanExecutionRecord record) {
      return new DispatchExecutionVo(
          record.getDispatchPlanExecutionId(),
          record.getExecutionOrder(),
          enumName(record.getDispatchExecutionState()),
          record.getDispatchMemberId(),
          record.getDispatchMessage(),
          record.getDispatchStartTime(),
          record.getDispatchEndTime(),
          record.getDueDate(),
          record.getDispatchRemaining(),
          enumName(record.getDispatchPlanType()));
    }

    static DispatchExecutionVo ofAdmin(
        final com.anyilanxin.kunpeng.protocol.admin.impl.record.command.admin
                .AdminDispatchPlanExecutionRecord
            record) {
      return new DispatchExecutionVo(
          record.getDispatchPlanExecutionId(),
          record.getExecutionOrder(),
          enumName(record.getDispatchExecutionState()),
          record.getDispatchMemberId(),
          record.getDispatchMessage(),
          record.getDispatchStartTime(),
          record.getDispatchEndTime(),
          record.getDueDate(),
          -1L,
          null);
    }
  }

  /** 目标拓扑分区元数据 */
  public record PartitionInfoMetaVo(
      String partitionGroup,
      int partitionId,
      int targetPriority,
      String primaryMemberId,
      List<PartitionMemberMetaVo> partitionMembers) {
    static PartitionInfoMetaVo of(final PartitionInfoMetaRecordValue record) {
      if (record == null) {
        return null;
      }
      return new PartitionInfoMetaVo(
          record.getPartitionGroup(),
          record.getPartitionId(),
          record.getTargetPriority(),
          record.getPrimaryMemberId(),
          record.getPartitionMembers().stream()
              .map(m -> new PartitionMemberMetaVo(m.getMemberId(), m.getPriority()))
              .toList());
    }
  }

  /** 目标拓扑分区成员 */
  public record PartitionMemberMetaVo(String memberId, int priority) {}

  private static String enumName(final Enum<?> value) {
    return value == null ? null : value.name();
  }
}
