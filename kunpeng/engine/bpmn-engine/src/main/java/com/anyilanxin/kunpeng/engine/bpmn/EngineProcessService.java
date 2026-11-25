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
import com.anyilanxin.kunpeng.protocol.business.ValueType;
import com.anyilanxin.kunpeng.protocol.business.impl.RecordMetadata;
import com.anyilanxin.kunpeng.protocol.business.impl.eventlog.TypedRecordReader;
import com.anyilanxin.kunpeng.protocol.business.impl.record.DefaultRecordValueMapper;
import com.anyilanxin.kunpeng.protocol.business.record.RecordType;
import com.anyilanxin.kunpeng.protocol.business.record.RecordValueMapper;
import com.anyilanxin.kunpeng.protocol.business.record.command.job.JobLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.timer.TimerLifeCycle;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.repository.business.BusinessRepository;
import com.anyilanxin.kunpeng.repository.business.BusinessRepositoryAppliers;
import com.anyilanxin.kunpeng.repository.business.BusinessRepositoryFactory;
import com.anyilanxin.kunpeng.repository.business.modules.key.ImmutableKeyGeneratorRepository;
import com.anyilanxin.kunpeng.repository.business.modules.key.MutableKeyGeneratorRepository;
import com.anyilanxin.kunpeng.repository.business.modules.position.MutableProcessedPositionRepository;
import com.anyilanxin.kunpeng.scheduler.Actor;
import com.anyilanxin.kunpeng.scheduler.ActorSchedulingService;
import com.anyilanxin.kunpeng.scheduler.future.ActorFuture;
import com.anyilanxin.kunpeng.scheduler.future.CompletableActorFuture;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.springframework.beans.factory.BeanFactory;

