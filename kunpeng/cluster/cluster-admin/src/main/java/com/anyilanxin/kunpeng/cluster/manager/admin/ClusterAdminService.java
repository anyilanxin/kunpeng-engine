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
package com.anyilanxin.kunpeng.cluster.manager.admin;

import static com.anyilanxin.kunpeng.cluster.config.BusinessSourceMetaUtils.addSourceInfo;
import static com.anyilanxin.kunpeng.protocol.admin.impl.record.command.common.ExecutionRecordSerialize.decode;
import static com.anyilanxin.kunpeng.protocol.admin.record.command.PartitionExecutionType.getTopic;
import static com.anyilanxin.kunpeng.protocol.common.ClusterCommonConstant.ADMIN_PARTITION_SOURCE;
import static com.anyilanxin.kunpeng.protocol.common.ClusterCommonConstant.ADMIN_RAFT_GROUP;

import com.anyilanxin.kunpeng.broker.client.admin.commandapi.CommandApiService;
import com.anyilanxin.kunpeng.cluster.business.RaftPartitionFactory;
import com.anyilanxin.kunpeng.cluster.cluster.AtomixCluster;
import com.anyilanxin.kunpeng.cluster.cluster.ClusterMembershipService;
import com.anyilanxin.kunpeng.cluster.cluster.MemberId;
import com.anyilanxin.kunpeng.cluster.cluster.PartitionId;
import com.anyilanxin.kunpeng.cluster.cluster.leaderfound.ClusterLeaderFoundService;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.ClusterCommunicationService;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.MessagingService;
import com.anyilanxin.kunpeng.cluster.config.ClusterAdminConfiguration;
import com.anyilanxin.kunpeng.cluster.config.ClusterMetaStore;
import com.anyilanxin.kunpeng.cluster.config.DispatchMeta;
import com.anyilanxin.kunpeng.cluster.config.topology.broker.DefaultClusterSwimTopologyService;
import com.anyilanxin.kunpeng.cluster.config.topology.cluster.ClusterTopologyService;
import com.anyilanxin.kunpeng.cluster.dispatch.api.ClusterDispatchClient;
import com.anyilanxin.kunpeng.cluster.dispatch.scheduling.TimerClock;
import com.anyilanxin.kunpeng.cluster.manager.ClusterAdminLoggers;
import com.anyilanxin.kunpeng.cluster.manager.admin.raft.*;
import com.anyilanxin.kunpeng.cluster.manager.business.raft.BusinessRaftClientService;
import com.anyilanxin.kunpeng.cluster.manager.business.raft.DefaultBusinessRaftClientService;
import com.anyilanxin.kunpeng.cluster.raft.partition.PartitionManagementService;
import com.anyilanxin.kunpeng.cluster.raft.partition.PartitionMetadata;
import com.anyilanxin.kunpeng.cluster.raft.partition.RaftPartition;
import com.anyilanxin.kunpeng.cluster.raft.partition.impl.DefaultPartitionManagementService;
import com.anyilanxin.kunpeng.cluster.utils.net.Address;
import com.anyilanxin.kunpeng.configuration.ZoneType;
import com.anyilanxin.kunpeng.configuration.broker.BrokerCfg;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.common.PartitionConfigChangeRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.common.PartitionExecutionAckRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.common.PartitionJoinRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.common.PartitionLeaveRecord;
import com.anyilanxin.kunpeng.protocol.admin.record.command.PartitionExecutionRecordValue;
import com.anyilanxin.kunpeng.protocol.admin.record.command.PartitionExecutionType;
import com.anyilanxin.kunpeng.protocol.admin.record.command.PartitionType;
import com.anyilanxin.kunpeng.scheduler.Actor;
import com.anyilanxin.kunpeng.scheduler.ActorSchedulingService;
import com.anyilanxin.kunpeng.scheduler.future.ActorFuture;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;

/**
 * 集群管理服务的默认实现。
 *
 * @author zxuanhong
 * @since
 */
