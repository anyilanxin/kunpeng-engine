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
package com.anyilanxin.kunpeng.engine.bpmn.scheduling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.anyilanxin.kunpeng.engine.bpmn.scheduling.cache.BoundedPendingCommandRegistry;
import com.anyilanxin.kunpeng.engine.bpmn.scheduling.cache.RegistryMetrics;
import com.anyilanxin.kunpeng.eventlog.serialize.BatchFrame;
import com.anyilanxin.kunpeng.eventlog.serialize.BatchFrameDecoder;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.timer.TimerEventRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.timer.TimerLifeCycle;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.concurrent.atomic.AtomicInteger;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.junit.jupiter.api.Test;

/** 调度命令收集的序列化次数、快照所有权与 EL 批帧往返测试。 */
class BufferedCommandCollectorTest {

  @Test
  void shouldSnapshotOnceAndPreserveSnapshotThroughBatchFrame() {
    CountingTimerEventRecord.WRITES.set(0);
    final var timer =
        new CountingTimerEventRecord()
            .setTimerId(42)
            .setDueDate(900)
            .setProcessDefinitionKey("test");
    final var registry =
        BoundedPendingCommandRegistry.forIntents(
            new RegistryMetrics.BoundedRegistryMetrics(new SimpleMeterRegistry(), 1),
            TimerLifeCycle.TRIGGER);
    final var collector = new BufferedCommandCollector((_, _) -> true, registry.stage());

    assertTrue(collector.appendCommand(timer.getTimerId(), TimerLifeCycle.TRIGGER, timer));
    assertEquals(1, CountingTimerEventRecord.WRITES.get(), "入批时只序列化一次");

    final var entries = collector.build().entries();
    // 收集完成后、EL 组帧前复用源对象，验证批内快照不依赖源 recordBuffer。
    timer.setTimerId(999).setDueDate(1_900);
    final int frameLength = BatchFrame.calculateLength(1, -1, 1_000, entries);
    final var frame = new UnsafeBuffer(new byte[frameLength]);
    final int written = BatchFrame.serialize(frame, 0, 1, -1, 1_000, entries);
    assertEquals(frameLength, written);
    assertEquals(1, CountingTimerEventRecord.WRITES.get(), "EL 组帧直接拷贝快照字节，不再编码记录");

    final var decoder = new BatchFrameDecoder().wrap(frame);
    assertTrue(decoder.nextEntry());
    assertEquals(42, decoder.entryKey());
    final var decoded = new TimerEventRecord();
    decoded.wrap(frame, decoder.entryValueOffset(), decoder.entryValueLength());
    assertEquals(42, decoded.getTimerId());
    assertEquals(900, decoded.getDueDate());
    assertFalse(decoder.nextEntry());
  }

  private static final class CountingTimerEventRecord extends TimerEventRecord {
    private static final AtomicInteger WRITES = new AtomicInteger();

    @Override
    public void write(final MutableDirectBuffer buffer, final int offset) {
      WRITES.incrementAndGet();
      super.write(buffer, offset);
    }
  }
}
