/*
 * Copyright © 2020 camunda services GmbH (info@camunda.com)
 * Copyright © 2026 anyilanxin zxh (anyilanxin@aliyun.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.atomix.raft.storage.log.entry;

import com.anyilanxin.kunpeng.structpack.buffer.BufferWriter;
import com.anyilanxin.kunpeng.structpack.buffer.DirectBufferWriter;
import io.atomix.raft.storage.serializer.RaftEntrySerializer;
import io.atomix.raft.storage.serializer.RaftEntrySerializer.SerializedBufferWriterAdapter;
import java.nio.ByteBuffer;
import java.util.Objects;
import org.agrona.DirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;

/**
 * 已序列化的 {@link ApplicationEntry}：负载按条目位置有序存放，lowestPosition 与
 * highestPosition 元数据支持在条目集合上做二分查找，快速定位某个记录。
 */
public final class SerializedApplicationEntry implements ApplicationEntry {

  /** 本条目覆盖的最小条目位置。 */
  private final long lowestPosition;

  /** 本条目覆盖的最大条目位置。 */
  private final long highestPosition;

  /** 已序列化的负载数据。 */
  private final DirectBuffer data;

  public SerializedApplicationEntry(
      final long lowestPosition, final long highestPosition, final DirectBuffer data) {
    this.lowestPosition = lowestPosition;
    this.highestPosition = highestPosition;
    this.data = data;
  }

  /** 便捷构造：把 {@link ByteBuffer} 包装为 {@link UnsafeBuffer} 作为负载。 */
  public SerializedApplicationEntry(
      final long lowestPosition, final long highestPosition, final ByteBuffer data) {
    this(lowestPosition, highestPosition, new UnsafeBuffer(data));
  }

  @Override
  public long lowestPosition() {
    return lowestPosition;
  }

  @Override
  public long highestPosition() {
    return highestPosition;
  }

  /** 已序列化的负载数据。 */
  public DirectBuffer data() {
    return data;
  }

  @Override
  public BufferWriter toSerializable(final long term, final RaftEntrySerializer serializer) {
    return new SerializedBufferWriterAdapter(
        () -> serializer.getApplicationEntrySerializedLength(this),
        (buffer, offset) -> serializer.writeApplicationEntry(term, this, buffer, offset));
  }

  @Override
  public BufferWriter dataWriter() {
    return new DirectBufferWriter().wrap(data);
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof SerializedApplicationEntry)) {
      return false;
    }
    final SerializedApplicationEntry that = (SerializedApplicationEntry) o;
    return lowestPosition == that.lowestPosition
        && highestPosition == that.highestPosition
        && Objects.equals(data, that.data);
  }

  @Override
  public int hashCode() {
    return Objects.hash(lowestPosition, highestPosition, data);
  }

  @Override
  public String toString() {
    return "SerializedApplicationEntry{lowestPosition="
        + lowestPosition
        + ", highestPosition="
        + highestPosition
        + ", capacity="
        + (data == null ? 0 : data.capacity())
        + '}';
  }
}
