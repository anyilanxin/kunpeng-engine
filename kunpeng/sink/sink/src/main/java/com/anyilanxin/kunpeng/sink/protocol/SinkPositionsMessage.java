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
package com.anyilanxin.kunpeng.sink.protocol;

import com.anyilanxin.kunpeng.structpack.util.BufferUtil;
import java.util.HashMap;
import java.util.Map;
import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.collections.MutableInteger;
import org.agrona.concurrent.UnsafeBuffer;

/**
 * 每个 Sink 确认位置（及可选的检查点元数据）的快照，由分区 leader 广播， follower 应用后即可在不重新投递的前提下接管。
 *
 * <p>字节布局刻意与上一代实现产出的保持一致（标识符见 schema 资源）， 两者之间的滚动升级可以继续互换这类消息。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class SinkPositionsMessage
    extends SbeMessage<SinkPositionsEncoder, SinkPositionsDecoder> {

  private final Map<String, SinkPosition> positions = new HashMap<>();
  private final SinkPositionsEncoder encoder = new SinkPositionsEncoder();
  private final SinkPositionsDecoder decoder = new SinkPositionsDecoder();

  /** 单个 Sink 的确认状态。 */
  public record SinkPosition(long position, DirectBuffer metadata) {}

  @Override
  protected SinkPositionsEncoder bodyEncoder() {
    return encoder;
  }

  @Override
  protected SinkPositionsDecoder bodyDecoder() {
    return decoder;
  }

  @Override
  protected void reset() {
    positions.clear();
  }

  /**
   * 为下一次 {@link #toByteBuffer()} 记录或替换某个 Sink 的状态。
   *
   * @param sinkId 状态所属的 Sink
   * @param position 该 Sink 的确认位置
   * @param metadata 该 Sink 的检查点元数据；{@code null} 编码为空
   */
  public void put(final String sinkId, final long position, final DirectBuffer metadata) {
    positions.put(
        sinkId, new SinkPosition(position, metadata == null ? new UnsafeBuffer() : metadata));
  }

  /**
   * @return 解码出的位置，按 Sink id 索引；在 {@link #wrap} 之后有效
   */
  public Map<String, SinkPosition> getPositions() {
    return positions;
  }

  @Override
  public int getLength() {
    // 消息头 + 组维度 + 各条目大小之和
    return super.getLength() + SinkPositionsEncoder.EntryEncoder.sbeHeaderSize() + entryBytes();
  }

  private int entryBytes() {
    final var total = new MutableInteger();
    positions.forEach(
        (id, state) ->
            total.addAndGet(
                SinkPositionsEncoder.EntryEncoder.positionEncodingLength()
                    + SinkPositionsEncoder.EntryEncoder.sinkIdHeaderLength()
                    + SinkPositionsEncoder.EntryEncoder.metadataHeaderLength()
                    + id.length()
                    + state.metadata().capacity()));
    return total.get();
  }

  @Override
  public void write(final MutableDirectBuffer buffer, final int offset) {
    super.write(buffer, offset);

    final var group = encoder.entryCount(positions.size());
    positions.forEach(
        (id, state) -> {
          final var idBuffer = BufferUtil.wrapString(id);
          group
              .next()
              .position(state.position())
              .putSinkId(idBuffer, 0, idBuffer.capacity())
              .putMetadata(state.metadata(), 0, state.metadata().capacity());
        });
  }

  @Override
  public void wrap(final DirectBuffer buffer, final int offset, final int length) {
    super.wrap(buffer, offset, length);

    final var group = decoder.entry();
    while (group.hasNext()) {
      final var entry = group.next();
      final byte[] sinkId = new byte[entry.sinkIdLength()];
      entry.getSinkId(sinkId, 0, sinkId.length);
      final byte[] metadata = new byte[entry.metadataLength()];
      entry.getMetadata(metadata, 0, metadata.length);

      positions.put(
          new String(sinkId), new SinkPosition(entry.position(), new UnsafeBuffer(metadata)));
    }
  }
}
