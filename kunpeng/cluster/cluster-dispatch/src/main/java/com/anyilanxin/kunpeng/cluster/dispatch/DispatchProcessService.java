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
package com.anyilanxin.kunpeng.cluster.dispatch;

import static com.anyilanxin.kunpeng.cluster.dispatch.LogEventWriter.BROKER_VERSION;
import static com.anyilanxin.kunpeng.protocol.common.ClusterCommonConstant.*;

import com.anyilanxin.kunpeng.broker.client.admin.commandapi.CommandApiHandle;
import com.anyilanxin.kunpeng.cluster.cluster.ClusterMembershipService;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.MessagingService;
import com.anyilanxin.kunpeng.cluster.config.ClusterAdminConfiguration;
import com.anyilanxin.kunpeng.cluster.config.ClusterMetaStore;
import com.anyilanxin.kunpeng.cluster.config.topology.cluster.ClusterTopologyService;
import com.anyilanxin.kunpeng.cluster.dispatch.api.ClusterDispatchClient;
import com.anyilanxin.kunpeng.cluster.dispatch.api.ClusterDispatchService;
import com.anyilanxin.kunpeng.cluster.dispatch.eventlog.RecordAppendEntryFactory;
import com.anyilanxin.kunpeng.cluster.dispatch.eventlog.TypedRecordReader;
import com.anyilanxin.kunpeng.cluster.dispatch.exception.EngineErrorHandleException;
import com.anyilanxin.kunpeng.cluster.dispatch.exception.EngineRollbackException;
import com.anyilanxin.kunpeng.cluster.dispatch.scheduling.*;
import com.anyilanxin.kunpeng.cluster.dispatch.scheduling.cache.BoundedPendingCommandRegistry;
import com.anyilanxin.kunpeng.cluster.dispatch.scheduling.cache.RegistryMetrics;
import com.anyilanxin.kunpeng.configuration.broker.BrokerCfg;
import com.anyilanxin.kunpeng.configuration.broker.BusinessRaftCfg;
import com.anyilanxin.kunpeng.configuration.broker.ManageRaftCfg;
import com.anyilanxin.kunpeng.configuration.broker.PartitioningCfg;
import com.anyilanxin.kunpeng.eventlog.*;
import com.anyilanxin.kunpeng.kvstore.RepositoryTransaction;
import com.anyilanxin.kunpeng.kvstore.TransactionContext;
import com.anyilanxin.kunpeng.protocol.admin.AdminValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.admin.AdminValueType;
import com.anyilanxin.kunpeng.protocol.admin.impl.AdminRecordMetadata;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.DefaultRecordValueMapper;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.admin.AdminClusterMetaRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.admin.AdminDispatchPlanRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.business.BusinessDispatchPlanRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.common.PartitionInfoMetaRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.source.NodeSourceMetaRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.source.NodeSourceRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.source.PartitionSourceMetaRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.source.PartitionSourceRecord;
import com.anyilanxin.kunpeng.protocol.admin.record.RecordType;
import com.anyilanxin.kunpeng.protocol.admin.record.RecordValueMapper;
import com.anyilanxin.kunpeng.protocol.admin.record.command.admin.AdminClusterMetaLifeCycle;
import com.anyilanxin.kunpeng.protocol.admin.record.command.admin.AdminDispatchPlanLifeCycle;
import com.anyilanxin.kunpeng.protocol.admin.record.command.admin.AdminDispatchType;
import com.anyilanxin.kunpeng.protocol.admin.record.command.business.BusinessDispatchPlanLifeCycle;
import com.anyilanxin.kunpeng.protocol.admin.record.command.business.BusinessDispatchType;
import com.anyilanxin.kunpeng.protocol.admin.record.command.delayed.DelayedLifeCycle;
import com.anyilanxin.kunpeng.protocol.admin.record.command.source.NodeSourceLifeCycle;
import com.anyilanxin.kunpeng.protocol.admin.record.command.source.NodeSourceMetaLifeCycle;
import com.anyilanxin.kunpeng.protocol.admin.record.command.source.PartitionSourceLifeCycle;
import com.anyilanxin.kunpeng.protocol.admin.record.command.source.PartitionSourceMetaLifeCycle;
import com.anyilanxin.kunpeng.protocol.common.PartitionSourceMetadata;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.repository.admin.AdminRepository;
import com.anyilanxin.kunpeng.repository.admin.AdminRepositoryAppliers;
import com.anyilanxin.kunpeng.repository.admin.AdminRepositoryFactory;
import com.anyilanxin.kunpeng.repository.admin.modules.key.ImmutableRepositoryKey;
import com.anyilanxin.kunpeng.repository.admin.modules.key.MutableRepositoryKey;
import com.anyilanxin.kunpeng.repository.admin.modules.position.ImmutableRepositoryPosition;
import com.anyilanxin.kunpeng.repository.admin.modules.position.MutableRepositoryPosition;
import com.anyilanxin.kunpeng.scheduler.Actor;
import com.anyilanxin.kunpeng.scheduler.ActorSchedulingService;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;

