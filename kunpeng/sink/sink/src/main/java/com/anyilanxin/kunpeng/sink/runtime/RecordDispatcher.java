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

import com.anyilanxin.kunpeng.eventlog.LoggedEntry;
import com.anyilanxin.kunpeng.protocol.business.ValueType;
import com.anyilanxin.kunpeng.protocol.business.impl.RecordMetadata;
import com.anyilanxin.kunpeng.protocol.business.impl.eventlog.TypedRecordReader;
import com.anyilanxin.kunpeng.protocol.business.impl.record.DefaultRecordValueMapper;
import com.anyilanxin.kunpeng.protocol.business.record.RecordValueMapper;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.sink.metrics.SinkMetrics;
import java.time.InstantSource;
import java.util.List;

/**
 * 把一条原始日志条目转换成类型化记录，并按顺序、恰好一次地交给每个槽位。
 *
 * <p>同一时刻一条记录最多投递给一个槽位。槽位失败时分发即停，并记住在槽位列表中的位置， 重试时从失败的槽位继续，而不是重新投递给已成功的那些。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
final class RecordDispatcher {

  private final RecordValueMapper valueMapper = DefaultRecordValueMapper.getInstance();
  private final RecordMetadata rawMetadata = new RecordMetadata();
  private final List<SinkSlot> slots;
  private final TypedRecordReader typedRecord;
  private final SinkMetrics metrics;
  private final InstantSource clock;

  private boolean dispatchable;
  private int nextSlot;

  RecordDispatcher(
      final SinkMetrics metrics,
      final List<SinkSlot> slots,
      final int partitionId,
      final InstantSource clock) {
    this.metrics = metrics;
    this.slots = slots;
    this.clock = clock;
    typedRecord = new TypedRecordReader(partitionId);
  }

  /**
   * 准备一条条目的分发：把它的元数据与值解码进复用的记录视图。
   *
   * @param entry 原始日志条目
   */
  void wrap(final LoggedEntry entry) {
    entry.readMetadata(rawMetadata);

    final UnifiedRecordValue value = valueMapper.getCacheValue(rawMetadata.getLifeCycle());
    dispatchable = value != null;
    if (dispatchable) {
      entry.readValue(value);
      typedRecord.wrap(entry, rawMetadata, value);
      nextSlot = 0;
    }
  }

  /**
   * 把已包装的记录分发给所有槽位，从上次失败的那个开始。
   *
   * @return 所有槽位都处理完时返回 true；一旦有槽位失败立即返回 false—— 无需重新 {@link #wrap}，再次调用本方法即可正确续传
   */
  boolean dispatch() {
    if (!dispatchable) {
      return true;
    }

    final ValueType valueType = typedRecord.getValueType();
    metrics.recordPickupLatency(valueType, typedRecord.getTimestamp(), clock.millis());

    while (nextSlot < slots.size()) {
      final SinkSlot slot = slots.get(nextSlot);
      try (final var ignored = metrics.startProcessTimer(valueType, slot.getId())) {
        if (slot.sinkRecord(rawMetadata, typedRecord)) {
          nextSlot++;
        } else {
          return false;
        }
      }
    }
    return true;
  }

  ValueType getValueType() {
    return typedRecord.getValueType();
  }

  /** 丢弃续传点。记录在途期间槽位列表发生变化后必须调用， 否则可能出现槽位被跳过或重复收到同一条记录。 */
  void resetResumePoint() {
    nextSlot = 0;
  }
}
