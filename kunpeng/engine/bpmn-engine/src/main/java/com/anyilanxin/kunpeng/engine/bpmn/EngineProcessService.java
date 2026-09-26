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
package com.anyilanxin.kunpeng.engine.bpmn;

import com.anyilanxin.kunpeng.broker.client.business.commandapi.CommandApiHandle;
import com.anyilanxin.kunpeng.cluster.business.step.RaftPartitionSource;
import com.anyilanxin.kunpeng.cluster.cluster.PartitionId;
import com.anyilanxin.kunpeng.engine.bpmn.exception.EngineErrorHandleException;
import com.anyilanxin.kunpeng.engine.bpmn.exception.EngineRollbackException;
import com.anyilanxin.kunpeng.engine.bpmn.scheduling.*;
import com.anyilanxin.kunpeng.engine.bpmn.scheduling.cache.BoundedPendingCommandRegistry;
import com.anyilanxin.kunpeng.engine.bpmn.scheduling.cache.RegistryMetrics;
import com.anyilanxin.kunpeng.eventlog.*;
import com.anyilanxin.kunpeng.kvstore.RepositoryTransaction;
import com.anyilanxin.kunpeng.kvstore.TransactionContext;
import com.anyilanxin.kunpeng.protocol.business.ValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.impl.RecordMetadata;
import com.anyilanxin.kunpeng.protocol.business.impl.eventlog.TypedRecordReader;
import com.anyilanxin.kunpeng.protocol.business.impl.record.DefaultRecordValueMapper;
import com.anyilanxin.kunpeng.protocol.business.record.RecordType;
import com.anyilanxin.kunpeng.protocol.business.record.RecordValueMapper;
import com.anyilanxin.kunpeng.protocol.business.record.command.job.JobLifeCycle;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.repository.business.BusinessRepository;
import com.anyilanxin.kunpeng.repository.business.BusinessRepositoryAppliers;
import com.anyilanxin.kunpeng.repository.business.BusinessRepositoryFactory;
import com.anyilanxin.kunpeng.repository.business.modules.key.ImmutableKeyGeneratorRepository;
import com.anyilanxin.kunpeng.repository.business.modules.key.MutableKeyGeneratorRepository;
import com.anyilanxin.kunpeng.repository.business.modules.position.MutableProcessedPositionRepository;
import com.anyilanxin.kunpeng.scheduler.Actor;
import com.anyilanxin.kunpeng.scheduler.ActorSchedulingService;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.springframework.beans.factory.BeanFactory;