/**
 * 日志事件处理状态机
 *
 * @author zxuanhong
 * @since
 */
public class DispatchProcessService extends Actor implements RecordAvailableListener {
  private final EventLog logStream;
  EventLogReader logStreamReader;
  EventLogWriter logStreamWriter;
  private boolean processing = false;
  public static final Logger LOGGER = ClusterDispatchLoggers.CLUSTER_DISPATCH;
  private long processPosition = -1;
  private MutableRepositoryPosition mutableRepositoryPosition;
  private ImmutableRepositoryPosition immutableRepositoryPosition;
  private final CommandApiHandle commandApiHandle;
  private final AdminRecordMetadata metadata = new AdminRecordMetadata();
  private final int sourceId;
  private final Set<Integer> agentSourceIds;
  private final int partitionId;
  private ImmutableRepositoryKey keyGenerator;
  private MutableRepositoryKey mutableKeyGenerator;
  private OrderedTimerScheduler primaryScheduler;
  private final AdminRepository repository;
  private RepositoryTransaction currentTransaction;
  private final RecordValueMapper recordValueMapper;
  private ClusterDispatchEngine bpmnEngine;
  private BatchProcessingCollect processingCollect;
  private final LogEventProcessors logEventProcessors;
  private final PartitionSourceMetadata partitionSourceMetadata;
  private final int maxBatch = 200;
  private LanePool lanePool;
  private final AdminRepositoryAppliers appliers;
  private final TransactionContext context;
  private final MeterRegistry meterRegistry;
  private final TimerClock clock;
  private final ActorSchedulingService actorSchedulingService;
  private final ClusterDispatchService dispatchService;
  private final ClusterDispatchClient dispatchClient;

  /** 流处理相位（执行门判据）：当前恒为 RUNNING；暂停/恢复能力落地时切换此值即可扣住定时任务。 */
  private final ExecutionPhase streamProcessorPhase = ExecutionPhase.RUNNING;

  private AsyncTimerRouter scheduleService;
  private LogEventWriter logEventWriter;
  private SchedulerContext schedulerContext;
  private final AdminRepositoryFactory repositoryFactory;
  private volatile ScheduledTasksHealthListener scheduledTasksHealthListener;
  private final ClusterMetaStore clusterMetaStore;
  private final ClusterMembershipService membershipService;
  private final BrokerCfg brokerCfg;
  private final ClusterTopologyService clusterTopologyService;

  /** 注册定时任务健康上报（车道 actor 失败/恢复时回调）；由 broker 接线到分区健康面。 */
  public void setScheduledTasksHealthListener(final ScheduledTasksHealthListener listener) {
    scheduledTasksHealthListener = listener;
  }

  /** 集群分区拓扑只读视图，供调度子系统查询成员/分区 Leader */
  public ClusterTopologyService getClusterTopologyService() {
    return clusterTopologyService;
  }