public class ClusterAdminService extends Actor implements ClusterAdmin, AdminExecutionService {
  private static final Logger LOG = ClusterAdminLoggers.CLUSTER_ADMIN;

  /** ack 重试次数上限 */
  private static final int ACK_RETRY_LIMIT = 10;

  private final ClusterMetaStore clusterMetaStore;
  private final MemberId initMemberId;
  private final MemberId localMemberId;
  private final RaftPartitionFactory raftPartitionFactory;
  private final ActorSchedulingService actorSchedulingService;
  private final PartitionManagementService partitionManagementService;
  private final ClusterCommunicationService communicationService;
  private final MessagingService messagingService;
  private final ClusterMembershipService membershipService;
  private final MeterRegistry meterRegistry;
  private final DefaultAdminRaftClientService adminRaftClientService;
  private final DefaultBusinessRaftClientService businessRaftClientService;
  private final BrokerCfg brokerCfg;

  /** 当前 admin 分区服务；startRaft 在异步链线程赋值，handleJoin 在 actor 线程读取，需 volatile 保证可见性 */
  private volatile AdminPartitionService partitionService;

  /** 初始 admin raft 启动流程（start() 触发的 bootstrap/join 链）；join 调度须等待其结束，避免并发构建分区服务 */
  private volatile ActorFuture<Void> adminRaftStarting;

  private final PartitionJoinRecord joinRecord = new PartitionJoinRecord();
  private final PartitionLeaveRecord leaveRecord = new PartitionLeaveRecord();
  private final PartitionConfigChangeRecord configChangeRecord = new PartitionConfigChangeRecord();
  private final ClusterLeaderFoundService leaderFoundService;
  private final CommandApiService commandApiService;
  private final ClusterDispatchClient dispatchClient;
  private final DefaultClusterSwimTopologyService brokerTopologyService;
  private final ClusterTopologyService clusterTopologyService;
  private final TimerClock timerClock;

  /** 已执行的 ack 重试次数 */
  private int ackRetryAttempts;

  public ClusterAdminService(
      final TimerClock timerClock,
      final BrokerCfg brokerCfg,
      final AtomixCluster atomixCluster,
      final MessagingService messagingService,
      final ActorSchedulingService actorSchedulingService,
      final MeterRegistry meterRegistry,
      final CommandApiService commandApiService,
      final ClusterMetaStore clusterMetaStore,
      final ClusterDispatchClient dispatchClient,
      final DefaultClusterSwimTopologyService brokerTopologyService,
      final ClusterTopologyService clusterTopologyService) {
    this.timerClock = timerClock;
    this.brokerTopologyService = brokerTopologyService;
    this.clusterTopologyService = clusterTopologyService;
    this.dispatchClient = dispatchClient;
    this.clusterMetaStore = clusterMetaStore;
    this.brokerCfg = brokerCfg;
    this.commandApiService = commandApiService;
    leaderFoundService = atomixCluster.getLeaderFoundService();
    membershipService = atomixCluster.getMembershipService();
    localMemberId = membershipService.getLocalMember().id();
    this.meterRegistry = meterRegistry;
    communicationService = atomixCluster.getCommunicationService();
    this.messagingService = messagingService;
    this.actorSchedulingService = actorSchedulingService;
    initMemberId =
        MemberId.from(ZoneType.BROKER.getType() + "@" + brokerCfg.getManage().getInitBrokerId());
    partitionManagementService = buildPartitionManagementService();
    raftPartitionFactory =
        new RaftPartitionFactory(brokerCfg, partitionManagementService, actorSchedulingService);
    adminRaftClientService = buildAdminRaftClientService();
    businessRaftClientService = buildBusinessRaftClientService();
  }

  @Override
  public ActorFuture<Void> start() {
    final ActorFuture<Void> future = actor.createFuture();
    // 必须在 submit 前赋值：本 actor 任务内触发的 handleJoin（重启重放）依赖它判断初始启动是否在进行
    adminRaftStarting = future;
    actor.submit(
        () -> {
          register();
          restartExecution();
          executionAck();
          actorSchedulingService
              .submitActor(adminRaftClientService)
              .thenApply(v -> actorSchedulingService.submitActor(businessRaftClientService))
              .thenApply(v -> startAdminRaft())
              .onComplete(
                  (_, throwable) -> {
                    if (throwable != null) {
                      future.completeExceptionally(throwable);
                    } else {
                      future.complete(null);
                    }
                  });
        });
    return future;
  }

