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
package com.anyilanxin.kunpeng.cluster.dispatch.scheduling;

import com.anyilanxin.kunpeng.eventlog.AppendEntry;
import com.anyilanxin.kunpeng.protocol.admin.impl.AdminRecordMetadata;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.DefaultRecordValueMapper;
import com.anyilanxin.kunpeng.protocol.admin.record.RecordValueMapper;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.anyilanxin.kunpeng.structpack.buffer.BufferWriter;
import org.agrona.concurrent.UnsafeBuffer;

/**
 * 批内单条命令记录：不可变 record 形态的 {@link AppendEntry}。
 *
 * <p>入批时 value 被物化为独立字节拷贝并按生命周期还原为 typed value，后续对源缓冲的修改 不再影响批内快照。
 */
public record CommandRecord(
    long key, int sourceIndex, AdminRecordMetadata header, UnifiedRecordValue valueSnapshot)
    implements AppendEntry {

  private static final RecordValueMapper VALUE_MAPPER = DefaultRecordValueMapper.getInstance();

  @Override
  public boolean isSkipProcessing() {
    return false;
  }

  @Override
  public BufferWriter metadata() {
    return header;
  }

  @Override
  public BufferWriter value() {
    return valueSnapshot;
  }

  /**
   * 将 {@code valueWriter} 的字节拷贝为独立快照，并按其生命周期还原为 typed value。
   *
   * @param key 记录 key，无 key 传 {@code -1}
   * @param sourceIndex 批内回指，无回指传 {@code -1}
   * @param metadata 记录元数据
   * @param valueWriter 记录值
   * @return 批内命令记录
   */
  public static CommandRecord materialize(
      final long key,
      final int sourceIndex,
      final AdminRecordMetadata metadata,
      final BufferWriter valueWriter) {
    final var bytes = new byte[valueWriter.getLength()];
    final var snapshot = new UnsafeBuffer(bytes);
    valueWriter.write(snapshot, 0);
    final UnifiedRecordValue typedValue = VALUE_MAPPER.getValue(metadata.getLifeCycle());
    typedValue.wrap(snapshot, 0, snapshot.capacity());
    return new CommandRecord(key, sourceIndex, metadata, typedValue);
  }
}
