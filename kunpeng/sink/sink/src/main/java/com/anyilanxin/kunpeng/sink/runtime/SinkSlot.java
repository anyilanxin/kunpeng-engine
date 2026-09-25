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

import com.anyilanxin.kunpeng.protocol.business.impl.RecordMetadata;
import com.anyilanxin.kunpeng.protocol.business.impl.eventlog.BusinessLogRecord;
import com.anyilanxin.kunpeng.repository.business.modules.sink.MutableSinkRepository;
import com.anyilanxin.kunpeng.scheduler.ActorControl;
import com.anyilanxin.kunpeng.sink.SinkLoggers;
import com.anyilanxin.kunpeng.sink.api.RecordSink;
import com.anyilanxin.kunpeng.sink.api.context.CancellableTask;
import com.anyilanxin.kunpeng.sink.api.context.SinkController;
import com.anyilanxin.kunpeng.sink.metrics.SinkMetrics;
import com.anyilanxin.kunpeng.sink.registry.SinkDescriptor;
import com.anyilanxin.kunpeng.structpack.util.BufferUtil;
import com.anyilanxin.kunpeng.utils.jar.ThreadContextUtil;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.time.InstantSource;
import java.util.Optional;
import org.agrona.DirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.slf4j.Logger;

/**
 * 绑定到一个分区的一个已配置 Sink 实例，以及围绕它的全部簿记： 确认位置、持久化状态与软暂停缓冲。
 *
 * <p>该槽位同时也是 Sink 拿到的 {@link SinkController}：通过它提交的确认 一律在 actor 线程上执行，绝不在调用者线程上执行。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@SuppressWarnings("java:S112") // Sink 实现可能抛出任意异常
public final class SinkSlot implements SinkController {

  private static final Logger LOG = SinkLoggers.SINK;
  private static final DirectBuffer NO_METADATA = new UnsafeBuffer(new byte[0]);

  private final RecordSink sink;
  private final SinkRuntimeContext context;
  private final SinkInitInfo initInfo;

  /** 该 Sink 状态已提交到的位置。 */
  private long committedPosition = -1L;

  /** 最后一条交给 Sink 的记录位置，无论是否已确认。 */
  private long lastDeliveredPosition = -1L;

  private boolean softPaused;
  private long pendingPosition = -1L;
  private byte[] pendingMetadata;

  private ActorControl actor;
  private SinkMetrics metrics;
  private MutableSinkRepository state;

  SinkSlot(
      final SinkDescriptor descriptor,
      final int partitionId,
      final SinkInitInfo initInfo,
      final MeterRegistry meterRegistry,
      final InstantSource clock) {
    this.initInfo = initInfo;
    context =
        new SinkRuntimeContext(
            SinkLoggers.forSink(descriptor.getId()),
            descriptor.getConfig(),
            partitionId,
            meterRegistry,
            clock);
    sink = descriptor.newInstance();
  }

  /** 接入运行时协作者；必须先于其它生命周期调用执行。 */
  void init(
      final ActorControl actor,
      final SinkMetrics metrics,
      final MutableSinkRepository state,
      final SinkPhase phase) {
    this.actor = actor;
    this.metrics = metrics;
    this.state = state;
    if (phase == SinkPhase.SOFT_PAUSED) {
      softPause();
    }
  }

  /** 在 Sink 自己的类加载器上下文中执行其 {@code initialize} 回调。 */
  void initializeSink() throws Exception {
    LOG.debug("Initializing sink '{}'", getId());
    ThreadContextUtil.runCheckedWithClassLoader(
        () -> sink.initialize(context), sink.getClass().getClassLoader());
  }

  /** 在 Sink 自己的类加载器上下文中执行其 {@code pause} 回调（引擎级暂停）。 */
  void pauseSink() {
    LOG.debug("Pausing sink '{}'", getId());
    ThreadContextUtil.runWithClassLoader(sink::pause, sink.getClass().getClassLoader());
  }

  /** 在 Sink 自己的类加载器上下文中执行其 {@code resume} 回调（暂停后恢复投递）。 */
  void resumeSink() {
    LOG.debug("Resuming sink '{}'", getId());
    ThreadContextUtil.runWithClassLoader(sink::resume, sink.getClass().getClassLoader());
  }

  /** 依据 {@link SinkInitInfo} 对齐持久化状态：该 Sink 从未见过（或出现更新的初始化规则）时先初始化， 然后把已提交位置加载进内存。 */
  void loadPersistedState() {
    if (initInfo.metadataVersion() > state.getMetadataVersion(getId())) {
      final String inheritFrom = initInfo.inheritStateFrom();
      if (inheritFrom != null) {
        final DirectBuffer inheritedMetadata = state.getSinkMetadata(inheritFrom);
        state.initializeSinkState(
            getId(),
            state.getSinkPosition(inheritFrom),
            inheritedMetadata == null ? NO_METADATA : inheritedMetadata,
            initInfo.metadataVersion());
      } else {
        state.initializeSinkState(getId(), -1L, NO_METADATA, initInfo.metadataVersion());
      }
    }

    final long storedPosition = state.getSinkPosition(getId());
    committedPosition =
        storedPosition == MutableSinkRepository.VALUE_NOT_FOUND ? -1L : storedPosition;
    lastDeliveredPosition = committedPosition;
    if (storedPosition == MutableSinkRepository.VALUE_NOT_FOUND) {
      state.setSinkPosition(getId(), -1L);
    }
  }

  /** 在 Sink 自己的类加载器上下文中执行其 {@code start} 回调。 */
  void startSink() {
    LOG.info("Starting sink '{}'", getId());
    ThreadContextUtil.runWithClassLoader(() -> sink.start(this), sink.getClass().getClassLoader());
  }

  String getId() {
    return context.getConfiguration().getId();
  }

  SinkRuntimeContext getContext() {
    return context;
  }

  long getCommittedPosition() {
    return committedPosition;
  }

  RecordSink getSink() {
    return sink;
  }

  /**
   * 把一条记录交给 Sink ，同时遵循槽位位置与 Sink 的记录匹配器。
   *
   * @param metadata 原始记录元数据，供匹配器使用
   * @param record 记录的类型化视图
   * @return 记录已处理（已投递，或按规则不适用）时返回 true； Sink 抛出异常、需要重试该记录时返回 false
   */
  boolean sinkRecord(final RecordMetadata metadata, final BusinessLogRecord<?> record) {
    final long recordPosition = record.getPosition();
    if (committedPosition >= recordPosition) {
      // 重启前已提交超过此位置；不重复投递
      return true;
    }

    if (!matches(metadata)) {
      skipUpTo(recordPosition);
      return true;
    }

    try {
      ThreadContextUtil.runWithClassLoader(
          () -> sink.sink(record), sink.getClass().getClassLoader());
    } catch (final Exception e) {
      context
          .getLogger()
          .warn(
              "Sink '{}' failed on record at position {}; the record will be retried",
              getId(),
              recordPosition,
              e);
      return false;
    }

    lastDeliveredPosition = recordPosition;
    metrics.recordDeliveredPosition(getId(), recordPosition);
    return true;
  }

  /**
   * 越过一条本 Sink 不需要的记录——但仅当没有更早的记录仍在 Sink 手上时才允许， 否则那条更早的记录失败后将无法重试。
   *
   * @param recordPosition 要越过到达的位置
   */
  void skipUpTo(final long recordPosition) {
    if (committedPosition >= lastDeliveredPosition && committedPosition < recordPosition) {
      commitPosition(recordPosition, null);
    }
  }

  void softPause() {
    softPaused = true;
  }

  void resumeFromSoftPause() {
    softPaused = false;
    if (pendingPosition > committedPosition) {
      commitPosition(pendingPosition, pendingMetadata);
    }
    pendingPosition = -1L;
    pendingMetadata = null;
  }

  private boolean matches(final RecordMetadata metadata) {
    final var matcher = context.getMatcher();
    return matcher.acceptsRecordType(metadata.getRecordType())
        && matcher.acceptsValueType(metadata.getValueType())
        && matcher.acceptsIntent(metadata.getLifeCycle());
  }

  /** 提交位置（单调递增）：软暂停期间只缓冲，否则立即持久化。 */
  void commitPosition(final long position, final byte[] metadata) {
    if (position <= committedPosition) {
      return;
    }

    if (softPaused) {
      pendingPosition = position;
      pendingMetadata = metadata;
      return;
    }

    committedPosition = position;
    state.setSinkState(
        getId(), position, metadata == null ? NO_METADATA : BufferUtil.wrapArray(metadata));
    metrics.recordCommittedPosition(getId(), position);
  }

  // ---------------------------------------------------------------- SinkController 实现

  @Override
  public void updatePosition(final long position) {
    actor.run(() -> commitPosition(position, null));
  }

  @Override
  public void updatePosition(final long position, final byte[] metadata) {
    actor.run(() -> commitPosition(position, metadata));
  }

  @Override
  public long getPosition() {
    return committedPosition;
  }

  @Override
  public CancellableTask scheduleTask(final Duration delay, final Runnable task) {
    final var timer = actor.schedule(delay, task);
    return timer::cancel;
  }

  @Override
  public Optional<byte[]> readMetadata() {
    final DirectBuffer metadata = state.getSinkMetadata(getId());
    if (metadata == null || metadata.capacity() == 0) {
      return Optional.empty();
    }
    return Optional.of(BufferUtil.bufferAsArray(metadata));
  }

  void close() {
    try {
      ThreadContextUtil.runCheckedWithClassLoader(sink::close, sink.getClass().getClassLoader());
    } catch (final Exception e) {
      context.getLogger().error("Sink '{}' failed to close", getId(), e);
    }
    context.close();
  }
}
