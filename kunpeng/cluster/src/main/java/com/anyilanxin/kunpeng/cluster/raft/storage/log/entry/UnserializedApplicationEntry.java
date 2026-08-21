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
package com.anyilanxin.kunpeng.cluster.raft.storage.log.entry;

import com.anyilanxin.kunpeng.structpack.buffer.BufferWriter;
import com.anyilanxin.kunpeng.cluster.raft.storage.serializer.RaftEntrySerializer;
import com.anyilanxin.kunpeng.cluster.raft.storage.serializer.RaftEntrySerializer.SerializedBufferWriterAdapter;
import java.util.Objects;

/** 负载尚未序列化的 {@link ApplicationEntry}，是向日志追加新条目时使用的形态。 */
public final class UnserializedApplicationEntry implements ApplicationEntry {

  /** 本条目覆盖的最小条目位置。 */
  private final long lowestPosition;

  /** 本条目覆盖的最大条目位置。 */
  private final long highestPosition;

  /** 延迟写入负载数据的写出器。 */
  private final BufferWriter dataWriter;

  public UnserializedApplicationEntry(
      final long lowestPosition, final long highestPosition, final BufferWriter dataWriter) {
    this.lowestPosition = lowestPosition;
    this.highestPosition = highestPosition;
    this.dataWriter = dataWriter;
  }

  @Override
  public long lowestPosition() {
    return lowestPosition;
  }

  @Override
  public long highestPosition() {
    return highestPosition;
  }

  /** 延迟写入负载数据的写出器。 */
  public BufferWriter dataWriter() {
    return dataWriter;
  }

  /** 延迟序列化：先按序列化器计算长度，再在写入时按指定任期落盘。 */
  @Override
  public BufferWriter toSerializable(final long term, final RaftEntrySerializer serializer) {
    return new SerializedBufferWriterAdapter(
        () -> serializer.getApplicationEntrySerializedLength(this),
        (buffer, offset) -> serializer.writeApplicationEntry(term, this, buffer, offset));
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof UnserializedApplicationEntry)) {
      return false;
    }
    final UnserializedApplicationEntry that = (UnserializedApplicationEntry) o;
    return lowestPosition == that.lowestPosition
        && highestPosition == that.highestPosition
        && Objects.equals(dataWriter, that.dataWriter);
  }

  @Override
  public int hashCode() {
    return Objects.hash(lowestPosition, highestPosition, dataWriter);
  }

  @Override
  public String toString() {
    return "UnserializedApplicationEntry{lowestPosition="
        + lowestPosition
        + ", highestPosition="
        + highestPosition
        + '}';
  }
}
