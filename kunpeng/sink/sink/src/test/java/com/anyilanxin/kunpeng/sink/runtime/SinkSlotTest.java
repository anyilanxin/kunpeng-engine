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

import static org.assertj.core.api.Assertions.assertThat;

import com.anyilanxin.kunpeng.sink.api.RecordSink;
import com.anyilanxin.kunpeng.sink.api.context.SinkContext;
import com.anyilanxin.kunpeng.sink.api.context.RecordMatcher;
import com.anyilanxin.kunpeng.sink.metrics.SinkMetrics;
import com.anyilanxin.kunpeng.sink.registry.SinkDescriptor;
import com.anyilanxin.kunpeng.sink.registry.SinkFactory;
import com.anyilanxin.kunpeng.protocol.Record;
import com.anyilanxin.kunpeng.protocol.UnifiedRecordValue;
import com.anyilanxin.kunpeng.protocol.ValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.ValueType;
import com.anyilanxin.kunpeng.protocol.impl.RecordMetadata;
import com.anyilanxin.kunpeng.protocol.impl.log.LogRecord;
import com.anyilanxin.kunpeng.protocol.record.RecordType;
import com.anyilanxin.kunpeng.protocol.record.RejectionType;
import com.anyilanxin.kunpeng.protocol.record.command.processinstance.ProcessInstanceLifeCycle;
import com.anyilanxin.kunpeng.repository.ResourceDataSplit.DataVisitor;
import com.anyilanxin.kunpeng.repository.modules.sink.mutable.RepositoryMutableSink;
import com.anyilanxin.kunpeng.repository.modules.sink.record.SinkStateEntry;
import com.anyilanxin.kunpeng.structpack.util.BufferUtil;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.InstantSource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import org.agrona.DirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * sink 槽位测试。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class SinkSlotTest {

  private final InMemorySinkState state = new InMemorySinkState();
  private RecordingSink sink;

  @BeforeEach
  void setUp() {
    sink = new RecordingSink();
  }

  private SinkSlot newSlot(final String id) {
    final var slot =
        new SinkSlot(
            new SinkDescriptor(id, new FixedFactory(id), Map.of()),
            1,
            SinkInitInfo.fresh(),
            new SimpleMeterRegistry(),
            InstantSource.system());
    slot.init(null, new SinkMetrics(new SimpleMeterRegistry()), state, SinkPhase.RUNNING);
    slot.loadPersistedState();
    try {
      slot.initializeSink();
    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
    return slot;
  }

  private LogRecord<UnifiedRecordValue> recordAt(final long position) {
    return new FakeLogRecord(position);
  }

  private RecordMetadata eventMetadata() {
    return new RecordMetadata()
        .recordType(RecordType.EVENT)
        .valueType(ValueType.PROCESS_INSTANCE)
        .valueLifeCycle(ProcessInstanceLifeCycle.values()[0]);
  }

  @Test
  void deliversARecordOnlyUntilItIsCommitted() {
    // 假设
    final var slot = newSlot("recorder");

    // 当
    assertThat(slot.sinkRecord(eventMetadata(), recordAt(1))).isTrue();
    slot.commitPosition(1L, null);

    // 则：已确认的记录不再重复投递
    assertThat(slot.sinkRecord(eventMetadata(), recordAt(1))).isTrue();
    assertThat(sink.receivedPositions).containsExactly(1L);
    assertThat(slot.getPosition()).isEqualTo(1L);
  }

  @Test
  void redeliversWhileNotCommitted() {
    // 假设：槽位从不确认
    final var slot = newSlot("recorder");

    // 当
    slot.sinkRecord(eventMetadata(), recordAt(5));
    slot.sinkRecord(eventMetadata(), recordAt(5));

    // 则
    assertThat(sink.receivedPositions).containsExactly(5L, 5L);
  }

  @Test
  void failureIsReportedForRetry() {
    // 假设
    sink.failOn = 7L;
    final var slot = newSlot("recorder");

    // 当
    final var handled = slot.sinkRecord(eventMetadata(), recordAt(7));

    // 则： Sink 被调用过但失败了，槽位把该记录报告为未处理
    assertThat(handled).isFalse();
    assertThat(sink.calls).isEqualTo(1);
    assertThat(sink.receivedPositions).isEmpty();
  }

  @Test
  void matcherRejectsRecordWithoutDelivery() {
    // 假设
    sink.matcher =
        new RecordMatcher() {
          @Override
          public boolean acceptsRecordType(final RecordType recordType) {
            return false;
          }

          @Override
          public boolean acceptsValueType(final ValueType valueType) {
            return true;
          }
        };
    final var slot = newSlot("recorder");

    // 当
    final var handled = slot.sinkRecord(eventMetadata(), recordAt(3));

    // 则：记录被跳过且位置已越过，之后再也不会投递给它
    assertThat(handled).isTrue();
    assertThat(sink.receivedPositions).isEmpty();
    assertThat(slot.getPosition()).isEqualTo(3L);
    assertThat(state.positions).containsEntry("recorder", 3L);
  }

  @Test
  void skipUpToDoesNotPassAnInFlightRecord() {
    // 假设：记录 2 已投递但未确认
    final var slot = newSlot("recorder");
    slot.sinkRecord(eventMetadata(), recordAt(2));

    // 当
    slot.skipUpTo(5L);

    // 则：若提交到 5，在途的记录 2 将无法重试，因此位置保持不动
    assertThat(slot.getPosition()).isEqualTo(-1L);
  }

  @Test
  void skipUpToAdvancesWhenUpToDate() {
    // 假设
    final var slot = newSlot("recorder");
    state.setSinkPosition("recorder", 4L);
    slot.loadPersistedState();

    // 当
    slot.skipUpTo(9L);

    // 则
    assertThat(slot.getPosition()).isEqualTo(9L);
    assertThat(state.positions).containsEntry("recorder", 9L);
  }

  @Test
  void pauseAndResumeNotifyTheSink() {
    // 假设
    final var slot = newSlot("recorder");
    assertThat(slot.sinkRecord(eventMetadata(), recordAt(1))).isTrue();

    // 当：引擎级暂停后恢复
    slot.pauseSink();
    slot.resumeSink();

    // 则： Sink 各收到一次回调
    assertThat(sink.pauses).isEqualTo(1);
    assertThat(sink.resumes).isEqualTo(1);
  }

  @Test
  void softPauseBuffersCommitsAndFlushesOnResume() {
    // 假设
    final var slot = newSlot("recorder");
    slot.softPause();
    assertThat(slot.sinkRecord(eventMetadata(), recordAt(1))).isTrue();

    // 当：软暂停期间 Sink 提交确认
    slot.commitPosition(1L, new byte[] {9});

    // 则：尚未提交任何内容（只有初始的 -1 占位）
    assertThat(slot.getPosition()).isEqualTo(-1L);
    assertThat(state.positions).containsEntry("recorder", -1L);

    // 并当
    slot.resumeFromSoftPause();

    // 则：缓冲的确认被写回
    assertThat(slot.getPosition()).isEqualTo(1L);
    assertThat(state.positions).containsEntry("recorder", 1L);
    assertThat(BufferUtil.bufferAsArray(state.metadata.get("recorder"))).containsExactly(9);
  }

  @Test
  void initInheritsStateFromAnotherSink() {
    // 假设
    state.setSinkState("old", 42L, new UnsafeBuffer(new byte[] {7}));

    // 当
    final var slot =
        new SinkSlot(
            new SinkDescriptor("renamed", new FixedFactory("renamed"), Map.of()),
            1,
            SinkInitInfo.inheritFrom("old"),
            new SimpleMeterRegistry(),
            InstantSource.system());
    slot.init(null, new SinkMetrics(new SimpleMeterRegistry()), state, SinkPhase.RUNNING);
    slot.loadPersistedState();

    // 则
    assertThat(slot.getPosition()).isEqualTo(42L);
    assertThat(state.positions).containsEntry("renamed", 42L);
    assertThat(BufferUtil.bufferAsArray(state.metadata.get("renamed"))).containsExactly(7);
  }

  // ------------------------------------------------------------------ 测试夹具

  /** 返回本测试的记录器实例的工厂，让断言能看到实际投递的内容。 */
  final class FixedFactory implements SinkFactory {
    private final String id;

    FixedFactory(final String id) {
      this.id = id;
    }

    @Override
    public String sinkId() {
      return id;
    }

    @Override
    public RecordSink newInstance() {
      return sink;
    }

    @Override
    public boolean producesSameType(final SinkFactory other) {
      return other instanceof FixedFactory;
    }
  }

  /** 记录收到的内容的 Sink ；可指定在某个位置上失败，并统计 pause/resume 回调次数。 */
  public static final class RecordingSink implements RecordSink {
    final List<Long> receivedPositions = new ArrayList<>();
    int calls;
    int pauses;
    int resumes;
    Long failOn;
    RecordMatcher matcher;

    @Override
    public void initialize(final SinkContext context) {
      if (matcher != null) {
        context.setRecordMatcher(matcher);
      }
    }

    @Override
    public void pause() {
      pauses++;
    }

    @Override
    public void resume() {
      resumes++;
    }

    @Override
    public void sink(final Record<?> record) {
      calls++;
      if (failOn != null && failOn == record.getPosition()) {
        throw new IllegalStateException("planned failure at " + failOn);
      }
      receivedPositions.add(record.getPosition());
    }
  }

  /** 持久化 Sink 状态的内存替身。 */
  public static final class InMemorySinkState implements RepositoryMutableSink {
    final Map<String, Long> positions = new HashMap<>();
    final Map<String, DirectBuffer> metadata = new HashMap<>();
    final Map<String, Long> metadataVersions = new HashMap<>();

    @Override
    public void setSinkPosition(final String sinkId, final long position) {
      positions.put(sinkId, position);
    }

    @Override
    public void removeSinkState(final String sinkId) {
      positions.remove(sinkId);
      metadata.remove(sinkId);
      metadataVersions.remove(sinkId);
    }

    @Override
    public void setSinkState(
        final String sinkId, final long position, final DirectBuffer metadataBuffer) {
      positions.put(sinkId, position);
      metadata.put(sinkId, metadataBuffer == null ? new UnsafeBuffer() : metadataBuffer);
    }

    @Override
    public void initializeSinkState(
        final String sinkId,
        final long position,
        final DirectBuffer metadataBuffer,
        final long metadataVersion) {
      setSinkState(sinkId, position, metadataBuffer);
      metadataVersions.put(sinkId, metadataVersion);
    }

    @Override
    public long getSinkPosition(final String sinkId) {
      return positions.getOrDefault(sinkId, VALUE_NOT_FOUND);
    }

    @Override
    public DirectBuffer getSinkMetadata(final String sinkId) {
      return metadata.get(sinkId);
    }

    @Override
    public long getLowestPosition() {
      return positions.values().stream().min(Long::compare).orElse(VALUE_NOT_FOUND);
    }

    @Override
    public long getMetadataVersion(final String sinkId) {
      return metadataVersions.getOrDefault(sinkId, VALUE_NOT_FOUND);
    }

    @Override
    public void visitSinkState(final BiConsumer<String, SinkStateEntry> consumer) {
      positions.forEach((id, position) -> consumer.accept(id, new SinkStateEntry().setPosition(position)));
    }

    @Override
    public boolean hasSinks() {
      return !positions.isEmpty();
    }

    @Override
    public void splitLocalFamilyData(final int resourceId, final DataVisitor visitor) {
      // 内存替身无需拆分持久化数据
    }
  }

  /** 只承载槽位实际会读取的字段的 BusinessLogRecord。 */
  static final class FakeLogRecord implements LogRecord<UnifiedRecordValue> {
    private final long position;

    FakeLogRecord(final long position) {
      this.position = position;
    }

    @Override
    public long getPosition() {
      return position;
    }

    @Override
    public long getSourceRecordPosition() {
      return -1L;
    }

    @Override
    public long getKey() {
      return 1L;
    }

    @Override
    public long getTimestamp() {
      return 0L;
    }

    @Override
    public ValueLifeCycle getValueState() {
      return ProcessInstanceLifeCycle.values()[0];
    }

    @Override
    public int getResourceId() {
      return 1;
    }

    @Override
    public RecordType getRecordType() {
      return RecordType.EVENT;
    }

    @Override
    public RejectionType getRejectionType() {
      return RejectionType.NULL_VAL;
    }

    @Override
    public String getRejectionReason() {
      return "";
    }

    @Override
    public String getBrokerVersion() {
      return "0.0.0";
    }

    @Override
    public Map<String, Object> getAuthorizations() {
      return Map.of();
    }

    @Override
    public int getRecordVersion() {
      return 1;
    }

    @Override
    public ValueType getValueType() {
      return ValueType.PROCESS_INSTANCE;
    }

    @Override
    public UnifiedRecordValue getValue() {
      return null;
    }

    @Override
    public long getOperationReferenceKey() {
      return -1L;
    }

    @Override
    public long getBatchOperationReferenceKey() {
      return -1L;
    }

    @Override
    public RecordMetadata getMetadata() {
      return new RecordMetadata();
    }

    @Override
    public int getRequestStreamId() {
      return -1;
    }

    @Override
    public long getRequestId() {
      return -1L;
    }

    @Override
    public int getLength() {
      return 0;
    }
  }
}