  public DispatchProcessService(
      final ClusterMetaStore clusterMetaStore,
      final EventLog logStream,
      final AdminRepositoryFactory repositoryFactory,
      final CommandApiHandle commandApiHandle,
      final PartitionSourceMetadata partitionSourceMetadata,
      final MeterRegistry meterRegistry,
      final TimerClock clock,
      final ActorSchedulingService actorSchedulingService,
      final MessagingService messagingService,
      final ClusterMembershipService membershipService,
      final ClusterDispatchClient dispatchClient,
      final BrokerCfg brokerCfg,
      final ClusterTopologyService clusterTopologyService) {
    this.brokerCfg = brokerCfg;
    this.clusterMetaStore = clusterMetaStore;
    this.dispatchClient = dispatchClient;
    this.membershipService = membershipService;
    this.clusterTopologyService = clusterTopologyService;
    dispatchService = new ClusterDispatchService(messagingService, this);
    this.meterRegistry = meterRegistry;
    this.actorSchedulingService = actorSchedulingService;
    this.clock = clock;
    this.repositoryFactory = repositoryFactory;
    recordValueMapper = DefaultRecordValueMapper.getInstance();
    this.logStream = logStream;
    repository = this.repositoryFactory.create();
    context = repository.getContext();
    appliers = repository.getAppliers();
    this.commandApiHandle = commandApiHandle;
    sourceId = partitionSourceMetadata.sourceId();
    agentSourceIds = partitionSourceMetadata.agentSourceIds();
    partitionId = partitionSourceMetadata.partitionId();
    logEventProcessors = new LogEventProcessors();
    this.partitionSourceMetadata = partitionSourceMetadata;
  }

  @Override
  protected void onActorStarting() {
    logStreamReader = logStream.newReader();
    logStreamWriter = logStream.newWriter();
    initScheduled();
    initProcessPosition();
  }

  private void initScheduled() {
    // 去重面与历史实现保持一致: 仅 JobLifeCycle.TIME_OUT 在途去重, 容量 100_000
    final var metrics = TimerMetrics.of(meterRegistry);
    final var registry =
        BoundedPendingCommandRegistry.forIntents(
            new RegistryMetrics.BoundedRegistryMetrics(meterRegistry), DelayedLifeCycle.TRIGGER);
    // 相位判据仅 RUNNING 放行; 当前恒为 RUNNING, 暂停能力落地时切 streamProcessorPhase 即生效;
    // 定时任务统一经车道 actor 执行, 与主处理隔离; 扫描间隔 250ms 保证到期执行的及时性上界
    final var schedulerFactory =
        new TimerSchedulerFactory(
            () -> streamProcessorPhase,
            () -> false,
            logStream::newWriter,
            registry,
            clock,
            Duration.ofMillis(250),
            metrics);

    primaryScheduler = schedulerFactory.create();
    lanePool = new LanePool(actorSchedulingService, schedulerFactory, partitionId);
    // 车道失败自愈: 池内原子重建失败车道; 失败即上报不健康, 重建完成后重振全部 checker 并恢复健康
    lanePool.setFailureListener(
        new LaneFailureListener() {
          @Override
          public void onLaneFailed(final ExecutionLane lane) {
            LOGGER.error(
                "Scheduling lane actor failed; timers on it stall until rebuilt. [lane: {}, partition: {}]",
                lane,
                partitionId);
            final var listener = scheduledTasksHealthListener;
            if (listener != null) {
              listener.onScheduledTasksHealth(false, "lane actor failed: " + lane);
            }
          }

          @Override
          public void onLaneRecovered(final ExecutionLane lane) {
            LOGGER.info(
                "Scheduling lane actor rebuilt; rearming checkers. [lane: {}, partition: {}]",
                lane,
                partitionId);
            actor.run(
                () ->
                    logEventWriter.schedulerCheckerAwares().forEach(SchedulerCheckerAware::rearm));
            final var listener = scheduledTasksHealthListener;
            if (listener != null) {
              listener.onScheduledTasksHealth(true, "lane actor rebuilt: " + lane);
            }
          }
        });
    // alwaysAsync 形态(沿用历史命名): 所有调度任务经车道执行, 主 actor 零调度负载
    scheduleService = AsyncTimerRouter.alwaysAsync(primaryScheduler, lanePool);
    schedulerContext = new SchedulerContext(scheduleService, partitionId, clock);
  }