  @Override
  public ActorFuture<Void> stop() {
    final ActorFuture<Void> future = actor.createFuture();
    actor.submit(
        () -> {
          unregister();
          adminRaftClientService
              .closeAsync()
              .thenApply(v -> businessRaftClientService.closeAsync())
              .thenApply(v -> closeAdminRaft())
              .onComplete(
                  (_, throwable) -> {
                    if (throwable != null) {
                      future.completeExceptionally(throwable);
                    } else {
                      future.complete(null);
                    }
                  });
        });
    return future;
  }

  private PartitionManagementService buildPartitionManagementService() {
    return new DefaultPartitionManagementService(
        membershipService, communicationService, messagingService, leaderFoundService);
  }

  private ActorFuture<Void> closeAdminRaft() {
    final ActorFuture<Void> future = actor.createFuture();
    if (partitionService != null) {
      partitionService
          .stop()
          .onComplete(
              (_, throwable) -> {
                if (throwable == null) {
                  future.complete(null);
                } else {
                  future.completeExceptionally(throwable);
                }
              });
    } else {
      future.complete(null);
    }
    return future;
  }

  private ActorFuture<Void> startAdminRaft() {
    final ActorFuture<Void> future = actor.createFuture();
    if (clusterMetaStore.getAdminConfiguration().isUninitialized()) {
      if (initMemberId.equals(localMemberId)) {
        LOG.info(
            "Admin raft is uninitialized and this node is the init member, bootstrapping a new admin raft, init member: {}",
            initMemberId);
        startInitAdminRaft(future);
      } else {
        LOG.info(
            "Admin raft is uninitialized, this node {} is not the init member {}, waiting for the admin raft to be initialized",
            localMemberId,
            initMemberId);
        future.complete(null);
      }
    } else {
      LOG.info("Admin raft is already initialized, joining the existing admin raft");
      startAdminRaft(future);
    }
    return future;
  }

  private void startInitAdminRaft(final ActorFuture<Void> future) {
    final PartitionId partitionId = PartitionId.from(ADMIN_RAFT_GROUP, 1);
    LOG.info("Init Starting admin raft partition {}", partitionId);
    final Set<MemberId> members = new HashSet<>();
    members.add(initMemberId);
    final Map<MemberId, Integer> priority = new HashMap<>();
    priority.put(initMemberId, 0);
    final PartitionMetadata partitionMetadata =
        new PartitionMetadata(partitionId, members, priority, 0, initMemberId);
    final ClusterAdminConfiguration adminConfiguration = clusterMetaStore.getAdminConfiguration();
    adminConfiguration.setAdminPartition(partitionMetadata);
    adminConfiguration.setVersion(1);
    adminConfiguration.setInitiator(false);
    clusterMetaStore.updateAdminConfiguration(adminConfiguration);
    startRaft(partitionMetadata, future);
  }

  private void startRaft(
      final PartitionMetadata partitionMetadata, final ActorFuture<Void> future) {
    final AdminPartitionStartupContext startupContext = createContent(partitionMetadata);
    final AdminPartitionService service = AdminPartitionService.bootstrapping(startupContext);
    partitionService = service;
    service
        .start()
        .onComplete(
            (_, throwable) -> {
              if (throwable == null) {
                final RaftPartition raftPartition = service.raftPartition();
                addSourceInfo(ADMIN_PARTITION_SOURCE, raftPartition);
                future.complete(null);
              } else {
                if (partitionService == service) {
                  partitionService = null;
                }
                future.completeExceptionally(throwable);
              }
            });
  }

