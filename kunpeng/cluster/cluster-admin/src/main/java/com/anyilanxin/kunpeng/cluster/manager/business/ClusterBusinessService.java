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
package com.anyilanxin.kunpeng.cluster.manager.business;

import static com.anyilanxin.kunpeng.cluster.config.BusinessSourceMetaUtils.*;
import static com.anyilanxin.kunpeng.protocol.admin.impl.record.command.common.ExecutionRecordSerialize.decode;
import static com.anyilanxin.kunpeng.protocol.admin.record.command.PartitionExecutionType.getTopic;

import com.anyilanxin.kunpeng.cluster.business.PartitionService;
import com.anyilanxin.kunpeng.cluster.business.RaftPartitionFactory;
import com.anyilanxin.kunpeng.cluster.cluster.AtomixCluster;
import com.anyilanxin.kunpeng.cluster.cluster.ClusterMembershipService;
import com.anyilanxin.kunpeng.cluster.cluster.MemberId;
import com.anyilanxin.kunpeng.cluster.cluster.PartitionId;
import com.anyilanxin.kunpeng.cluster.cluster.leaderfound.ClusterLeaderFoundService;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.ClusterCommunicationService;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.MessagingService;
import com.anyilanxin.kunpeng.cluster.config.ClusterMetaStore;
import com.anyilanxin.kunpeng.cluster.config.ClusterRaftConfiguration;
import com.anyilanxin.kunpeng.cluster.config.DispatchMeta;
import com.anyilanxin.kunpeng.cluster.config.topology.broker.DefaultClusterSwimTopologyService;
import com.anyilanxin.kunpeng.cluster.dispatch.api.ClusterDispatchClient;
import com.anyilanxin.kunpeng.cluster.dispatch.scheduling.TimerClock;
import com.anyilanxin.kunpeng.cluster.manager.ClusterAdminLoggers;
import com.anyilanxin.kunpeng.cluster.manager.business.raft.BusinessPartitionService;
import com.anyilanxin.kunpeng.cluster.manager.business.raft.BusinessPartitionStartupContext;
import com.anyilanxin.kunpeng.cluster.manager.business.raft.BusinessRaftSnapshotProvider;
import com.anyilanxin.kunpeng.cluster.raft.partition.PartitionManagementService;
import com.anyilanxin.kunpeng.cluster.raft.partition.PartitionMetadata;
import com.anyilanxin.kunpeng.cluster.raft.partition.RaftPartition;
import com.anyilanxin.kunpeng.cluster.raft.partition.impl.DefaultPartitionManagementService;
import com.anyilanxin.kunpeng.cluster.utils.net.Address;
import com.anyilanxin.kunpeng.configuration.broker.BrokerCfg;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.common.*;
import com.anyilanxin.kunpeng.protocol.admin.record.command.PartitionExecutionRecordValue;
import com.anyilanxin.kunpeng.protocol.admin.record.command.PartitionExecutionType;
import com.anyilanxin.kunpeng.protocol.admin.record.command.PartitionType;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.scheduler.Actor;
import com.anyilanxin.kunpeng.scheduler.ActorSchedulingService;
import com.anyilanxin.kunpeng.scheduler.future.ActorFuture;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.agrona.collections.Int2ObjectHashMap;
import org.slf4j.Logger;

/**
 * 集群管理服务的默认实现。
 *
 * @author zxuanhong
 * @since
 */
