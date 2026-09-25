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
package com.anyilanxin.kunpeng.sink.runtime;

import com.anyilanxin.kunpeng.cluster.config.messaging.PartitionMessagingService;
import com.anyilanxin.kunpeng.cluster.utils.health.FailureListener;
import com.anyilanxin.kunpeng.cluster.utils.health.HealthMonitorable;
import com.anyilanxin.kunpeng.cluster.utils.health.HealthReport;
import com.anyilanxin.kunpeng.eventlog.EntryFilter;
import com.anyilanxin.kunpeng.eventlog.EventLog;
import com.anyilanxin.kunpeng.eventlog.EventLogReader;
import com.anyilanxin.kunpeng.eventlog.LoggedEntry;
import com.anyilanxin.kunpeng.eventlog.RecordAvailableListener;
import com.anyilanxin.kunpeng.protocol.business.ValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.ValueType;
import com.anyilanxin.kunpeng.protocol.business.impl.RecordMetadata;
import com.anyilanxin.kunpeng.protocol.business.record.RecordType;
import com.anyilanxin.kunpeng.repository.business.modules.sink.MutableSinkRepository;
import com.anyilanxin.kunpeng.scheduler.Actor;
import com.anyilanxin.kunpeng.scheduler.ActorSchedulingService;
import com.anyilanxin.kunpeng.scheduler.ScheduledTimer;
import com.anyilanxin.kunpeng.scheduler.SchedulingHints;
import com.anyilanxin.kunpeng.scheduler.future.ActorFuture;
import com.anyilanxin.kunpeng.scheduler.future.CompletableActorFuture;
import com.anyilanxin.kunpeng.scheduler.retry.BackOffRetryStrategy;
import com.anyilanxin.kunpeng.sink.SinkLoggers;
import com.anyilanxin.kunpeng.sink.api.context.RecordMatcher;
import com.anyilanxin.kunpeng.sink.metrics.SinkMetrics;
import com.anyilanxin.kunpeng.sink.protocol.SinkPositionsMessage;
import com.anyilanxin.kunpeng.sink.registry.SinkDescriptor;
import com.anyilanxin.kunpeng.utils.exception.UnrecoverableException;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.time.InstantSource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.agrona.LangUtil;
import org.slf4j.Logger;