  private void initProcessPosition() {
    mutableRepositoryPosition = repository.repositoryPosition();
    immutableRepositoryPosition = repository.repositoryPosition();
    keyGenerator = repository.repositoryKey();
    mutableKeyGenerator = repository.repositoryKey();
    processPosition = immutableRepositoryPosition.getLastSuccessfulProcessedRecordPosition();
    final ProcessingCollectSupplier collectSupplier = new ProcessingCollectSupplier();
    logEventWriter =
        new LogEventWriter(
            repositoryFactory,
            clock,
            repository,
            collectSupplier,
            partitionSourceMetadata,
            meterRegistry,
            clusterMetaStore,
            membershipService,
            dispatchClient,
            brokerCfg,
            clusterTopologyService);

    bpmnEngine =
        new ClusterDispatchEngine(
            logEventProcessors,
            repository,
            partitionSourceMetadata,
            meterRegistry,
            logEventWriter,
            collectSupplier);
    processingCollect = new BatchProcessingCollect(commandApiHandle, appliers, partitionId);
    logStreamReader.seekToNextEntry(processPosition);
  }

  @Override
  protected void onActorStarted() {
    // 调度器 attach(绑定 owner)必须发生在 STARTED 相位(STARTING 相位的投递不保证执行, 见调度器文档)
    primaryScheduler.attach(actor);
    lanePool
        .launch(actor)
        .onComplete(
            (ignored, error) -> {
              if (error != null) {
                return;
              }
              startProcessing();
            },
            actor);
  }

  private void startProcessing() {
    logStream.registerRecordAvailableListener(this);
    dispatchService.start(logStream.newWriter(), repositoryFactory);
    actor.submit(this::processNextEvent);
    actor.run(
        () -> {
          logEventWriter
              .schedulerCheckerAwares()
              .forEach(
                  v -> {
                    v.onRecovered(schedulerContext);
                  });
          adminInit();
        });
  }

  @Override
  public void close() {
    dispatchService.stop();
    logStream.removeRecordAvailableListener(this);
    logEventWriter.schedulerCheckerAwares().forEach(SchedulerCheckerAware::onClose);
    if (currentTransaction != null) {
      try {
        currentTransaction.rollback();
        currentTransaction = null;
      } catch (final Exception e) {
        throw new RuntimeException(e);
      }
    }
    if (lanePool != null) {
      actor.run(
          () -> {
            lanePool.shutdown(actor);
            primaryScheduler.close();
          });
    }
  }

  void processNextEvent() {
    if (processing || !logStreamReader.hasNext()) {
      return;
    }
    try {
      processing = true;
      while (logStreamReader.hasNext()) {
        final LoggedEntry loggedEvent = logStreamReader.next();
        final long position = loggedEvent.getPosition();
        if (position > processPosition) {
          metadata.reset();
          loggedEvent.readMetadata(metadata);
          final RecordType recordType = metadata.getRecordType();
          processPosition = position;
          if ((recordType == RecordType.COMMAND || recordType == RecordType.COMMAND_API)
              && !loggedEvent.isSkipProcessing()) {
            processEvent(loggedEvent);
          }
          // 处理/跳过即释放: 推进流控在途水位（释放窗口占位与在途环槽位）
          logStream.getFlowControl().onProcessed(processPosition);
        }
      }
    } finally {
      processing = false;
      actor.submit(this::processNextEvent);
    }
  }

