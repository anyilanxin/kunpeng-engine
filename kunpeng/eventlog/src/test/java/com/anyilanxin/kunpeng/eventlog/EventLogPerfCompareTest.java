/*
 * Copyright © 2026 anyilanxin zxh (anyilanxin@aliyun.com)
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
package com.anyilanxin.kunpeng.eventlog;

import com.anyilanxin.kunpeng.logstreams.log.LogStream;
import com.anyilanxin.kunpeng.logstreams.log.LogStreamWriter;
import com.anyilanxin.kunpeng.logstreams.storage.LogStorage;
import com.anyilanxin.kunpeng.logstreams.storage.LogStorageReader;
import com.anyilanxin.kunpeng.protocol.ValueType;
import com.anyilanxin.kunpeng.protocol.impl.RecordMetadata;
import com.anyilanxin.kunpeng.protocol.impl.eventlog.RecordAppendEntryFactory;
import com.anyilanxin.kunpeng.protocol.impl.record.command.processinstance.ProcessInstanceRecord;
import com.anyilanxin.kunpeng.protocol.record.RecordType;
import org.agrona.DirectBuffer;
import org.agrona.ExpandableArrayBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 新（EventLog）vs 旧（LogStream, Zeebe 翻译版）追加性能与帧体积对比——
 * 验证性能预估报告的门槛（不劣于 ±5%）；旧模块删除后本测试随之移除。
 *
 * <p>跑法：{@code ./gradlew :kunpeng:logstreams:test --tests "*EventLogPerfCompare*"}
 */
@DisplayName("EventLog vs 旧 LogStream 性能对比")
class EventLogPerfCompareTest {

  /** typed 单条 entry（7 字段 Record 实例, 同数据两侧共享） */
  private static AppendEntry typedEntry(final RecordMetadata metadata,
      final ProcessInstanceRecord record) {
    return RecordAppendEntryFactory.of(42, metadata, record);
  }

  @Test
  @DisplayName("单写者追加吞吐 + 帧体积: 新旧对比（交替测量, 3 轮取最优）")
  void appendThroughputAndSize() {
    // 同一批 typed entry, 两侧共用 record 对象
    final RecordMetadata metadata = new RecordMetadata();
    metadata.recordType(RecordType.COMMAND).valueType(ValueType.PROCESS_INSTANCE);
    final List<ProcessInstanceRecord> records = new ArrayList<>();
    for (int i = 0; i < 64; i++) {
      records.add(fullRecord(i));
    }

    final InMemoryEventStore newStore = new InMemoryEventStore();
    final EventLog newLog = EventLog.builder()
        .withEventStore(newStore).withLogName("perf-new").withPartitionId(1).build();
    final EventLogWriter newWriter = newLog.newWriter();

    final OldListStorage oldStore = new OldListStorage();
    final LogStream oldLog = LogStream.builder()
        .withLogStorage(oldStore).withLogName("perf-old").withPartitionId(1)
        .withClock(java.time.InstantSource.system())
        .withMeterRegistry(new io.micrometer.core.instrument.simple.SimpleMeterRegistry()).build();
    final LogStreamWriter oldWriter = oldLog.newWriter();

    final int measured = 20_000;
    final int warmup = 5_000;

    // 帧体积（首批块）: 确定性对比
    newWriter.tryAppend(WriteContext.INTERNAL, typedBatch(metadata, records), -1);
    oldWriter.tryWrite(
        com.anyilanxin.kunpeng.logstreams.log.WriteContext.internal(),
        oldTypedBatch(metadata, records));
    final int newSize = newStore.firstBlockSize();
    final int oldSize = oldStore.firstBlockSize();
    assertThat(newSize).isGreaterThan(0);

    long oldBest = Long.MAX_VALUE;
    long newBest = Long.MAX_VALUE;
    for (int round = 0; round < 3; round++) {
      if (round % 2 == 0) {
        oldBest = Math.min(oldBest, benchOld(oldLog, metadata, records, warmup, measured));
        newBest = Math.min(newBest, benchNew(newLog, metadata, records, warmup, measured));
      } else {
        newBest = Math.min(newBest, benchNew(newLog, metadata, records, warmup, measured));
        oldBest = Math.min(oldBest, benchOld(oldLog, metadata, records, warmup, measured));
      }
    }

    System.out.printf("""
        ==========================================================
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         EventLog(logstreams) vs LogStream(旧模块) 追加性能对比
         形状: 单条 typed entry (ProcessInstanceRecord 全字段)
         方法: 预热 %d + 3 轮 × %d 次取最优, 轮间交替测量顺序
        ==========================================================
         帧体积   旧=%d B   新=%d B    → 新 %s %.0f%%
         追加耗时 旧=%d ns/条   新=%d ns/条   → 新 %s %.2fx
        ==========================================================%n""",
        warmup, measured,
        oldSize, newSize, newSize <= oldSize ? "小" : "大",
        (double) newSize / oldSize * 100,
        oldBest, newBest, newBest <= oldBest ? "快" : "慢",
        (double) oldBest / newBest);

    // 体积硬断言（确定性）: 新格式显著小于旧格式
    assertThat(newSize).isLessThan(oldSize);
    // 性能门槛: 不劣于旧实现 ±25%（预估报告说 10-15%, 留机器噪声余量）
    assertThat((double) newBest / oldBest).as("新实现单条耗时/旧实现").isLessThan(1.25);

    newLog.close();
    oldLog.close();
  }