  private void startAdminRaft(final ActorFuture<Void> future) {
    final ClusterAdminConfiguration adminConfiguration = clusterMetaStore.getAdminConfiguration();
    final PartitionMetadata partitionMetadata = adminConfiguration.getAdminPartition();
    LOG.info("Starting admin raft partition {}", partitionMetadata.id());
    startRaft(partitionMetadata, future);
  }

  private AdminPartitionStartupContext createContent(final PartitionMetadata partitionMetadata) {
    final AdminRaftSnapshotProvider snapshotProvider =
        new AdminRaftSnapshotProvider(
            this, brokerCfg.getRocksdb().createRocksDbConfiguration(), meterRegistry);
    return new AdminPartitionStartupContext(
        clusterMetaStore,
        actorSchedulingService,
        this,
        raftPartitionFactory,
        partitionMetadata,
        snapshotProvider,
        meterRegistry,
        partitionManagementService,
        brokerCfg,
        commandApiService,
        dispatchClient,
        brokerTopologyService,
        clusterTopologyService,
        timerClock);
  }

  private DefaultAdminRaftClientService buildAdminRaftClientService() {
    return new DefaultAdminRaftClientService(messagingService);
  }

  private DefaultBusinessRaftClientService buildBusinessRaftClientService() {
    return new DefaultBusinessRaftClientService(messagingService);
  }

  @Override
  public PartitionManagementService getPartitionManagementService() {
    return partitionManagementService;
  }

  @Override
  public AdminRaftClientService getAdminRaftClientService() {
    return adminRaftClientService;
  }

  @Override
  public BusinessRaftClientService getBusinessRaftClientService() {
    return businessRaftClientService;
  }

  @Override
  public AdminPartitionService getRaftPartition() {
    if (partitionService == null) {
      throw new IllegalStateException("Partition service is not starter");
    }
    return partitionService;
  }

  private void register() {
    LOG.info("Register Admin Raft Execution Service");
    messagingService.registerHandler(
        getTopic(PartitionType.ADMIN, PartitionExecutionType.JOIN), this::handleJoin, this);
    messagingService.registerHandler(
        getTopic(PartitionType.ADMIN, PartitionExecutionType.LEAVE), this::handleLeave, this);
    messagingService.registerHandler(
        getTopic(PartitionType.ADMIN, PartitionExecutionType.CONFIG_CHANGE),
        this::handleConfigChange,
        this);
  }

  private void unregister() {
    LOG.info("Unregister Admin Raft Execution Service");
    messagingService.unregisterHandler(getTopic(PartitionType.ADMIN, PartitionExecutionType.JOIN));
    messagingService.unregisterHandler(getTopic(PartitionType.ADMIN, PartitionExecutionType.LEAVE));
    messagingService.unregisterHandler(
        getTopic(PartitionType.ADMIN, PartitionExecutionType.CONFIG_CHANGE));
  }

  /** 分区加入调度 */
  private void handleJoin(final Address address, final byte[] bytes) {
    final PartitionJoinRecord record = decode(bytes, joinRecord);
    LOG.info("收到调度，调度类型: {}，调度信息: \n{}", PartitionExecutionType.JOIN, record);
    addDispatch(
        record.getDispatchPlanId(),
        record.getDispatchPlanExecutionId(),
        PartitionExecutionType.JOIN,
        bytes);
    actor.submit(() -> tryJoin(record));
  }

  /**
   * 以加入者身份启动 admin 分区。partitionService 已存在（含启动中）或初始启动流程尚未结束时不得再建服务， 否则两个 server 会并发初始化同一分区目录，在 meta
   * 文件等资源上竞争。
   */
  private void tryJoin(final PartitionJoinRecord record) {
    final PartitionMetadata metadata = record.getPartitionMeta().toMetadata();
    if (partitionService != null) {
      LOG.info("Admin raft partition is already started, complete join dispatch directly");
      dispatchComplete();
      return;
    }
    final ActorFuture<Void> starting = adminRaftStarting;
    if (starting != null && !starting.isDone()) {
      // 初始启动还在进行：等它结束后再决定是否需要以加入者身份启动
      starting.onComplete((_, error) -> actor.submit(() -> tryJoin(record)));
      return;
    }
    final AdminPartitionService joining = AdminPartitionService.joining(createContent(metadata));
    partitionService = joining;
    joining
        .start()
        .onComplete(
            (_, throwable) -> {
              if (throwable != null) {
                LOG.error("Join Admin Raft Execution Service failed", throwable);
                if (partitionService == joining) {
                  partitionService = null;
                }
                dispatchFailed(throwable);
              } else {
                dispatchComplete(metadata);
              }
            });
  }