/**
 * 分区级服务：把日志记录喂给所有已配置的 Sink 。
 *
 * <p>在分区 leader 上，它按顺序读取事件日志并把每条记录分发给每个 Sink ； 失败的记录按退避重试直到成功——因此缓慢或损坏的 Sink 不会悄悄丢数据，而是拖住日志压缩。
 * 确认位置会被持久化并周期性广播；在 follower 上，服务只记录广播来的位置， 这样 leader 切换后新 leader 能精确地从旧 leader 停下的位置继续。
 *
 * <p>记录投递在 actor 线程上按批执行：连续记录之间不再重复入队， 每处理 {@link #DRAIN_BATCH_SIZE}
 * 条后让出线程，保证控制指令（暂停、恢复、启用、禁用）能及时得到处理。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class SinkService extends Actor implements HealthMonitorable, RecordAvailableListener {

  private static final Logger LOG = SinkLoggers.SINK;

  /** 单次 actor 任务内连续处理的最大记录数，之后让出线程。 */
  private static final int DRAIN_BATCH_SIZE = 512;

  private static final Duration START_RETRY_BACKOFF = Duration.ofSeconds(10);
  private static final String POSITION_TOPIC_PREFIX = "sink-positions-";

  private final EventLog eventLog;
  private final int partitionId;
  private final String actorName;
  private final MeterRegistry meterRegistry;
  private final SinkMetrics metrics;
  private final RecordDispatcher dispatcher;
  private final List<SinkSlot> slots;
  private final MutableSinkRepository state;
  private final SinkRole role;
  private final Duration broadcastInterval;
  private final EntryFilter positionsToSkip;
  private final PartitionMessagingService messaging;
  private final String positionTopic;
  private final InstantSource clock;
  private final AtomicBoolean started = new AtomicBoolean(false);
  private final Set<FailureListener> failureListeners = new HashSet<>();
  private final BackOffRetryStrategy dispatchRetry;

  private EventLogReader reader;
  private EntryFilter entryFilter;
  private final RecordMetadata skipMetadata = new RecordMetadata();
  private PositionBroadcaster broadcaster;
  private ScheduledTimer broadcastTimer;
  private SinkPhase phase;

  @SuppressWarnings("java:S3077") // 健康报告一经发布即不可变
  private volatile HealthReport healthReport;

  /** 服务没有任何 Sink 时置位；actor 随之停摆，且不持有日志读取器。 */
  private boolean idle;

  public SinkService(final SinkServiceContext context, final SinkPhase initialPhase) {
    actorName = Objects.requireNonNull(context.getActorName(), "actor name must not be null");
    eventLog = Objects.requireNonNull(context.getEventLog(), "event log must not be null");
    partitionId = eventLog.getPartitionId();
    final MeterRegistry meterRegistry =
        Objects.requireNonNull(context.getMeterRegistry(), "meter registry must not be null");
    this.meterRegistry = meterRegistry;
    clock = context.getClock();
    role = context.getRole();
    broadcastInterval = context.getBroadcastInterval();
    positionsToSkip = context.getPositionsToSkip();
    messaging = context.getMessaging();
    positionTopic = POSITION_TOPIC_PREFIX + partitionId;

    slots =
        context.getSinks().entrySet().stream()
            .map(
                entry ->
                    new SinkSlot(
                        entry.getKey(), partitionId, entry.getValue(), meterRegistry, clock))
            .collect(Collectors.toCollection(ArrayList::new));
    metrics = new SinkMetrics(meterRegistry);
    metrics.initializePhase(initialPhase);
    dispatcher = new RecordDispatcher(metrics, slots, partitionId, clock);
    dispatchRetry = new BackOffRetryStrategy(actor, START_RETRY_BACKOFF);
    state = context.getRepository().sinkRepository();
    phase = initialPhase;

    healthReport = HealthReport.healthy(this);
  }

  // ------------------------------------------------------------------ 生命周期

  /** 把本服务交给调度器；服务随后异步启动。 */
  public ActorFuture<Void> startAsync(final ActorSchedulingService scheduler) {
    return scheduler.submitActor(this, SchedulingHints.IO_BOUND);
  }

  /** 停止本服务；可重复调用。 */
  public ActorFuture<Void> stopAsync() {
    return actor.close();
  }

  @Override
  public String getName() {
    return actorName;
  }

  @Override
  protected Map<String, String> createContext() {
    final var context = super.createContext();
    context.put("partitionId", Integer.toString(partitionId));
    return context;
  }

  @Override
  protected void onActorStarting() {
    if (role == SinkRole.LEADER) {
      reader = eventLog.newReader();
    }
  }

  @Override
  protected void onActorStarted() {
    try {
      broadcaster = new PositionBroadcaster(this::applyBroadcastPosition, messaging, positionTopic);
      initSlots();
      dropStateOfRemovedSinks();
    } catch (final Exception e) {
      failService(e);
      LangUtil.rethrowUnchecked(e);
    }

    started.set(true);
    if (role == SinkRole.LEADER) {
      startLeaderMode();
    } else {
      startFollowerMode();
    }
  }

  @Override
  protected void onActorCloseRequested() {
    started.set(false);
    slots.forEach(SinkSlot::close);
    if (broadcaster != null) {
      broadcaster.close();
    }
  }

  @Override
  protected void onActorClosing() {
    if (reader != null) {
      reader.close();
      reader = null;
    }
    eventLog.removeRecordAvailableListener(this);
  }

  @Override
  protected void onActorClosed() {
    LOG.debug("Sink service '{}' closed", actorName);
    phase = SinkPhase.CLOSED;
  }

  @Override
  protected void handleFailure(final Throwable failure) {
    LOG.error(
        "Sink service '{}' failed in phase {}: {}",
        actorName,
        actor.getLifecyclePhase(),
        failure,
        failure);
    actor.fail(failure);
    publishFailure(failure);
  }

  // ------------------------------------------------------------------ 控制 API

  /** 停止读取记录，直到 {@link #resume()}；同时通知每个 Sink {@code pause}。 */
  public ActorFuture<Void> pause() {
    if (actor.isClosed()) {
      return CompletableActorFuture.completed(null);
    }
    return actor.call(
        () -> {
          phase = SinkPhase.PAUSED;
          metrics.setPhase(phase);
          slots.forEach(SinkSlot::pauseSink);
        });
  }

  /** 继续投递记录，但暂缓位置提交，直到恢复；适用于短暂维护窗口—— 与其让 Sink 落后太多，不如位置稍微滞后。 */
  public ActorFuture<Void> softPause() {
    if (actor.isClosed()) {
      return CompletableActorFuture.completed(null);
    }
    return actor.call(
        () -> {
          slots.forEach(SinkSlot::softPause);
          phase = SinkPhase.SOFT_PAUSED;
          metrics.setPhase(phase);
        });
  }

  /** 从任一暂停状态恢复；软暂停缓冲的位置将被写回，硬暂停的 Sink 收到 {@code resume}。 */
  public ActorFuture<Void> resume() {
    if (actor.isClosed()) {
      return CompletableActorFuture.completed(null);
    }
    return actor.call(
        () -> {
          final var wasHardPaused = phase == SinkPhase.PAUSED;
          if (phase == SinkPhase.SOFT_PAUSED) {
            slots.forEach(SinkSlot::resumeFromSoftPause);
          }
          phase = SinkPhase.RUNNING;
          metrics.setPhase(phase);
          if (wasHardPaused) {
            slots.forEach(SinkSlot::resumeSink);
          }
          if (role == SinkRole.LEADER && !idle) {
            actor.submit(this::drain);
          }
        });
  }

  /** 运行时移除一个 Sink ；其持久化位置一并删除。 */
  public ActorFuture<Void> disableSink(final String sinkId) {
    if (actor.isClosed()) {
      return CompletableActorFuture.completed(null);
    }
    return actor.call(() -> removeSlot(sinkId));
  }

  /** 运行时添加一个 Sink ；打开或配置失败时 future 以异常结束。 */
  public ActorFuture<Void> enableSink(
      final String sinkId, final SinkInitInfo initInfo, final SinkDescriptor descriptor) {
    if (actor.isClosed()) {
      return CompletableActorFuture.completed(null);
    }
    return actor.call(
        () -> {
          addSlot(sinkId, initInfo, descriptor);
          return null;
        });
  }

  /**
   * 与 {@link #enableSink(String, SinkInitInfo, SinkDescriptor)} 相同， 但失败时按退避重试直到服务关闭——适用于目标系统可能尚未就绪的
   * Sink 。
   */
  public ActorFuture<Boolean> enableSinkWithRetry(
      final String sinkId, final SinkInitInfo initInfo, final SinkDescriptor descriptor) {
    return new BackOffRetryStrategy(actor, START_RETRY_BACKOFF)
        .runWithRetry(
            () -> {
              try {
                addSlot(sinkId, initInfo, descriptor);
                return true;
              } catch (final Exception e) {
                LOG.error("Failed to enable sink '{}'; retrying", sinkId, e);
                return false;
              }
            },
            this::isServiceClosed);
  }

  /**
   * @return 本服务当前所处阶段
   */
  public ActorFuture<SinkPhase> getPhase() {
    if (actor.isClosed()) {
      return CompletableActorFuture.completed(SinkPhase.CLOSED);
    }
    return actor.call(() -> phase);
  }

  /**
   * @return 全部 Sink 中最低的已提交位置；日志压缩以其为门槛
   */
  public ActorFuture<Long> getLowestPosition() {
    if (actor.isClosed()) {
      return CompletableActorFuture.completed(MutableSinkRepository.VALUE_NOT_FOUND);
    }
    return actor.call(state::getLowestPosition);
  }

  // ------------------------------------------------------------------ 健康

  @Override
  public String componentName() {
    return actorName;
  }

  @Override
  public HealthReport getHealthReport() {
    return healthReport;
  }

  @Override
  public void addFailureListener(final FailureListener listener) {
    actor.run(() -> failureListeners.add(listener));
  }

  @Override
  public void removeFailureListener(final FailureListener listener) {
    actor.run(() -> failureListeners.remove(listener));
  }

  // ------------------------------------------------------------------ 记录流转

  @Override
  public void onRecordAvailable() {
    actor.run(this::drain);
  }

  private void initSlots() throws Exception {
    for (final var slot : slots) {
      slot.init(actor, metrics, state, phase);
      slot.initializeSink();
    }
    entryFilter =
        positionsToSkip == null ? combinedMatcher() : positionsToSkip.and(combinedMatcher());
    LOG.debug("Installed entry filter for partition {}: {}", partitionId, entryFilter);
  }

  /** 至少有一个已安装匹配器想要该条目时才放行的过滤器。 */
  private EntryFilter combinedMatcher() {
    if (slots.isEmpty()) {
      return entry -> false;
    }

    final List<RecordMatcher> matchers =
        slots.stream().map(slot -> slot.getContext().getMatcher()).toList();

    final Map<RecordType, Boolean> byRecordType =
        Arrays.stream(RecordType.values())
            .collect(
                Collectors.toMap(
                    Function.identity(),
                    type -> matchers.stream().anyMatch(m -> m.acceptsRecordType(type))));
    final Map<ValueType, Boolean> byValueType =
        Arrays.stream(ValueType.values())
            .collect(
                Collectors.toMap(
                    Function.identity(),
                    type -> matchers.stream().anyMatch(m -> m.acceptsValueType(type))));
    final Map<ValueLifeCycle, Boolean> byIntent =
        ValueLifeCycle.INTENT_CLASSES.stream()
            .flatMap(clazz -> Arrays.stream(clazz.getEnumConstants()))
            .collect(
                Collectors.toMap(
                    Function.identity(),
                    intent -> matchers.stream().anyMatch(m -> m.acceptsIntent(intent))));

    return new EntryFilter() {
      private final RecordMetadata metadata = new RecordMetadata();

      @Override
      public boolean applies(final LoggedEntry entry) {
        entry.readMetadata(metadata);
        return byRecordType.getOrDefault(metadata.getRecordType(), false)
            && byValueType.getOrDefault(metadata.getValueType(), false)
            && byIntent.getOrDefault(metadata.getLifeCycle(), false);
      }
    };
  }

  private void startLeaderMode() {
    final var startFutures = new ArrayList<ActorFuture<Boolean>>();
    for (final var slot : slots) {
      slot.loadPersistedState();
      startFutures.add(startSlotWithRetry(slot));
    }

    actor.runOnCompletion(
        startFutures,
        error -> {
          if (state.hasSinks()) {
            startFrom(state.getLowestPosition());
          } else {
            goIdle();
          }
        });
  }

  private ActorFuture<Boolean> startSlotWithRetry(final SinkSlot slot) {
    return new BackOffRetryStrategy(actor, START_RETRY_BACKOFF)
        .runWithRetry(
            () -> {
              try {
                slot.startSink();
                return true;
              } catch (final Exception e) {
                LOG.warn("Failed to start sink '{}'; retrying", slot.getId());
                LOG.debug("Start failure of sink '{}'", slot.getId(), e);
                return false;
              }
            },
            this::isServiceClosed);
  }

  private void startFrom(final long position) {
    if (!reader.seekToNextEntry(position)) {
      throw new IllegalStateException(
          "Expected to find the entry at position %d in the log of partition %d to resume processing, but the log has no such entry"
              .formatted(position, partitionId));
    }

    eventLog.registerRecordAvailableListener(this);
    if (phase != SinkPhase.PAUSED) {
      actor.submit(this::drain);
    }
    broadcastTimer = actor.runAtFixedRate(broadcastInterval, this::broadcastPositions);
  }

  private void restartLeaderMode() {
    reader = eventLog.newReader();
    startFrom(MutableSinkRepository.VALUE_NOT_FOUND);
  }

  private void startFollowerMode() {
    for (final var slot : slots) {
      slot.loadPersistedState();
    }
    if (state.hasSinks()) {
      broadcaster.subscribe(actor::run);
    } else {
      goIdle();
    }
  }

  private void restartFollowerMode() {
    broadcaster.subscribe(actor::run);
  }

  private void broadcastPositions() {
    final var message = new SinkPositionsMessage();
    state.visitSinkState(
        (sinkId, entry) -> message.put(sinkId, entry.getPosition(), entry.getMetadata()));
    broadcaster.broadcast(message);
  }

  private void applyBroadcastPosition(
      final String sinkId, final SinkPositionsMessage.SinkPosition position) {
    if (state.getSinkPosition(sinkId) < position.position()) {
      state.setSinkState(sinkId, position.position(), position.metadata());
    }
  }

  /** 持续投递记录，直到日志读尽、某条记录需要重试，或批次达到上限而让出线程。 */
  private void drain() {
    if (!canRead()) {
      return;
    }

    int batch = 0;
    while (canRead() && reader.hasNext()) {
      final LoggedEntry entry = reader.next();
      if (entryFilter.applies(entry)) {
        if (!dispatchEntry(entry)) {
          // 控制权已交出去：要么重试回调已排定（由它继续 drain），要么服务已失败（无需继续）
          return;
        }
      } else {
        skipEntry(entry);
      }

      if (++batch >= DRAIN_BATCH_SIZE) {
        actor.submit(this::drain);
        return;
      }
    }
  }

  private boolean canRead() {
    return started.get() && !idle && phase != SinkPhase.PAUSED && reader != null;
  }

  /**
   * 把一条条目交给所有 Sink 。
   *
   * @return 记录已分发、drain 可以继续时返回 true；控制权交给重试回调或服务失败时返回 false
   */
  private boolean dispatchEntry(final LoggedEntry entry) {
    try {
      dispatcher.wrap(entry);
    } catch (final Exception e) {
      LOG.warn("Failed to decode record from entry {}; aborting processing", entry, e);
      failService(new UnrecoverableException(e));
      return false;
    }

    final var dispatched = dispatchRetry.runWithRetry(dispatcher::dispatch, this::isServiceClosed);
    if (!dispatched.isDone()) {
      actor.runOnCompletion(
          dispatched,
          (ignored, error) -> {
            if (error != null) {
              LOG.error("Processing of record from entry {} aborted", entry, error);
              failService(error);
            } else {
              metrics.recordDelivered(dispatcher.getValueType());
              drain();
            }
          });
      return false;
    }

    if (dispatched.isCompletedExceptionally()) {
      failService(dispatched.getException());
      return false;
    }
    metrics.recordDelivered(dispatcher.getValueType());
    return true;
  }

  private void skipEntry(final LoggedEntry entry) {
    entry.readMetadata(skipMetadata);
    metrics.recordSkipped(skipMetadata.getValueType());

    final long position = entry.getPosition();
    for (final var slot : slots) {
      slot.skipUpTo(position);
    }
  }

  private void removeSlot(final String sinkId) {
    final var slot = slots.stream().filter(s -> s.getId().equals(sinkId)).findFirst().orElse(null);
    if (slot == null) {
      LOG.debug("Sink '{}' is not installed here; nothing to disable", sinkId);
      return;
    }

    slot.close();
    slots.remove(slot);
    state.removeSinkState(sinkId);
    // drain 进行到一半时槽位列表变了：强制下一条记录重新走一轮完整分发
    dispatcher.resetResumePoint();
    LOG.debug("Sink '{}' disabled", sinkId);

    if (slots.isEmpty()) {
      goIdle();
    }
  }

  private void addSlot(
      final String sinkId, final SinkInitInfo initInfo, final SinkDescriptor descriptor) {
    final var alreadyInstalled = slots.stream().map(SinkSlot::getId).anyMatch(sinkId::equals);
    if (alreadyInstalled) {
      LOG.debug("Sink '{}' is already installed; skipping enable", sinkId);
      return;
    }

    final var slot = new SinkSlot(descriptor, partitionId, initInfo, meterRegistry, clock);
    slot.init(actor, metrics, state, phase);
    try {
      slot.initializeSink();
    } catch (final Exception e) {
      LOG.error("Failed to initialize sink '{}'", sinkId, e);
      LangUtil.rethrowUnchecked(e);
    }
    slot.loadPersistedState();
    if (role == SinkRole.LEADER) {
      slot.startSink();
    }
    slots.add(slot);
    dispatcher.resetResumePoint();
    LOG.debug("Sink '{}' enabled", sinkId);

    if (idle) {
      wakeUp();
    }
  }

  private void dropStateOfRemovedSinks() {
    final var installedIds = slots.stream().map(SinkSlot::getId).collect(Collectors.toSet());
    state.visitSinkState(
        (sinkId, entry) -> {
          if (!installedIds.contains(sinkId)) {
            state.removeSinkState(sinkId);
            LOG.info(
                "Sink '{}' is no longer configured; its persisted position was removed", sinkId);
          }
        });
  }

  private void goIdle() {
    idle = true;
    LOG.debug("No sinks configured; sink service of partition {} goes idle", partitionId);
    eventLog.removeRecordAvailableListener(this);
    if (broadcaster != null) {
      broadcaster.close();
    }
    if (broadcastTimer != null) {
      broadcastTimer.cancel();
      broadcastTimer = null;
    }
    if (reader != null) {
      // 读取器不关掉会一直阻止日志段删除
      reader.close();
      reader = null;
    }
  }

  private void wakeUp() {
    LOG.debug("Sinks configured again; sink service of partition {} resumes", partitionId);
    if (role == SinkRole.LEADER) {
      restartLeaderMode();
    } else {
      restartFollowerMode();
    }
    idle = false;
  }

  private boolean isServiceClosed() {
    return !started.get();
  }

  private void failService(final Throwable failure) {
    started.set(false);
    actor.close();
    publishFailure(failure);
  }

  private void publishFailure(final Throwable failure) {
    final HealthReport report =
        failure instanceof UnrecoverableException
            ? HealthReport.dead(this).withIssue(failure, clock.instant())
            : HealthReport.unhealthy(this).withIssue(failure, clock.instant());
    healthReport = report;
    for (final var listener : failureListeners) {
      if (failure instanceof UnrecoverableException) {
        listener.onUnrecoverableFailure(report);
      } else {
        listener.onFailure(report);
      }
    }
  }
}