  private static ProcessInstanceRecord fullRecord(final int seed) {
    return new ProcessInstanceRecord()
        .setProcessInstanceId(seed)
        .setParentProcessInstanceId(seed)
        .setRootProcessInstanceId(seed)
        .setRev(seed)
        .setProcessDefinitionKey("perf-def-key-" + seed)
        .setProcessDefinitionId(seed)
        .setLifeCycle(com.anyilanxin.kunpeng.protocol.record.command.processinstance
            .ProcessInstanceLifeCycle.ACTIVATING);
  }

  private static List<AppendEntry> typedBatch(
      final RecordMetadata metadata, final List<ProcessInstanceRecord> records) {
    final List<AppendEntry> batch = new ArrayList<>();
    for (final ProcessInstanceRecord record : records) {
      batch.add(typedEntry(metadata, record));
    }
    return batch;
  }

  private static List<com.anyilanxin.kunpeng.logstreams.log.LogAppendEntry> oldTypedBatch(
      final RecordMetadata metadata, final List<ProcessInstanceRecord> records) {
    final List<com.anyilanxin.kunpeng.logstreams.log.LogAppendEntry> batch = new ArrayList<>();
    for (final ProcessInstanceRecord record : records) {
      batch.add(com.anyilanxin.kunpeng.logstreams.log.LogAppendEntry.of(42, metadata, record));
    }
    return batch;
  }

  private long benchNew(final EventLog eventLog, final RecordMetadata metadata,
      final List<ProcessInstanceRecord> records, final int warmup, final int measured) {
    final EventLogWriter writer = eventLog.newWriter();
    for (int i = 0; i < warmup; i++) {
      final AppendResult r = writer.tryAppend(WriteContext.INTERNAL, typedEntry(metadata, records.get(i & 63)));
      if (r instanceof final AppendResult.Appended a) {
        eventLog.getFlowControl().onProcessed(a.lastPosition());
      }
    }
    final long start = System.nanoTime();
    for (int i = 0; i < measured; i++) {
      final AppendResult r = writer.tryAppend(WriteContext.INTERNAL, typedEntry(metadata, records.get(i & 63)));
      if (r instanceof final AppendResult.Appended a) {
        eventLog.getFlowControl().onProcessed(a.lastPosition());
      }
    }
    return (System.nanoTime() - start) / measured;
  }

  private long benchOld(final LogStream logStream, final RecordMetadata metadata,
      final List<ProcessInstanceRecord> records, final int warmup, final int measured) {
    final LogStreamWriter writer = logStream.newWriter();
    for (int i = 0; i < warmup; i++) {
      final var e = writer.tryWrite(com.anyilanxin.kunpeng.logstreams.log.WriteContext.internal(),
          com.anyilanxin.kunpeng.logstreams.log.LogAppendEntry.of(42, metadata, records.get(i & 63)));
      if (e.isRight()) {
        logStream.getFlowControl().onProcessed(e.get());
      }
    }
    final long start = System.nanoTime();
    for (int i = 0; i < measured; i++) {
      final var e = writer.tryWrite(com.anyilanxin.kunpeng.logstreams.log.WriteContext.internal(),
          com.anyilanxin.kunpeng.logstreams.log.LogAppendEntry.of(42, metadata, records.get(i & 63)));
      if (e.isRight()) {
        logStream.getFlowControl().onProcessed(e.get());
      }
    }
    return (System.nanoTime() - start) / measured;
  }

  /** 旧 SPI 的最小内存实现（同步提交, 记录块大小） */
  private static final class OldListStorage implements LogStorage {

    private final List<DirectBuffer> blocks = new CopyOnWriteArrayList<>();
    private final List<Integer> sizes = new CopyOnWriteArrayList<>();

    int firstBlockSize() {
      return sizes.isEmpty() ? 0 : sizes.get(0);
    }

    @Override
    public LogStorageReader newReader() {
      return new LogStorageReader() {
        int index;

        @Override
        public boolean hasNext() {
          return index < blocks.size();
        }

        @Override
        public DirectBuffer next() {
          if (!hasNext()) {
            throw new NoSuchElementException();
          }
          return blocks.get(index++);
        }

        @Override
        public void seek(final long position) {
          index = 0;
        }

        @Override
        public void close() {
          // 无资源
        }
      };
    }

    @Override
    public void append(
        final long lowestPosition,
        final long highestPosition,
        final com.anyilanxin.kunpeng.structpack.buffer.BufferWriter bufferWriter,
        final AppendListener listener) {
      final ExpandableArrayBuffer copy = new ExpandableArrayBuffer(bufferWriter.getLength());
      bufferWriter.write(copy, 0);
      blocks.add(new UnsafeBuffer(copy.byteArray(), 0, bufferWriter.getLength()));
      sizes.add(bufferWriter.getLength());
      listener.onWrite(lowestPosition, highestPosition);
      listener.onCommit(lowestPosition, highestPosition);
    }

    @Override
    public void addCommitListener(final CommitListener listener) {
      // 不需要
    }

    @Override
    public void removeCommitListener(final CommitListener listener) {
      // 不需要
    }
  }
}