  /** 调度完成且无需更新分区元数据（分区状态已满足调度，避免挂起记录每次重启重放） */
  private void dispatchComplete() {
    final ClusterAdminConfiguration raftConfiguration = clusterMetaStore.getAdminConfiguration();
    raftConfiguration.notDispatch();
    raftConfiguration.setVersion(raftConfiguration.getVersion() + 1);
    clusterMetaStore.updateAdminConfiguration(raftConfiguration);
    executionAck();
  }

  /** 分区离开调度 */
  private void handleLeave(final Address address, final byte[] bytes) {
    final PartitionLeaveRecord record = decode(bytes, leaveRecord);
    LOG.info("收到调度，调度类型: {}，调度信息: \n{}", PartitionExecutionType.LEAVE, record);
    addDispatch(
        record.getDispatchPlanId(),
        record.getDispatchPlanExecutionId(),
        PartitionExecutionType.LEAVE,
        bytes);
    actor.submit(
        () -> {
          if (partitionService != null) {
            partitionService
                .leave()
                .onComplete(
                    (_, throwable) -> {
                      if (throwable != null) {
                        LOG.error("Leave Admin Raft Execution Service", throwable);
                        dispatchFailed(throwable);
                      } else {
                        dispatchComplete(null);
                      }
                    });
          } else {
            // 管理分区不在本节点时离开视为已满足，直接完成调度并 ack
            LOG.info("Admin raft partition is not started, complete leave dispatch directly");
            dispatchComplete(null);
          }
        });
  }

  /** 配置更改调度 */
  private void handleConfigChange(final Address address, final byte[] bytes) {
    final PartitionConfigChangeRecord record = decode(bytes, configChangeRecord);
    LOG.info("收到调度，调度类型: {}，调度信息: \n{}", PartitionExecutionType.CONFIG_CHANGE, record);
    addDispatch(
        record.getDispatchPlanId(),
        record.getDispatchPlanExecutionId(),
        PartitionExecutionType.CONFIG_CHANGE,
        bytes);
    actor.submit(
        () -> {
          final Set<MemberId> memberIds = record.getMemberIds();
          if (partitionService != null) {
            partitionService
                .forceReconfigure(memberIds)
                .onComplete(
                    (unused, throwable) -> {
                      if (throwable == null) {
                        final PartitionMetadata metadata = record.getTargetMeta().toMetadata();
                        dispatchComplete(metadata);
                      } else {
                        LOG.error("Config Change Admin Raft Execution Service", throwable);
                        dispatchFailed(throwable);
                      }
                    });
          } else {
            // 管理分区不在本节点时无配置可改，完成调度并 ack，避免挂起记录每次重启重放
            LOG.info("Admin raft partition is not started, skip config change dispatch");
            dispatchComplete();
          }
        });
  }

  /** 添加调度信息存储 */
  private void addDispatch(
      final long dispatchPlanId,
      final long dispatchPlanExecutionId,
      final PartitionExecutionType executionType,
      final byte[] bytes) {
    final ClusterAdminConfiguration raftConfiguration = clusterMetaStore.getAdminConfiguration();
    raftConfiguration.haveDispatch(dispatchPlanId, dispatchPlanExecutionId, executionType, bytes);
    raftConfiguration.setVersion(raftConfiguration.getVersion() + 1);
    clusterMetaStore.updateAdminConfiguration(raftConfiguration);
  }