/**
 * 日志事件处理状态机
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class EngineProcessService extends Actor implements RecordAvailableListener {
  private static final int DEFAULT_MAX_BATCH = 100;

  private final EventLog logStream;
  EventLogReader logStreamReader;
  EventLogWriter logStreamWriter;
  private boolean processing = false;
  public static final Logger LOGGER = Loggers.SYSTEM_LOGGER;
  private long processPosition = -1;
  private MutableProcessedPositionRepository mutableProcessedPosition;
  private final CommandApiHandle commandApiHandle;
  private final RecordMetadata metadata = new RecordMetadata();
  private ImmutableKeyGeneratorRepository keyGenerator;
  private MutableKeyGeneratorRepository mutableKeyGenerator;
  private OrderedTimerScheduler primaryScheduler;
  private final BusinessRepository repository;
  private RepositoryTransaction currentTransaction;
  private final BeanFactory beanFactory;
  private final RecordValueMapper recordValueMapper;
  private BpmnEngine bpmnEngine;
  private BatchProcessingCollect processingCollect;
  private final LogEventProcessors logEventProcessors;
  private final RaftPartitionSource partitionSource;
  private final PartitionId partitionId;
  private final InterPartitionCommandSender commandSender;
  private final int maxBatch;
  private LanePool lanePool;
  private final BusinessRepositoryAppliers appliers;
  private final TransactionContext context;
  private final MeterRegistry meterRegistry;
  private final TimerClock clock;
  private final ActorSchedulingService actorSchedulingService;

  /** 流处理相位（执行门判据）：当前恒为 RUNNING；暂停/恢复能力落地时切换此值即可扣住定时任务。 */
  private final ExecutionPhase streamProcessorPhase = ExecutionPhase.RUNNING;

  private AsyncTimerRouter scheduleService;
  private LogEventWriter logEventWriter;
  private SchedulerContext schedulerContext;
  private final BusinessRepositoryFactory repositoryFactory;
  private final JobDeliveryPort jobDeliveryPort;
  private volatile ScheduledTasksHealthListener scheduledTasksHealthListener;

  /** 注册定时任务健康上报（车道 actor 失败/恢复时回调）；由 broker 接线到分区健康面。 */
  public void setScheduledTasksHealthListener(final ScheduledTasksHealthListener listener) {
    scheduledTasksHealthListener = listener;
  }

  public EngineProcessService(
      final JobDeliveryPort jobDeliveryPort,
      final BeanFactory beanFactory,
      final EventLog logStream,
      final BusinessRepositoryFactory repositoryFactory,
      final CommandApiHandle commandApiHandle,
      final RaftPartitionSource partitionSource,
      final InterPartitionCommandSender commandSender,
      final MeterRegistry meterRegistry,
      final TimerClock clock,
      final ActorSchedulingService actorSchedulingService) {
    this.partitionSource = partitionSource;
    partitionId = partitionSource.getPartitionId();
    this.meterRegistry = meterRegistry;
    this.jobDeliveryPort = jobDeliveryPort;
    this.commandSender = commandSender;
    this.actorSchedulingService = actorSchedulingService;
    this.clock = clock;
    this.repositoryFactory = repositoryFactory;
    recordValueMapper = DefaultRecordValueMapper.getInstance();
    this.beanFactory = beanFactory;
    this.logStream = logStream;
    repository = this.repositoryFactory.create();
    context = repository.getContext();
    appliers = repository.getAppliers();
    this.commandApiHandle = commandApiHandle;
    logEventProcessors = new LogEventProcessors();
    maxBatch = DEFAULT_MAX_BATCH;
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
            new RegistryMetrics.BoundedRegistryMetrics(meterRegistry), JobLifeCycle.TIME_OUT);

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
    lanePool = new LanePool(actorSchedulingService, schedulerFactory, partitionId.id());
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
    schedulerContext = new SchedulerContext(scheduleService, partitionId.id(), clock);
  }

  private void initProcessPosition() {
    mutableProcessedPosition = repository.processedPositionRepository();
    keyGenerator = repository.keyGeneratorRepository();
    mutableKeyGenerator = repository.keyGeneratorRepository();
    processPosition = mutableProcessedPosition.getLastSuccessfulProcessedRecordPosition();
    final ProcessingCollectSupplier collectSupplier = new ProcessingCollectSupplier();
    logEventWriter =
        new LogEventWriter(
            repositoryFactory,
            clock,
            repository,
            collectSupplier,
            partitionSource,
            partitionId.id(),
            beanFactory,
            commandSender,
            jobDeliveryPort,
            meterRegistry);

    bpmnEngine =
        new BpmnEngine(
            logEventProcessors,
            repository,
            partitionSource,
            beanFactory,
            commandSender,
            meterRegistry,
            logEventWriter,
            collectSupplier);
    processingCollect = new BatchProcessingCollect(commandApiHandle, appliers, partitionId.id());
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
    actor.submit(this::processNextEvent);
    actor.run(
        () ->
            logEventWriter
                .schedulerCheckerAwares()
                .forEach(
                    v -> {
                      v.onRecovered(schedulerContext);
                    }));
  }

  @Override
  public void close() {
    logStream.removeRecordAvailableListener(this);
    logEventWriter
        .schedulerCheckerAwares()
        .forEach(
            v -> {
              v.onClose();
            });
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
          } else {
            System.out.println("--metadata--1-" + metadata.getLifeCycle().name());
            System.out.println("--metadata--2-" + metadata.getValueType().name());
            System.out.println("--metadata--3-" + loggedEvent.getKey());
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
              final ValueLifeCycle lifeCycle = metadata.getLifeCycle();
              final UnifiedRecordValue recordValue = recordValueMapper.getCacheValue(lifeCycle);
              loggedEvent.readValue(recordValue);
              final TypedRecordReader initialCommand = new TypedRecordReader(partitionId.id());
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
              mutableProcessedPosition.markAsProcessed(processPosition);
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
}