  private void processEvent(final LoggedEntry loggedEvent) {
    try (final var ignore = processingCollect) {
      currentTransaction = context.getCurrentTransaction();
      try {
        currentTransaction.run(
            () -> {
              final AdminValueLifeCycle lifeCycle = metadata.getLifeCycle();
              final UnifiedRecordValue recordValue = recordValueMapper.getCacheValue(lifeCycle);
              loggedEvent.readValue(recordValue);
              final TypedRecordReader initialCommand = new TypedRecordReader(partitionId);
              initialCommand.wrap(loggedEvent, metadata, recordValue);
              processingCollect.addInitCommand(initialCommand);
              while (processingCollect.hasNext()) {
                final int processSize = processingCollect.processSize();
                final int stateSize = processingCollect.stateSize();
                if (processSize >= maxBatch || stateSize >= maxBatch) {
                  break;
                }
                try {
                  bpmnEngine.processEvent(processingCollect.next(), processingCollect);
                } catch (final Exception e) {
                  throw e;
                }
              }
              processingCollect.sendResponse();
              final List<AppendEntry> logAppendEntries = processingCollect.waitWrite();
              if (!logAppendEntries.isEmpty()) {
                logStreamWriter.tryAppend(WriteContext.INTERNAL, new ArrayList<>(logAppendEntries));
              }
              mutableRepositoryPosition.markAsProcessed(processPosition);
            });
        currentTransaction.commit();
        currentTransaction = null;
        final List<SideEffectProducer> sideEffectProducers = processingCollect.sideEffect();
        if (!sideEffectProducers.isEmpty()) {
          sideEffectProducers.forEach(SideEffectProducer::flush);
        }
      } catch (final EngineRollbackException rollbackException) {
        if (currentTransaction != null) {
          try {
            processingCollect.sendResponse();
            currentTransaction.rollback();
          } catch (final Exception e) {
            throw new RuntimeException(e);
          }
          currentTransaction = null;
        }
      } catch (final EngineErrorHandleException errorHandleException) {
        LOGGER.error("---需要内部处理--", errorHandleException);
      } catch (final Exception e) {
        LOGGER.error("---其他异常--", e);
      }
    }
  }

  @Override
  public void onRecordAvailable() {
    actor.submit(this::processNextEvent);
  }