  /** 调度完成并更新某个分区元数据或删除分区元数据 */
  private void dispatchComplete(final PartitionMetadata metadata) {
    final ClusterAdminConfiguration raftConfiguration = clusterMetaStore.getAdminConfiguration();
    raftConfiguration.notDispatch();
    raftConfiguration.setAdminPartition(metadata);
    raftConfiguration.setVersion(raftConfiguration.getVersion() + 1);
    clusterMetaStore.updateAdminConfiguration(raftConfiguration);
    executionAck();
  }

  /** 调度执行失败：清除挂起标记（保留 meta 供 ack 组装）后上报失败 ack */
  private void dispatchFailed(final Throwable throwable) {
    final ClusterAdminConfiguration raftConfiguration = clusterMetaStore.getAdminConfiguration();
    if (raftConfiguration.haveDispatch()) {
      raftConfiguration.notDispatch();
      raftConfiguration.setVersion(raftConfiguration.getVersion() + 1);
      clusterMetaStore.updateAdminConfiguration(raftConfiguration);
    }
    executionAck(throwable);
  }

  /** 系统重启重新执行调度 */
  private void restartExecution() {
    final ClusterAdminConfiguration raftConfiguration = clusterMetaStore.getAdminConfiguration();
    if (raftConfiguration.haveDispatch()) {
      final DispatchMeta dispatchMeta = raftConfiguration.getDispatchMeta();
      if (dispatchMeta != null) {
        final byte[] bytes = dispatchMeta.bytes();
        final PartitionExecutionType partitionExecutionType = dispatchMeta.executionType();
        LOG.info("存在未完成调度，重启后重新执行，调度类型: {}，调度信息: \n{}", partitionExecutionType, dispatchMeta);
        switch (partitionExecutionType) {
          case JOIN -> handleJoin(null, bytes);
          case LEAVE -> handleLeave(null, bytes);
          case CONFIG_CHANGE -> handleConfigChange(null, bytes);
        }
      }
    }
  }

  /** 调度信息 ack */
  private void executionAck() {
    executionAck(null);
  }

  /** 调度信息 ack，failure 非空时上报失败结果 */
  private void executionAck(final Throwable failure) {
    try {
      final ClusterAdminConfiguration raftConfiguration = clusterMetaStore.getAdminConfiguration();
      if (raftConfiguration.haveAck()) {
        final DispatchMeta dispatchMeta = raftConfiguration.getDispatchMeta();
        final PartitionExecutionType partitionExecutionType = dispatchMeta.executionType();
        final byte[] bytes = dispatchMeta.bytes();
        final PartitionExecutionRecordValue decode = decode(bytes, partitionExecutionType);
        final PartitionExecutionAckRecord data = new PartitionExecutionAckRecord();
        data.setDispatchPlanExecutionId(decode.getDispatchPlanExecutionId())
            .setDispatchPlanId(decode.getDispatchPlanId())
            .setExecutionType(partitionExecutionType)
            .setPartitionType(decode.getPartitionType())
            .setExecutionMemberId(decode.executionMemberId())
            .setSuccess(failure == null);
        if (failure != null) {
          data.setErrorMessage(failure.toString());
        }
        dispatchClient.ack(data);
        LOG.info(
            "调度执行 ack，成功: {}，调度类型: {}，ack 信息: \n{}", failure == null, partitionExecutionType, data);
        raftConfiguration.ackOk();
        raftConfiguration.setVersion(raftConfiguration.getVersion() + 1);
        clusterMetaStore.updateAdminConfiguration(raftConfiguration);
        ackRetryAttempts = 0;
      }
    } catch (final Exception e) {
      if (ackRetryAttempts >= ACK_RETRY_LIMIT) {
        LOG.error("Ack dispatch execution failed after {} retries, giving up", ACK_RETRY_LIMIT, e);
        return;
      }
      ackRetryAttempts++;
      final long delaySeconds = ackRetryAttempts * 4L;
      LOG.warn(
          "Ack dispatch execution failed, retrying in {}s (attempt {}/{})",
          delaySeconds,
          ackRetryAttempts,
          ACK_RETRY_LIMIT,
          e);
      actor.schedule(Duration.ofSeconds(delaySeconds), this::executionAck);
    }
  }
}