/**
 * 日志事件处理状态机
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class EngineProcessService extends Actor implements RecordAvailableListener {
  public static final Logger LOGGER = Loggers.SYSTEM_LOGGER;
  private static final int DEFAULT_MAX_BATCH = 100;

  /**
   * 单个 job 内最多处理的日志条目数。整个积压在调度器眼里只是一个信封（按信封计数的排空预算管不到它）， 追平/恢复期间的百万级积压会独占载体线程；到量后经 runBehind
   * 续跑，把线程让给已排队的控制指令与其他 actor。
   */
  private static final int MAX_ENTRIES_PER_JOB = 1000;

  private final EventLog logStream;
  private EventLogReader logStreamReader;
  private BatchEntryReader logStreamBatchReader;
  private EventLogWriter logStreamWriter;
  private boolean processing = false;
  private long processPosition = -1;

  /** 冻结在途 future（数据手术窗口或 leader 转移窗口；null=未冻结） */
  private volatile CompletableActorFuture<Void> freezeFuture;

  /** 当前冻结的原因；只在 {@link #freezeFuture} 非 null 时有意义，与它一起读写 */
  private volatile FreezeReason freezeReason;

  /** closeAsync 已开始：之后不再接受新的冻结（leader 转移可能在退位的同一瞬间发起） */
  private volatile boolean closing;

  private volatile boolean drainAfterQuiesce;
  private volatile TimerScheduler.TimerHandle drainPollHandle;
  private final Object freezeLock = new Object();
  private static final long FREEZE_DRAIN_POLL_MS = 500;

  /** 冻结的原因。两种冻结都停掉分区全部外部写入，区别在持续多久、要不要留痕。 */
  private enum FreezeReason {
    /** 数据手术：持续分钟到小时，写持久标记，重启后凭标记重新进入 */
    DATA_SURGERY,
    /** leader 转移：秒级，纯内存状态，不写标记也不排干 */
    LEADERSHIP_TRANSFER
  }

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
  private PendingCommandRegistry.Stageable pendingCommandRegistry;
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
    logStreamBatchReader = logStream.newBatchReader();
    logStreamWriter = logStream.newWriter();
    initScheduled();
    initProcessPosition();
  }

  private void initScheduled() {
    // Timer TRIGGER 与 Job TIME_OUT 都在扫描到处理完成之间去重；处理结束由主 actor 释放。
    final var metrics = TimerMetrics.of(meterRegistry, partitionId.id());
    pendingCommandRegistry =
        BoundedPendingCommandRegistry.forIntents(
            new RegistryMetrics.BoundedRegistryMetrics(meterRegistry, partitionId.id()),
            JobLifeCycle.TIME_OUT,
            TimerLifeCycle.TRIGGER);

    // 相位判据仅 RUNNING 放行; 当前恒为 RUNNING, 暂停能力落地时切 streamProcessorPhase 即生效;
    // 中止判据恒 false: 定时任务的关闭权威在各 checker 的 onClose(句柄取消+任务自查), 调度器层不做兜底;
    // 定时任务统一经车道 actor 执行, 与主处理隔离; 周期扫描只作时钟校正兜底, 到点执行由调度器为队首补的一次性唤醒保证
    final var schedulerFactory =
        new TimerSchedulerFactory(
            () -> streamProcessorPhase,
            () -> false,
            logStream::newWriter,
            pendingCommandRegistry,
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
    logStreamBatchReader.seekToNextBatch(processPosition);
  }

  @Override
  protected void onActorStarted() {
    startProcessing();
  }

  private void startProcessing() {
    final ActorFuture<Void> future = actor.createFuture();
    recover(future);
    future.onComplete(
        (_, throwable) -> {
          if (throwable == null) {
            // 调度器 attach(绑定 owner)必须发生在 STARTED 相位(STARTING 相位的投递不保证执行, 见调度器文档)
            primaryScheduler.attach(actor);
            lanePool
                .launch(actor)
                .onComplete(
                    (ignored, error) -> {
                      if (error != null) {
                        return;
                      }
                      recoverFrozenMarker();
                      logStreamReader.seekToNextEntry(processPosition);
                      processing();
                    },
                    actor);
          }
        });
  }

  /**
   * 冻结标记恢复：broker 手术中重启后内存冻结态丢失，凭 PARTITION_STATE 标记重入冻结窗口（含两段排水重走）—— 手术的后续明细 （合并/拆分/删除）经
   * restartExecution 重放到达时分区已重新静止，窗口不破防（03 号文档 §3.2）。
   */
  private void recoverFrozenMarker() {
    final Boolean frozenDrain = repository.frozenStateRepository().frozenMarker();
    if (frozenDrain != null) {
      LOGGER.info(
          "Partition {} recovering frozen state from marker (drain={})", partitionId, frozenDrain);
      freezeAndDrain(frozenDrain);
    }
  }

  private void recover(final ActorFuture<Void> future) {
    final long lastCommittedPosition = logStream.getLastCommittedPosition();
    LOGGER.info(
        "Log replay starting from processed position {} to last committed position {} [partition:"
            + " {}]",
        processPosition,
        lastCommittedPosition,
        partitionId);
    final long startTime = clock.millis();
    int replayedBatches = 0;
    try {
      while (logStreamBatchReader.hasNext()) {
        final BatchEntryReader.Batch batch = logStreamBatchReader.next();
        replayedBatches++;
        final boolean b = processReplayEvent(batch, lastCommittedPosition);
        if (!b) {
          break;
        }
      }
    } catch (final Exception e) {
      // 回放失败必须让恢复失败，伪装成功会让分区带残缺状态继续服务
      future.completeExceptionally(e);
      return;
    } finally {
      logStreamBatchReader.close();
    }
    LOGGER.info(
        "Log replay finished at position {} with {} batches, took {} ms [partition: {}]",
        processPosition,
        replayedBatches,
        clock.millis() - startTime,
        partitionId);
    future.complete(null);
  }

  boolean processReplayEvent(final BatchEntryReader.Batch batch, final long lastCommittedPosition)
      throws Exception {
    final AtomicBoolean continueProcess = new AtomicBoolean(false);
    currentTransaction = context.getCurrentTransaction();
    currentTransaction.run(
        () -> {
          while (batch.hasNext()) {
            final LoggedEntry loggedEvent = batch.next();
            metadata.reset();
            loggedEvent.readMetadata(metadata);
            final ValueType valueType = metadata.getValueType();
            final ValueLifeCycle lifeCycle = metadata.getLifeCycle();
            final RecordType recordType = metadata.getRecordType();
            final long key = loggedEvent.getKey();
            if (recordType == RecordType.EVENT) {
              final UnifiedRecordValue recordValue = recordValueMapper.getCacheValue(lifeCycle);
              recordValue.reset();
              loggedEvent.readValue(recordValue);
              appliers.applyState(key, valueType, lifeCycle, recordValue);
            }
            processPosition = loggedEvent.getPosition();
            continueProcess.set(lastCommittedPosition > processPosition);
            mutableKeyGenerator.setKeyIfHigher(key);
          }
          mutableProcessedPosition.markAsProcessed(processPosition);
        });
    currentTransaction.commit();
    currentTransaction = null;
    return continueProcess.get();
  }

  private void processing() {
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

  /**
   * 异步关闭：先经各 checker 的 onClose 取消定时任务并回滚在途事务，随后在引擎 actor 上关闭车道池与主调度器。
   *
   * <p>返回的 future 完成即本引擎不再执行任何定时任务（含飞行中任务）——关闭链必须等它完成后再放行后续步骤关闭 RocksDB，否则跨越关库窗口的定时任务会撞上已关闭的存储。
   */
  @Override
  public ActorFuture<Void> closeAsync() {
    closing = true;
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
    logStreamWriter.close();
    logStreamReader.close();
    if (lanePool == null) {
      return CompletableActorFuture.completed();
    }
    final CompletableActorFuture<Void> drained = new CompletableActorFuture<>();
    actor.run(
        () -> {
          // 转移冻结只在本任期内有效：转移成功时老 leader 直接退位、没有人来解冻，随引擎关闭一并解除
          final CompletableActorFuture<Void> transferFreeze = freezeFuture;
          if (transferFreeze != null && freezeReason == FreezeReason.LEADERSHIP_TRANSFER) {
            freezeFuture = null;
            freezeReason = null;
            // 已静止的冻结 future 已经正常完成，只有还在等静止的才需要以失败完结
            if (!transferFreeze.isDone()) {
              transferFreeze.completeExceptionally(
                  new IllegalStateException(
                      "partition "
                          + partitionId
                          + " closed while frozen for a leadership transfer"));
            }
            logStreamWriter.unfreeze();
          }
          lanePool
              .shutdown(actor)
              .onComplete(
                  (ignored, error) -> {
                    primaryScheduler.close();
                    if (error != null) {
                      drained.completeExceptionally(error);
                    } else {
                      drained.complete(null);
                    }
                  });
        });
    return drained;
  }

  /**
   * 冻结并排干（数据手术前置，幂等）：冻结写入（仅放行引擎处理写入与内部补写）→ 暂停全部内部触发（checker 家族）→ 等处理追平日志已定序水位（final 确定）→ {@code
   * drain=true} 时等全部 sink 确认位追平 final。
   *
   * <p>幂等契约：冻结期间重复调用（看门狗重发）返回同一在途 future。完成回调运行于完成方线程（引擎 actor 或 排水轮询车道），调用方如需回写自身 actor 状态须自行提交回目标
   * actor。
   *
   * <p>与 leader 转移冻结（{@link #freezeForLeadershipTransfer}）互斥：转移冻结进行中时立即失败。
   */
  public ActorFuture<Void> freezeAndDrain(final boolean drain) {
    synchronized (freezeLock) {
      final CompletableActorFuture<Void> existing = freezeFuture;
      if (existing != null) {
        // 看门狗重发复用在途的手术冻结；在途的是 leader 转移冻结，立即失败走手术自己的重试链
        return freezeReason == FreezeReason.DATA_SURGERY
            ? existing
            : CompletableActorFuture.completedExceptionally(
                new IllegalStateException(
                    "partition " + partitionId + " is frozen for a leadership transfer"));
      }
      final CompletableActorFuture<Void> future = new CompletableActorFuture<>();
      // 在锁内登记冻结：等 actor 任务执行时才登记的话，本方法返回后、任务执行前，并发的另一次冻结看不到它在途
      reserveFreeze(future, FreezeReason.DATA_SURGERY, drain);
      queueFreeze(future, FreezeReason.DATA_SURGERY, drain);
      return future;
    }
  }

  /**
   * 为 leader 转移冻结写入：冻结全部外部写入、暂停内部触发，等引擎静止——冻结之前收下的命令到此全部处理并应答完毕，日志头不再移动。
   *
   * <p>与数据手术冻结互斥：任一种冻结已在进行都立即失败。返回的 future 在引擎静止时完成。
   */
  public ActorFuture<Void> freezeForLeadershipTransfer() {
    synchronized (freezeLock) {
      final CompletableActorFuture<Void> existing = freezeFuture;
      if (existing != null) {
        return CompletableActorFuture.completedExceptionally(
            new IllegalStateException(
                "partition " + partitionId + " is already frozen for " + freezeReason));
      }
      final CompletableActorFuture<Void> future = new CompletableActorFuture<>();
      reserveFreeze(future, FreezeReason.LEADERSHIP_TRANSFER, false);
      queueFreeze(future, FreezeReason.LEADERSHIP_TRANSFER, false);
      return future;
    }
  }

  /** 解除冻结（数据手术收尾，幂等）：取消排干轮询、恢复内部触发、恢复全部写入； 未排干即解冻按失败完成冻结 future（调用方据此走失败 ack 重试）。 */
  public ActorFuture<Void> resumeFromFreeze() {
    final CompletableActorFuture<Void> future = new CompletableActorFuture<>();
    actor.run(
        () -> {
          if (freezeFuture != null && freezeReason == FreezeReason.LEADERSHIP_TRANSFER) {
            // 手术收尾不能解开转移冻结：转移暂停期间 Raft 层要求分区零追加
            future.completeExceptionally(
                new IllegalStateException(
                    "partition " + partitionId + " is frozen for a leadership transfer"));
            return;
          }
          // 先清标记再开闸：中途崩溃重启会凭残留标记重新冻结——保守方向无害（未冻当已冻只多等一次排干）
          repository.frozenStateRepository().clearFrozen();
          liftFreeze();
          LOGGER.info("Partition {} resumed from freeze", partitionId);
          future.complete(null);
        });
    return future;
  }

  /** 解除 leader 转移冻结（幂等）：没有转移冻结时什么都不做，数据手术冻结不受影响。 */
  public ActorFuture<Void> unfreezeFromLeadershipTransfer() {
    final CompletableActorFuture<Void> future = new CompletableActorFuture<>();
    actor.run(
        () -> {
          if (!closing
              && freezeFuture != null
              && freezeReason == FreezeReason.LEADERSHIP_TRANSFER) {
            liftFreeze();
            LOGGER.info("Partition {} unfrozen after leadership transfer", partitionId);
          }
          future.complete(null);
        });
    return future;
  }

  /** 当前是否处于冻结窗口（数据手术或 leader 转移，含排干等待中） */
  public boolean isFrozen() {
    return freezeFuture != null;
  }

  /** 是否因数据手术处于冻结窗口（含排干等待中）。leader 转移冻结不算。 */
  public boolean isFrozenForDataSurgery() {
    return freezeFuture != null && freezeReason == FreezeReason.DATA_SURGERY;
  }

  /** 冻结排干剩余量（final − 全部 sink 最低确认位）；未冻结或不排干返回 -1，已静止待排干为非负 */
  public long freezeRemaining() {
    if (freezeFuture == null || !drainAfterQuiesce) {
      return -1;
    }
    final long lastWritten =
        Math.max(logStream.getLastWrittenPosition(), logStream.getLastCommittedPosition());
    return lastWritten < 1 ? 0 : lastWritten - sinkLowestPosition();
  }

  /** 登记一次冻结（持有 freezeLock）：此后对外的查询都能看到分区已冻结，实际生效在引擎 actor 上完成。 */
  private void reserveFreeze(
      final CompletableActorFuture<Void> future, final FreezeReason reason, final boolean drain) {
    freezeFuture = future;
    freezeReason = reason;
    drainAfterQuiesce = drain;
  }

  /** 撤回 {@link #reserveFreeze} 的登记：冻结没能生效，不能让分区看起来仍在冻结中。 */
  private void releaseFreezeReservation(final CompletableActorFuture<Void> future) {
    if (freezeFuture == future) {
      freezeFuture = null;
      freezeReason = null;
    }
  }

  /** 到引擎 actor 上让已登记的冻结生效；引擎没在服务（未启动完成或已在关闭）则撤回登记并以失败完结。 */
  private void queueFreeze(
      final CompletableActorFuture<Void> future, final FreezeReason reason, final boolean drain) {
    actor.run(
        () -> {
          if (logStreamReader == null || closing) {
            releaseFreezeReservation(future);
            future.completeExceptionally(
                new IllegalStateException(
                    "partition " + partitionId + " is not serving; cannot freeze"));
            return;
          }
          applyFreeze(reason, drain);
        });
  }

  /** 冻结生效（引擎 actor 上）：冻结全部外部写入、暂停内部触发，随后开始等静止。冻结已由调用方登记。 */
  private void applyFreeze(final FreezeReason reason, final boolean drain) {
    logStreamWriter.freeze();
    if (reason == FreezeReason.DATA_SURGERY) {
      // 只有手术写标记：它用来在重启后重新进入冻结；转移是秒级的内存状态，写了标记节点下次当上 leader 时会被错误冻结
      repository.frozenStateRepository().markFrozen(drain);
    }
    logEventWriter.schedulerCheckerAwares().forEach(SchedulerCheckerAware::onPaused);
    LOGGER.info("Partition {} frozen ({}), waiting quiesce", partitionId, reason);
    checkQuiesced();
  }

  /** 解除冻结（引擎 actor 上）：取消排干轮询、恢复内部触发与全部写入；没等到排干或静止的冻结 future 以失败完结。持久标记由调用方负责。 */
  private void liftFreeze() {
    cancelDrainPoll();
    final CompletableActorFuture<Void> freezing = freezeFuture;
    freezeFuture = null;
    freezeReason = null;
    if (freezing != null && !freezing.isDone()) {
      freezing.completeExceptionally(
          new IllegalStateException("partition " + partitionId + " resumed before drained"));
    }
    logEventWriter.schedulerCheckerAwares().forEach(SchedulerCheckerAware::onResumed);
    logStreamWriter.unfreeze();
  }

  private void cancelDrainPoll() {
    final TimerScheduler.TimerHandle handle = drainPollHandle;
    drainPollHandle = null;
    if (handle != null) {
      handle.cancel();
    }
  }

  /** 引擎静止判定（引擎 actor 上调用）：无在途处理、无待消费条目、处理位追平已定序水位（自写未提交的尾巴由 lastWritten 挡住，不会误判静止）。 */
  private void checkQuiesced() {
    final CompletableActorFuture<Void> future = freezeFuture;
    if (future == null || future.isDone()) {
      return;
    }
    if (processing || logStreamReader == null || logStreamReader.hasNext()) {
      return;
    }
    final long lastWritten =
        Math.max(logStream.getLastWrittenPosition(), logStream.getLastCommittedPosition());
    if (lastWritten > 0 && processPosition < lastWritten) {
      return;
    }
    if (!drainAfterQuiesce) {
      future.complete(null);
      return;
    }
    // 第二段排水：等全部 sink 确认位追平 final（final<1 即空日志或无 sink 状态，视为已排干）
    if (lastWritten < 1 || sinkLowestPosition() >= lastWritten) {
      future.complete(null);
      return;
    }
    if (drainPollHandle == null) {
      drainPollHandle =
          primaryScheduler.scheduleEvery(
              java.time.Duration.ofMillis(FREEZE_DRAIN_POLL_MS),
              output -> {
                if (sinkLowestPosition() >= lastWritten) {
                  cancelDrainPoll();
                  final CompletableActorFuture<Void> draining = freezeFuture;
                  if (draining != null) {
                    draining.complete(null);
                  }
                }
                return output.build();
              });
    }
  }

  /** 全部 sink 最低确认位（repository 直读；未装配 sink 为 -1） */
  private long sinkLowestPosition() {
    return repository.sinkRepository().getLowestPosition();
  }

  void processNextEvent() {
    if (processing || !logStreamReader.hasNext()) {
      return;
    }
    boolean yielded = false;
    try {
      processing = true;
      int processedInJob = 0;
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
            try {
              processEvent(loggedEvent);
            } finally {
              // processEvent 返回时批次已提交或回滚；此刻才允许后续扫描重新触发同一 key。
              // append 成功只代表定序，不能提前释放，否则下一轮会重复灌入相同调度命令。
              pendingCommandRegistry.remove(metadata.getLifeCycle(), loggedEvent.getKey());
            }
          }
          // 处理/跳过即释放: 推进流控在途水位（释放窗口占位与在途环槽位）
          logStream.getFlowControl().onProcessed(processPosition);
        }
        if (++processedInJob >= MAX_ENTRIES_PER_JOB && logStreamReader.hasNext()) {
          yielded = true;
          break;
        }
      }
    } finally {
      processing = false;
      checkQuiesced();
      if (yielded) {
        // 续跑排到外部队列尾部：已排队的控制指令与其他 actor 先得到执行
        actor.runBehind(this::processNextEvent);
      } else {
        actor.submit(this::processNextEvent);
      }
    }
  }

  private void processEvent(final LoggedEntry loggedEvent) {
    // 初始命令的请求 id 在批处理前留存：批次失败时用于失败应答闭环
    final long requestId = metadata.getRequestId();
    // 批次开始清空变量快照缓存：跨批次的仓储变更不进新批次
    logEventWriter.behavior().variableBehavior().resetBatchCache();
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
        LOGGER.error("引擎处理内部异常，回滚当前批次并回失败应答", errorHandleException);
        rollbackFailedBatch(requestId, errorHandleException);
      } catch (final Exception e) {
        LOGGER.error("引擎处理未知异常，回滚当前批次并回失败应答", e);
        rollbackFailedBatch(requestId, e);
      }
    }
  }

  /**
   * 批次处理失败收尾：回滚当前事务，并以失败应答闭环初始命令的请求（批次内半置的成功应答一并覆盖）。
   *
   * <p>事务不回滚会让事务上下文滞留在途状态，污染该分区后续所有批次；不回失败应答会让网关侧请求悬挂到超时。
   */
  private void rollbackFailedBatch(final long requestId, final Throwable error) {
    if (currentTransaction == null) {
      return;
    }
    processingCollect.adErrorResponse(
        requestId, -1, "Process command failed: " + error.getMessage());
    try {
      processingCollect.sendResponse();
      currentTransaction.rollback();
    } catch (final Exception e) {
      error.addSuppressed(e);
      throw new RuntimeException(e);
    } finally {
      currentTransaction = null;
    }
  }

  @Override
  public void onRecordAvailable() {
    actor.submit(this::processNextEvent);
  }
}