public class ClusterBusinessService extends Actor
    implements ClusterBusiness, BusinessExecutionService {
  private static final Logger LOG = ClusterAdminLoggers.CLUSTER_BUSINESS;

  /** ack 重试次数上限 */
  private static final int ACK_RETRY_LIMIT = 10;

  private final ClusterMetaStore clusterMetaStore;
  private final RaftPartitionFactory raftPartitionFactory;
  private final ActorSchedulingService actorSchedulingService;
  private final PartitionManagementService partitionManagementService;
  private final ClusterCommunicationService communicationService;
  private final MessagingService messagingService;
  private final ClusterMembershipService membershipService;
  private final MeterRegistry meterRegistry;
  private final Int2ObjectHashMap<BusinessPartitionService> raftPartitionMap;
  private final ClusterLeaderFoundService leaderFoundService;
  private final ClusterDispatchClient dispatchClient;
  private final DefaultClusterSwimTopologyService clusterPartitionTopology;
  private final TimerClock timerClock;
  private final BrokerCfg brokerCfg;

  /** 已执行的 ack 重试次数 */
  private int ackRetryAttempts;

  public ClusterBusinessService(
      final TimerClock timerClock,
      final BrokerCfg brokerCfg,
      final ClusterMetaStore clusterMetaStore,
      final AtomixCluster atomixCluster,
      final MessagingService messagingService,
      final ActorSchedulingService actorSchedulingService,
      final MeterRegistry meterRegistry,
      final ClusterDispatchClient dispatchClient,
      final DefaultClusterSwimTopologyService clusterPartitionTopology) {
    this.timerClock = timerClock;
    this.brokerCfg = brokerCfg;
    this.meterRegistry = meterRegistry;
    this.dispatchClient = dispatchClient;
    this.clusterPartitionTopology = clusterPartitionTopology;
    this.clusterMetaStore = clusterMetaStore;
    membershipService = atomixCluster.getMembershipService();
    communicationService = atomixCluster.getCommunicationService();
    leaderFoundService = atomixCluster.getLeaderFoundService();
    this.messagingService = messagingService;
    this.actorSchedulingService = actorSchedulingService;
    partitionManagementService = buildPartitionManagementService();
    raftPartitionFactory =
        new RaftPartitionFactory(brokerCfg, partitionManagementService, actorSchedulingService);
    raftPartitionMap = new Int2ObjectHashMap<>();
  }

  @Override
  public ActorFuture<Void> start() {
    final ActorFuture<Void> future = actor.createFuture();
    actor.submit(
        () -> {
          register();
          // 等待全部已有分区启动完成后再重放调度，调度重放时能看到分区的最终占位状态
          startBusinessRaft()
              .onComplete(
                  (_, throwable) -> {
                    if (throwable != null) {
                      future.completeExceptionally(throwable);
                    } else {
                      executionAck(null);
                      restartExecution();
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
          closeBusinessRaft()
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

  private ActorFuture<Void> closeBusinessRaft() {
    final ActorFuture<Void> result = actor.createFuture();
    final List<ActorFuture<PartitionService<BusinessPartitionStartupContext>>> futures =
        new ArrayList<>();
    for (final BusinessPartitionService raftPartition : getRaftPartitions()) {
      futures.add(raftPartition.stop());
    }
    actor.runOnCompletion(
        futures,
        throwable -> {
          raftPartitionMap.clear();
          if (throwable != null) {
            result.completeExceptionally(throwable);
          } else {
            result.complete(null);
          }
        });
    return result;
  }

  private ActorFuture<Void> startBusinessRaft() {
    final ActorFuture<Void> result = actor.createFuture();
    final ClusterRaftConfiguration raftConfiguration = clusterMetaStore.getRaftConfiguration();
    if (raftConfiguration.isUninitialized()) {
      LOG.info(
          "Cluster Raft configuration is not initialized, Not Business Partition Service Starter");
      result.complete(null);
      return result;
    }
    final List<PartitionMetadata> partitions = raftConfiguration.getPartitions();
    final List<ActorFuture<PartitionService<BusinessPartitionStartupContext>>> futures =
        new ArrayList<>();
    for (final PartitionMetadata partition : partitions) {
      final BusinessPartitionStartupContext content = createContent(partition);
      final BusinessPartitionService bootstrapping =
          BusinessPartitionService.bootstrapping(content);
      if (raftPartitionMap.putIfAbsent(partition.id().id(), bootstrapping) == null) {
        futures.add(bootstrapping.start());
      }
    }
    actor.runOnCompletion(
        futures,
        throwable -> {
          if (throwable != null) {
            final List<Integer> failedPartitionIds = new ArrayList<>();
            raftPartitionMap.forEach(
                (id, v) -> {
                  if (v.isStarted()) {
                    v.stop();
                  } else {
                    failedPartitionIds.add(id);
                  }
                });
            // 启动失败的分区从占位 map 移除，避免残留占位阻塞后续调度重试
            failedPartitionIds.forEach(raftPartitionMap::remove);
            result.completeExceptionally(throwable);
          } else {
            result.complete(null);
          }
        });
    return result;
  }

  private BusinessPartitionStartupContext createContent(final PartitionMetadata metadata) {
    final BusinessRaftSnapshotProvider snapshotProvider = new BusinessRaftSnapshotProvider();
    return new BusinessPartitionStartupContext(
        actorSchedulingService,
        this,
        raftPartitionFactory,
        metadata,
        snapshotProvider,
        meterRegistry,
        partitionManagementService,
        clusterPartitionTopology,
        timerClock,
        brokerCfg);
  }

  @Override
  public PartitionManagementService getPartitionManagementService() {
    return partitionManagementService;
  }

  @Override
  public List<BusinessPartitionService> getRaftPartitions() {
    return raftPartitionMap.values().stream().toList();
  }

  @Override
  public BusinessPartitionService getRaftPartition(final int partitionId) {
    return raftPartitionMap.get(partitionId);
  }

  private void register() {
    LOG.info("Register Business Raft Execution Service");
    messagingService.registerHandler(
        getTopic(PartitionType.BUSINESS, PartitionExecutionType.BOOTSTRAP),
        this::handleBootstrap,
        actor);
    messagingService.registerHandler(
        getTopic(PartitionType.BUSINESS, PartitionExecutionType.JOIN), this::handleJoin, actor);
    messagingService.registerHandler(
        getTopic(PartitionType.BUSINESS, PartitionExecutionType.LEAVE), this::handleLeave, actor);
    messagingService.registerHandler(
        getTopic(PartitionType.BUSINESS, PartitionExecutionType.STOP), this::handleStop, actor);
    messagingService.registerHandler(
        getTopic(PartitionType.BUSINESS, PartitionExecutionType.CONFIG_CHANGE),
        this::handleConfigChange,
        actor);
    messagingService.registerHandler(
        getTopic(PartitionType.BUSINESS, PartitionExecutionType.LEAVE_SOURCE_TRANSFER),
        this::handleLeaveSourceTransfer,
        actor);
    messagingService.registerHandler(
        getTopic(PartitionType.BUSINESS, PartitionExecutionType.LEAVE_SOURCE_DATA_TRANSFER),
        this::handleLeaveDataSourceTransfer,
        actor);
    messagingService.registerHandler(
        getTopic(PartitionType.BUSINESS, PartitionExecutionType.BOOTSTRAP_SOURCE_TRANSFER),
        this::handleBootstrapSourceTransfer,
        actor);
    messagingService.registerHandler(
        getTopic(PartitionType.BUSINESS, PartitionExecutionType.BOOTSTRAP_SOURCE_DATA_TRANSFER),
        this::handleBootstrapDataSourceTransfer,
        actor);
  }

  private void unregister() {
    LOG.info("Unregister Business Raft Execution Service");
    messagingService.unregisterHandler(
        getTopic(PartitionType.BUSINESS, PartitionExecutionType.BOOTSTRAP));
    messagingService.unregisterHandler(
        getTopic(PartitionType.BUSINESS, PartitionExecutionType.JOIN));
    messagingService.unregisterHandler(
        getTopic(PartitionType.BUSINESS, PartitionExecutionType.LEAVE));
    messagingService.unregisterHandler(
        getTopic(PartitionType.BUSINESS, PartitionExecutionType.STOP));
    messagingService.unregisterHandler(
        getTopic(PartitionType.BUSINESS, PartitionExecutionType.CONFIG_CHANGE));
    messagingService.unregisterHandler(
        getTopic(PartitionType.BUSINESS, PartitionExecutionType.LEAVE_SOURCE_DATA_TRANSFER));
    messagingService.unregisterHandler(
        getTopic(PartitionType.BUSINESS, PartitionExecutionType.LEAVE_SOURCE_TRANSFER));
    messagingService.unregisterHandler(
        getTopic(PartitionType.BUSINESS, PartitionExecutionType.BOOTSTRAP_SOURCE_DATA_TRANSFER));
    messagingService.unregisterHandler(
        getTopic(PartitionType.BUSINESS, PartitionExecutionType.BOOTSTRAP_SOURCE_TRANSFER));
  }

  /** 分区引导调度 */
  private void handleBootstrap(final Address address, final byte[] bytes) {
    final PartitionBootstrapRecord record = decodeBytes(bytes, PartitionExecutionType.BOOTSTRAP);
    actor.submit(
        () -> {
          final PartitionMetadata metadata = record.getPartitionMeta().toMetadata();
          final PartitionInfoMetaRecord targetMeta = record.getTargetMeta();
          final PartitionMetadata targetMetadata = targetMeta.toMetadata();
          final BusinessPartitionStartupContext content = createContent(metadata);
          final BusinessPartitionService bootstrapping =
              BusinessPartitionService.bootstrapping(content);
          if (raftPartitionMap.putIfAbsent(bootstrapping.id(), bootstrapping) == null) {
            bootstrapping
                .start()
                .onComplete(
                    (_, throwable) -> {
                      if (throwable != null) {
                        raftPartitionMap.remove(bootstrapping.id());
                        dispatchFailed(throwable);
                      } else {
                        final RaftPartition raftPartition = bootstrapping.raftPartition();
                        addSourceInfo(record.getSourceId(), raftPartition);
                        dispatchComplete(targetMetadata);
                      }
                    });
          } else {
            // 分区已存在，调度视为已满足：完成调度并 ack，避免挂起记录每次重启重放
            LOG.info(
                "Business partition {} already exists, skip bootstrap dispatch",
                bootstrapping.id());
            dispatchComplete();
          }
        });
  }

  /** 分区加入调度 */
  private void handleJoin(final Address address, final byte[] bytes) {
    final PartitionJoinRecord record = decodeBytes(bytes, PartitionExecutionType.JOIN);
    actor.submit(
        () -> {
          final PartitionMetadata metadata = record.getPartitionMeta().toMetadata();
          final BusinessPartitionStartupContext content = createContent(metadata);
          final BusinessPartitionService joining = BusinessPartitionService.joining(content);
          if (raftPartitionMap.putIfAbsent(joining.id(), joining) == null) {
            joining
                .start()
                .onComplete(
                    (_, throwable) -> {
                      if (throwable != null) {
                        raftPartitionMap.remove(joining.id());
                        dispatchFailed(throwable);
                      } else {
                        dispatchComplete(metadata);
                      }
                    });
          } else {
            // 分区已存在，调度视为已满足：完成调度并 ack，避免挂起记录每次重启重放
            LOG.info("Business partition {} already exists, skip join dispatch", joining.id());
            dispatchComplete();
          }
        });
  }

  /** 分区离开调度 */
  private void handleLeave(final Address address, final byte[] bytes) {
    final PartitionLeaveRecord record = decodeBytes(bytes, PartitionExecutionType.LEAVE);
    actor.submit(
        () -> {
          final PartitionId partitionId = record.toPartitionId();
          final BusinessPartitionService businessPartitionService =
              raftPartitionMap.get(partitionId.id());
          if (businessPartitionService != null) {
            businessPartitionService
                .leave()
                .onComplete(
                    (_, throwable) -> {
                      if (throwable != null) {
                        // leave 协议失败（如 leader 不可达）：回退本地停止并销毁，成功即视为调度完成
                        businessPartitionService
                            .stopAndDelete()
                            .onComplete(
                                (_, fallbackError) -> {
                                  if (fallbackError != null) {
                                    dispatchFailed(fallbackError);
                                  } else {
                                    raftPartitionMap.remove(partitionId.id());
                                    clusterPartitionTopology.removePartition(partitionId);
                                    dispatchComplete(partitionId);
                                  }
                                });
                      } else {
                        raftPartitionMap.remove(partitionId.id());
                        clusterPartitionTopology.removePartition(partitionId);
                        dispatchComplete(partitionId);
                      }
                    });
          } else {
            // 分区不在本节点时离开视为已满足，直接完成调度并 ack
            LOG.info(
                "Business partition {} is not started, complete leave dispatch directly",
                partitionId.id());
            dispatchComplete(partitionId);
          }
        });
  }

  /** 分区停止调度（分区最后一个成员本地停止并销毁分区，不走 leave 协议） */
  private void handleStop(final Address address, final byte[] bytes) {
    final PartitionStopRecord record = decodeBytes(bytes, PartitionExecutionType.STOP);
    actor.submit(
        () -> {
          final PartitionId partitionId = record.toPartitionId();
          final BusinessPartitionService businessPartitionService =
              raftPartitionMap.get(partitionId.id());
          if (businessPartitionService != null) {
            businessPartitionService
                .stopAndDelete()
                .onComplete(
                    (_, throwable) -> {
                      if (throwable != null) {
                        dispatchFailed(throwable);
                      } else {
                        raftPartitionMap.remove(partitionId.id());
                        clusterPartitionTopology.removePartition(partitionId);
                        dispatchComplete(partitionId);
                      }
                    });
          } else {
            // 分区不在本节点时停止视为已满足，直接完成调度并 ack
            LOG.info(
                "Business partition {} is not started, complete stop dispatch directly",
                partitionId.id());
            dispatchComplete(partitionId);
          }
        });
  }

  /** 配置更改调度 */
  private void handleConfigChange(final Address address, final byte[] bytes) {
    final PartitionConfigChangeRecord record =
        decodeBytes(bytes, PartitionExecutionType.CONFIG_CHANGE);
    actor.submit(
        () -> {
          final Set<MemberId> memberIds = record.getMemberIds();
          final BusinessPartitionService businessPartitionService =
              raftPartitionMap.get(record.getPartitionId());
          final PartitionInfoMetaRecord targetMeta = record.getTargetMeta();
          final PartitionMetadata targetMetadata = targetMeta.toMetadata();
          if (businessPartitionService != null) {
            businessPartitionService
                .forceReconfigure(memberIds)
                .onComplete(
                    (unused, throwable) -> {
                      if (throwable == null) {
                        dispatchComplete(targetMetadata);
                      } else {
                        LOG.error("Config Change Admin Raft Execution Service", throwable);
                        dispatchFailed(throwable);
                      }
                    });
          } else {
            // 分区不在本节点时无配置可改，完成调度并 ack，避免挂起记录每次重启重放
            LOG.info(
                "Business partition {} is not started, skip config change dispatch",
                record.getPartitionId());
            dispatchComplete();
          }
        });
  }

  private void handleBootstrapSourceTransfer(final Address address, final byte[] bytes) {
    final PartitionBootstrapSourceTransferRecord record =
        decodeBytes(bytes, PartitionExecutionType.BOOTSTRAP_SOURCE_TRANSFER);
    actor.submit(
        () -> {
          final BusinessPartitionService businessPartitionService =
              raftPartitionMap.get(record.getPartitionId());
          removeAgentSourceIds(
              Set.of(record.getSourceId()), businessPartitionService.raftPartition());
          final ClusterRaftConfiguration raftConfiguration =
              clusterMetaStore.getRaftConfiguration();
          raftConfiguration.setVersion(raftConfiguration.getVersion() + 1);
          clusterMetaStore.updateRaftConfiguration(raftConfiguration);
          dispatchComplete();
        });
  }

  private void handleBootstrapDataSourceTransfer(final Address address, final byte[] bytes) {
    final PartitionBootstrapSourceDataTransferRecord record =
        decodeBytes(bytes, PartitionExecutionType.BOOTSTRAP_SOURCE_DATA_TRANSFER);
    actor.submit(
        () -> {
          // TODO: 暂时空处理，具体逻辑需要后期实现
          dispatchComplete();
        });
  }

  /** 配置资源转移调度 */
  private void handleLeaveSourceTransfer(final Address address, final byte[] bytes) {
    final PartitionLeaveSourceTransferRecord record =
        decodeBytes(bytes, PartitionExecutionType.LEAVE_SOURCE_TRANSFER);
    actor.submit(
        () -> {
          final BusinessPartitionService businessPartitionService =
              raftPartitionMap.get(record.getPartitionId());
          final RaftPartition raftPartition = businessPartitionService.raftPartition();
          mergeSourceInfo(record.getAgentSourceIds(), raftPartition);
          final ClusterRaftConfiguration raftConfiguration =
              clusterMetaStore.getRaftConfiguration();
          raftConfiguration.setVersion(raftConfiguration.getVersion() + 1);
          clusterMetaStore.updateRaftConfiguration(raftConfiguration);
          dispatchComplete();
        });
  }

  /** 数据合并调度 */
  private void handleLeaveDataSourceTransfer(final Address address, final byte[] bytes) {
    final PartitionLeaveSourceDataTransferRecord record =
        decodeBytes(bytes, PartitionExecutionType.LEAVE_SOURCE_DATA_TRANSFER);
    actor.submit(
        () -> {
          final int partitionId = record.getPartitionId();
          final BusinessPartitionService businessPartitionService =
              raftPartitionMap.get(partitionId);
          if (businessPartitionService != null) {
            final PartitionId targetPartitionId =
                PartitionId.from(record.getTargetPartitionGroup(), record.getTargetPartitionId());
            businessPartitionService
                .dataMerge(targetPartitionId)
                .onComplete(
                    (unused, throwable) -> {
                      if (throwable != null) {
                        LOG.error("Data Merge Admin Raft Execution Service", throwable);
                        dispatchFailed(throwable);
                      } else {
                        dispatchComplete();
                      }
                    });
          } else {
            // 分区不在本节点时无数据可合并，完成调度并 ack，避免挂起记录每次重启重放
            LOG.info("Business partition {} is not started, skip data merge dispatch", partitionId);
            dispatchComplete();
          }
        });
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private <T extends PartitionExecutionRecordValue> T decodeBytes(
      final byte[] bytes, final PartitionExecutionType executionType) {
    final UnifiedRecordValue decode = decode(bytes, executionType);
    final T record = (T) decode;
    LOG.info("收到调度，调度类型: {}，调度信息: \n{}", executionType, record);
    addDispatch(
        record.getDispatchPlanId(), record.getDispatchPlanExecutionId(), executionType, bytes);
    return record;
  }

  /** 添加调度信息 */
  private void addDispatch(
      final long dispatchPlanId,
      final long dispatchPlanExecutionId,
      final PartitionExecutionType executionType,
      final byte[] bytes) {
    final ClusterRaftConfiguration raftConfiguration = clusterMetaStore.getRaftConfiguration();
    raftConfiguration.haveDispatch(dispatchPlanId, dispatchPlanExecutionId, executionType, bytes);
    raftConfiguration.setVersion(raftConfiguration.getVersion() + 1);
    clusterMetaStore.updateRaftConfiguration(raftConfiguration);
  }

  /** 调度完成不进行分区元数据更新 */
  private void dispatchComplete() {
    final ClusterRaftConfiguration raftConfiguration = clusterMetaStore.getRaftConfiguration();
    raftConfiguration.notDispatch();
    raftConfiguration.setVersion(raftConfiguration.getVersion() + 1);
    clusterMetaStore.updateRaftConfiguration(raftConfiguration);
    clusterPartitionTopology.publishBroadcast();
    executionAck(null);
  }

  /** 调度完成并移除某个分区元数据 */
  private void dispatchComplete(final PartitionId partitionId) {
    final ClusterRaftConfiguration raftConfiguration = clusterMetaStore.getRaftConfiguration();
    raftConfiguration.removePartition(partitionId);
    raftConfiguration.notDispatch();
    raftConfiguration.setVersion(raftConfiguration.getVersion() + 1);
    clusterMetaStore.updateRaftConfiguration(raftConfiguration);
    clusterPartitionTopology.publishBroadcast();
    executionAck(null);
  }

  /** 调度完成并添加某个分区元数据 */
  private void dispatchComplete(final PartitionMetadata metadata) {
    final ClusterRaftConfiguration raftConfiguration = clusterMetaStore.getRaftConfiguration();
    raftConfiguration.addPartition(metadata);
    raftConfiguration.notDispatch();
    raftConfiguration.setVersion(raftConfiguration.getVersion() + 1);
    clusterMetaStore.updateRaftConfiguration(raftConfiguration);
    clusterPartitionTopology.publishBroadcast();
    executionAck(null);
  }

  /** 调度执行失败：清除挂起标记（保留 meta 供 ack 组装）后上报失败 ack */
  private void dispatchFailed(final Throwable throwable) {
    final ClusterRaftConfiguration raftConfiguration = clusterMetaStore.getRaftConfiguration();
    if (raftConfiguration.haveDispatch()) {
      raftConfiguration.notDispatch();
      raftConfiguration.setVersion(raftConfiguration.getVersion() + 1);
      clusterMetaStore.updateRaftConfiguration(raftConfiguration);
    }
    executionAck(throwable);
  }

  /** 系统重启重新执行调度 */
  private void restartExecution() {
    final ClusterRaftConfiguration raftConfiguration = clusterMetaStore.getRaftConfiguration();
    if (raftConfiguration.haveDispatch()) {
      final DispatchMeta dispatchMeta = raftConfiguration.getDispatchMeta();
      if (dispatchMeta != null) {
        final byte[] bytes = dispatchMeta.bytes();
        final PartitionExecutionType partitionExecutionType = dispatchMeta.executionType();
        LOG.info("存在未完成调度，重启后重新执行，调度类型: {}，调度信息: \n{}", partitionExecutionType, dispatchMeta);
        switch (partitionExecutionType) {
          case BOOTSTRAP -> handleBootstrap(null, bytes);
          case JOIN -> handleJoin(null, bytes);
          case LEAVE -> handleLeave(null, bytes);
          case STOP -> handleStop(null, bytes);
          case CONFIG_CHANGE -> handleConfigChange(null, bytes);
          case LEAVE_SOURCE_TRANSFER -> handleLeaveSourceTransfer(null, bytes);
          case LEAVE_SOURCE_DATA_TRANSFER -> handleLeaveDataSourceTransfer(null, bytes);
          case BOOTSTRAP_SOURCE_TRANSFER -> handleBootstrapDataSourceTransfer(null, bytes);
          case BOOTSTRAP_SOURCE_DATA_TRANSFER -> handleBootstrapDataSourceTransfer(null, bytes);
        }
      }
    }
  }

  /** 调度信息 ack，failure 非空时上报失败结果 */
  private void executionAck(final Throwable failure) {
    try {
      final ClusterRaftConfiguration raftConfiguration = clusterMetaStore.getRaftConfiguration();
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
        clusterMetaStore.updateRaftConfiguration(raftConfiguration);
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
      actor.schedule(Duration.ofSeconds(delaySeconds), () -> executionAck(failure));
    }
  }
}
