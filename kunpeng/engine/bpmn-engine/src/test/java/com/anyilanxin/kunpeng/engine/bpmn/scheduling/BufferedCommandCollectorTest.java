/*
 * Copyright © 2026 anyilanxin zxh(anyilanxin@aliyun.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
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
import com.anyilanxin.kunpeng.protocol.business.impl.record.DefaultRecordValueMapper;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.timer.TimerEventRecord;
import com.anyilanxin.kunpeng.protocol.business.record.RecordValueMapper;
import com.anyilanxin.kunpeng.protocol.business.record.command.timer.TimerLifeCycle;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.junit.jupiter.api.Test;

/** 调度命令收集的序列化次数、快照所有权与 EL 批帧往返测试。 */
class BufferedCommandCollectorTest {

  @Test
  void shouldMaterializeOnceAndPreserveSnapshotThroughBatchFrame() throws Exception {
    CountingTimerEventRecord.WRITES.set(0);
    final List<SupplierRegistration> registrations = new ArrayList<>();
    try {
      instrumentMapper(BufferedCommandCollector.class, false, registrations);
      instrumentMapper(CommandRecord.class, true, registrations);
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
      assertEquals(1, CountingTimerEventRecord.WRITES.get(), "收集器与入批合计只能序列化一次");

      final var entries = collector.build().entries();
      // 收集完成后、EL 组帧前复用源对象，验证批内快照不依赖源 recordBuffer。
      timer.setTimerId(999).setDueDate(1_900);
      final int frameLength = BatchFrame.calculateLength(1, -1, 1_000, entries);
      final var frame = new UnsafeBuffer(new byte[frameLength]);
      final int written = BatchFrame.serialize(frame, 0, 1, -1, 1_000, entries);
      assertEquals(frameLength, written);
      assertEquals(2, CountingTimerEventRecord.WRITES.get(), "EL 组帧再正常写入一次快照");

      final var decoder = new BatchFrameDecoder().wrap(frame);
      assertTrue(decoder.nextEntry());
      assertEquals(42, decoder.entryKey());
      final var decoded = new TimerEventRecord();
      decoded.wrap(frame, decoder.entryValueOffset(), decoder.entryValueLength());
      assertEquals(42, decoded.getTimerId());
      assertEquals(900, decoded.getDueDate());
      assertFalse(decoder.nextEntry());
    } finally {
      registrations.forEach(SupplierRegistration::restore);
    }
  }

  /** 接管指定类的 mapper；只有允许缺失的收集器可跳过，CommandRecord 漏接管会直接失败。 */
  private static void instrumentMapper(
      final Class<?> owner,
      final boolean required,
      final List<SupplierRegistration> registrations)
      throws Exception {
    final Field mapperField;
    try {
      mapperField = owner.getDeclaredField("VALUE_MAPPER");
    } catch (final NoSuchFieldException e) {
      if (required) {
        throw e;
      }
      return;
    }
    mapperField.setAccessible(true);
    final var mapper = (RecordValueMapper) mapperField.get(null);
    final Field suppliersField = DefaultRecordValueMapper.class.getDeclaredField("valueSuppliers");
    suppliersField.setAccessible(true);
    final var suppliers = (Supplier<?>[]) suppliersField.get(mapper);
    final int index = TimerLifeCycle.TRIGGER.recordIndex();
    registrations.add(new SupplierRegistration(suppliers, suppliers[index]));
    suppliers[index] = CountingTimerEventRecord::new;
  }

  private record SupplierRegistration(Supplier<?>[] suppliers, Supplier<?> original) {
    void restore() {
      suppliers[TimerLifeCycle.TRIGGER.recordIndex()] = original;
    }
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
