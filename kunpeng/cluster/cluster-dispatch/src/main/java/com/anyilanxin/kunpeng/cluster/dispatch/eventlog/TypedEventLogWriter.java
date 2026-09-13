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
package com.anyilanxin.kunpeng.cluster.dispatch.eventlog;

import com.anyilanxin.kunpeng.eventlog.AppendEntry;
import com.anyilanxin.kunpeng.eventlog.AppendResult;
import com.anyilanxin.kunpeng.eventlog.EventLogWriter;
import com.anyilanxin.kunpeng.eventlog.WriteContext;
import com.anyilanxin.kunpeng.protocol.admin.impl.AdminRecordMetadata;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * typed 追加端：typed record 经 {@link RecordAppendEntryFactory} 转为不透明 {@link AppendEntry} 后委托底层 {@link
 * EventLogWriter}，并在成功时按 RecordType × ValueType 打点（承接旧模块的 typed 计数维度）。
 *
 * <p>计数在 typed 信息可得的工厂侧完成——底层 writer 只见字节，无需解码。
 */
public final class TypedEventLogWriter implements EventLogWriter {

  private final EventLogWriter delegate;
  private final MeterRegistry registry;
  private final ConcurrentHashMap<String, Counter> typedCounters = new ConcurrentHashMap<>();

  public TypedEventLogWriter(final EventLogWriter delegate, final MeterRegistry registry) {
    this.delegate = delegate;
    this.registry = registry;
  }

  public AppendResult tryAppend(
      final WriteContext context,
      final AdminRecordMetadata metadata,
      final UnifiedRecordValue value) {
    return tryAppend(context, -1, metadata, value);
  }

  public AppendResult tryAppend(
      final WriteContext context,
      final long key,
      final AdminRecordMetadata metadata,
      final UnifiedRecordValue value) {
    return record(
        metadata, delegate.tryAppend(context, RecordAppendEntryFactory.of(key, metadata, value)));
  }

  public AppendResult tryAppend(
      final WriteContext context,
      final long key,
      final AdminRecordMetadata metadata,
      final UnifiedRecordValue value,
      final int sourceIndex) {
    return record(
        metadata,
        delegate.tryAppend(
            context, RecordAppendEntryFactory.of(key, metadata, value, sourceIndex)));
  }

  @Override
  public AppendResult tryAppend(final WriteContext context, final AppendEntry entry) {
    return delegate.tryAppend(context, entry);
  }

  @Override
  public AppendResult tryAppend(final WriteContext context, final List<AppendEntry> entries) {
    return delegate.tryAppend(context, entries);
  }

  @Override
  public AppendResult tryAppend(
      final WriteContext context, final List<AppendEntry> entries, final long sourcePosition) {
    return delegate.tryAppend(context, entries, sourcePosition);
  }

  @Override
  public boolean canAppend(final int entryCount, final int batchSizeBytes) {
    return delegate.canAppend(entryCount, batchSizeBytes);
  }

  @Override
  public void close() {
    delegate.close();
  }

  private AppendResult record(final AdminRecordMetadata metadata, final AppendResult result) {
    if (registry != null && result instanceof AppendResult.Appended) {
      final String valueType = String.valueOf(metadata.getValueType());
      typedCounters
          .computeIfAbsent(
              valueType,
              key -> registry.counter("protocol.eventlog.append.count", "valueType", valueType))
          .increment();
    }
    return result;
  }
}