  private void adminInit() {
    final ClusterAdminConfiguration adminConfiguration = clusterMetaStore.getAdminConfiguration();
    if (!adminConfiguration.isInitiator()) {

      final AdminRecordMetadata nodeSourceMetaMetadata =
          new AdminRecordMetadata()
              .recordType(RecordType.COMMAND)
              .recordVersion(1)
              .brokerVersion(BROKER_VERSION)
              .valueLifeCycle(NodeSourceMetaLifeCycle.CREATING)
              .valueType(AdminValueType.NODE_SOURCE_META);
      final NodeSourceMetaRecord nodeSourceMetaRecord =
          new NodeSourceMetaRecord().setVersion(1).setMaxNodeSourceId(INITIAL_NODE_SOURCE);
      final AppendEntry nodeSourceMetaAppendEntry =
          RecordAppendEntryFactory.of(nodeSourceMetaMetadata, nodeSourceMetaRecord);

      final AdminRecordMetadata nodeSourceMetadata =
          new AdminRecordMetadata()
              .recordType(RecordType.COMMAND)
              .recordVersion(1)
              .brokerVersion(BROKER_VERSION)
              .valueLifeCycle(NodeSourceLifeCycle.APPLYING)
              .valueType(AdminValueType.NODE_SOURCE);
      final NodeSourceRecord nodeSourceRecord =
          new NodeSourceRecord().setMemberId(membershipService.getLocalMember().id().id());
      final AppendEntry nodeSourceAppendEntry =
          RecordAppendEntryFactory.of(nodeSourceMetadata, nodeSourceRecord);

      final AdminRecordMetadata partitionSourceMetaMetadata =
          new AdminRecordMetadata()
              .recordType(RecordType.COMMAND)
              .recordVersion(1)
              .brokerVersion(BROKER_VERSION)
              .valueLifeCycle(PartitionSourceMetaLifeCycle.CREATING)
              .valueType(AdminValueType.PARTITION_SOURCE_META);
      final PartitionSourceMetaRecord partitionSourceMetaRecord =
          new PartitionSourceMetaRecord().setMaxPartitionSourceId(INITIAL_PARTITION_SOURCE);

      final AppendEntry partitionSourceMetaAppendEntry =
          RecordAppendEntryFactory.of(partitionSourceMetaMetadata, partitionSourceMetaRecord);

      final AdminRecordMetadata partitionSourceMetadata =
          new AdminRecordMetadata()
              .recordType(RecordType.COMMAND)
              .recordVersion(1)
              .brokerVersion(BROKER_VERSION)
              .valueLifeCycle(PartitionSourceLifeCycle.APPLYING)
              .valueType(AdminValueType.PARTITION_SOURCE);
      final PartitionSourceRecord partitionSourceRecord =
          new PartitionSourceRecord().setPartitionId(1).setPartitionGroup(ADMIN_RAFT_GROUP);

      final AppendEntry partitionSourceAppendEntry =
          RecordAppendEntryFactory.of(partitionSourceMetadata, partitionSourceRecord);

      // 初始化管理节点调度
      final ManageRaftCfg manage = brokerCfg.getManage();

      final AdminRecordMetadata adminMetadata =
          new AdminRecordMetadata()
              .recordType(RecordType.COMMAND)
              .recordVersion(1)
              .brokerVersion(BROKER_VERSION)
              .valueLifeCycle(AdminClusterMetaLifeCycle.CREATING)
              .valueType(AdminValueType.ADMIN_CLUSTER_META);
      final AdminClusterMetaRecord clusterMetaRecord =
          new AdminClusterMetaRecord()
              .setMeta(
                  new PartitionInfoMetaRecord()
                      .fromMetadata(adminConfiguration.getAdminPartition()))
              .setCurrentReplicationFactor(1)
              .setReplicationFactor(1)
              .setVersion(1)
              .setCreateTime(logEventWriter.millis());
      final AppendEntry adminAppendEntry =
          RecordAppendEntryFactory.of(adminMetadata, clusterMetaRecord);

      final AdminRecordMetadata adminPlanMetadata =
          new AdminRecordMetadata()
              .recordType(RecordType.COMMAND)
              .recordVersion(1)
              .brokerVersion(BROKER_VERSION)
              .valueLifeCycle(AdminDispatchPlanLifeCycle.CHANGE_REPLICATION)
              .valueType(AdminValueType.ADMIN_DISPATCH);
      final AdminDispatchPlanRecord clusterPlanMetaRecord =
          new AdminDispatchPlanRecord()
              .setInitialize(true)
              .setExpectReplicationFactor(manage.getReplicationFactor())
              .setDispatchPlanType(AdminDispatchType.CHANGE_REPLICATION);
      final AppendEntry adminPlanAppendEntry =
          RecordAppendEntryFactory.of(adminPlanMetadata, clusterPlanMetaRecord);

      // 初始化业务节点调度
      final BusinessRaftCfg raft = brokerCfg.getRaft();
      final PartitioningCfg partitioning = raft.getPartitioning();
      final AdminRecordMetadata businessMetadata =
          new AdminRecordMetadata()
              .recordType(RecordType.COMMAND)
              .recordVersion(1)
              .brokerVersion(BROKER_VERSION)
              .valueLifeCycle(BusinessDispatchPlanLifeCycle.CHANGE_PARTITION)
              .valueType(AdminValueType.BUSINESS_DISPATCH);

      final BusinessDispatchPlanRecord businessDispatchPlanRecord =
          new BusinessDispatchPlanRecord()
              .setApplyPlan(true)
              .setInitialize(true)
              .setExpectPartitionsCount(partitioning.getPartitionsCount())
              .setDispatchPlanType(BusinessDispatchType.CHANGE_PARTITION);
      final AppendEntry businessAppendEntry =
          RecordAppendEntryFactory.of(businessMetadata, businessDispatchPlanRecord);

      // 发送raft 日志
      logStreamWriter.tryAppend(
          WriteContext.INTERNAL,
          List.of(
              nodeSourceMetaAppendEntry,
              nodeSourceAppendEntry,
              partitionSourceMetaAppendEntry,
              partitionSourceAppendEntry,
              adminAppendEntry,
              adminPlanAppendEntry,
              businessAppendEntry));
    }
  }
}
