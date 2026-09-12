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
package com.anyilanxin.kunpeng.cluster.raft.snapshot;

import com.anyilanxin.kunpeng.cluster.raft.snapshot.impl.SnapshotChunkImpl;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.agrona.concurrent.UnsafeBuffer;

/**
 * 快照分片传输协议的 SBE 编解码器：覆盖拉取请求（序号 + 命令）、单个分片（{@link SnapshotChunk}） 与传输单元（{@link
 * SnapshotChunkBatch}）三种消息，编码产物可直接作为 {@code MessagingService} 的 payload。
 *
 * <p>传输单元线格式：外层 kind 与 hasMore 标记 + 定长 group 条目（含各分片名字与内容的长度）+ 尾部 payload（所有分片的 chunkName 与 content
 * 按条目顺序拼接）。编码时布局定长可算，名字与内容直接写入 消息数组最终位置，全程一次拷贝；解码时内容区只做 payload 视图包装，不发生拷贝。每次调用使用独立 的 flyweight
 * 实例，线程安全。
 */
public final class SnapshotTransferCodec {

  /** group 头部长度：blockLength(uint16) + numInGroup(uint32)。 */
  private static final int GROUP_HEADER_LENGTH =
      SnapshotChunkBatchEncoder.ChunksEncoder.sbeHeaderSize();

  private SnapshotTransferCodec() {}

  /**
   * 把一次分片拉取请求编码为 SBE 消息字节数组（命令为 {@link RequestCommand#PULL}）。
   *
   * @param requestId 请求标识，一次传输对应一个标识
   * @return 完整的消息 payload
   */
  public static byte[] encodeRequest(final String requestId) {
    return encodeRequest(requestId, RequestCommand.PULL);
  }

  /**
   * 把一次读取完成通知编码为 SBE 消息字节数组（命令为 {@link RequestCommand#COMPLETE}）。
   *
   * @param requestId 请求标识
   * @return 完整的消息 payload
   */
  public static byte[] encodeCompleteRequest(final String requestId) {
    return encodeRequest(requestId, RequestCommand.COMPLETE);
  }

  /**
   * 把指定命令的请求编码为 SBE 消息字节数组。
   *
   * @param requestId 请求标识
   * @param command 请求命令
   * @return 完整的消息 payload
   */
  public static byte[] encodeRequest(final String requestId, final RequestCommand command) {
    final byte[] idBytes = requestId.getBytes(StandardCharsets.UTF_8);
    final int capacity =
        MessageHeaderEncoder.ENCODED_LENGTH
            + SnapshotChunkRequestEncoder.BLOCK_LENGTH
            + SnapshotChunkRequestEncoder.requestIdHeaderLength()
            + idBytes.length;
    final var message = new byte[capacity];
    new SnapshotChunkRequestEncoder()
        .wrapAndApplyHeader(new UnsafeBuffer(message), 0, new MessageHeaderEncoder())
        .command(command)
        .requestId(requestId);
    return message;
  }

  /**
   * 从 SBE 消息字节数组解码出请求。
   *
   * @param payload 完整的消息 payload
   * @return 请求（标识 + 命令）
   * @throws IllegalArgumentException 消息模板不匹配时抛出
   */
  public static Request decodeRequest(final byte[] payload) {
    final var buffer = new UnsafeBuffer(payload);
    final var headerDecoder = new MessageHeaderDecoder();
    headerDecoder.wrap(buffer, 0);
    if (headerDecoder.templateId() != SnapshotChunkRequestDecoder.TEMPLATE_ID) {
      throw new IllegalArgumentException("Unexpected template id " + headerDecoder.templateId());
    }
    final var decoder =
        new SnapshotChunkRequestDecoder().wrapAndApplyHeader(buffer, 0, headerDecoder);
    return new Request(decoder.requestId(), decoder.command());
  }

  /**
   * 把传输单元编码为 SBE 消息字节数组。
   *
   * @param batch 待编码的传输单元
   * @return 完整的消息 payload
   */
  public static byte[] encodeChunkBatch(final SnapshotChunkBatch batch) {
    final List<SnapshotChunk> chunks = batch.chunks();
    // 消息布局：头部 + kind/hasMore + group 头 + 每个条目的定长块与末尾变长字段（名字 + 内容）
    final int entryBlockLength = SnapshotChunkBatchEncoder.ChunksEncoder.sbeBlockLength();
    int capacity =
        MessageHeaderEncoder.ENCODED_LENGTH
            + SnapshotChunkBatchEncoder.BLOCK_LENGTH
            + GROUP_HEADER_LENGTH;
    for (final SnapshotChunk chunk : chunks) {
      capacity +=
          entryBlockLength
              + SnapshotChunkBatchEncoder.ChunksEncoder.chunkNameHeaderLength()
              + chunk.getChunkName().getBytes(StandardCharsets.UTF_8).length
              + SnapshotChunkBatchEncoder.ChunksEncoder.contentHeaderLength()
              + chunk.getLength();
    }
    final var message = new byte[capacity];
    final var buffer = new UnsafeBuffer(message);

    final var encoder = new SnapshotChunkBatchEncoder();
    encoder
        .wrapAndApplyHeader(buffer, 0, new MessageHeaderEncoder())
        .kind(batch.kind())
        .hasMore((short) (batch.hasMore() ? 1 : 0));
    final var group = encoder.chunksCount(chunks.size());
    for (final SnapshotChunk chunk : chunks) {
      group
          .next()
          .checksum(chunk.getChecksum())
          .snapshotChecksum(chunk.getSnapshotChecksum())
          .totalLength(chunk.getTotalLength())
          .fileOffset(chunk.getOffset())
          .chunkName(chunk.getChunkName());

      // content 从源缓冲区一次性拷入消息体；手工铺 length 前缀以接受任意 ByteBuffer 源
      final ByteBuffer content = chunk.getContent();
      final int contentLength = content.remaining();
      final int lengthOffset = MessageHeaderEncoder.ENCODED_LENGTH + encoder.encodedLength();
      buffer.putInt(lengthOffset, contentLength, SnapshotChunkBatchEncoder.BYTE_ORDER);
      encoder.limit(
          lengthOffset
              + SnapshotChunkBatchEncoder.ChunksEncoder.contentHeaderLength()
              + contentLength);
      buffer.putBytes(
          lengthOffset + SnapshotChunkBatchEncoder.ChunksEncoder.contentHeaderLength(),
          content,
          contentLength);
    }
    return message;
  }

  /**
   * 从 SBE 消息字节数组零拷贝解码出传输单元，各分片内容区为消息字节数组的视图。
   *
   * @param message 完整的消息 payload
   * @return 解码出的传输单元
   * @throws IllegalArgumentException 消息模板或类型标记非法时抛出
   */
  public static SnapshotChunkBatch decodeChunkBatch(final byte[] message) {
    final var buffer = new UnsafeBuffer(message);
    final var headerDecoder = new MessageHeaderDecoder();
    headerDecoder.wrap(buffer, 0);
    if (headerDecoder.templateId() != SnapshotChunkBatchDecoder.TEMPLATE_ID) {
      throw new IllegalArgumentException("Unexpected template id " + headerDecoder.templateId());
    }

    final var decoder = new SnapshotChunkBatchDecoder();
    decoder.wrapAndApplyHeader(buffer, 0, headerDecoder);
    final TransferKind kind = decoder.kind();
    if (kind == null || kind == TransferKind.NULL_VAL) {
      throw new IllegalArgumentException("Illegal transfer kind");
    }
    final boolean hasMore = decoder.hasMore() != 0;

    final var chunks = new ArrayList<SnapshotChunk>();
    final var group = decoder.chunks();
    for (final SnapshotChunkBatchDecoder.ChunksDecoder entry : group) {
      final long checksum = entry.checksum();
      final long snapshotChecksum = entry.snapshotChecksum();
      final long totalLength = entry.totalLength();
      final long fileOffset = entry.fileOffset();
      final String chunkName = entry.chunkName();
      final int contentLength = entry.contentLength();
      final int contentOffset =
          decoder.limit() + SnapshotChunkBatchDecoder.ChunksDecoder.contentHeaderLength();
      entry.skipContent();
      final ByteBuffer content = ByteBuffer.wrap(message, contentOffset, contentLength).slice();
      chunks.add(
          new SnapshotChunkImpl(
              chunkName, snapshotChecksum, totalLength, checksum, content, fileOffset));
    }
    return new SnapshotChunkBatch(kind, chunks, hasMore);
  }

  /**
   * 把单个分片编码为 SBE 消息字节数组（字段与传输单元的 chunks 条目一致）。
   *
   * @param chunk 待编码的分片
   * @return 完整的消息 payload
   */
  public static byte[] encodeChunk(final SnapshotChunk chunk) {
    final ByteBuffer content = chunk.getContent();
    final int contentLength = content.remaining();
    final byte[] name = chunk.getChunkName().getBytes(StandardCharsets.UTF_8);

    final int capacity =
        MessageHeaderEncoder.ENCODED_LENGTH
            + SnapshotChunkEncoder.BLOCK_LENGTH
            + SnapshotChunkEncoder.chunkNameHeaderLength()
            + name.length
            + SnapshotChunkEncoder.contentHeaderLength()
            + contentLength;
    final var message = new byte[capacity];
    final var buffer = new UnsafeBuffer(message);
    final var encoder = new SnapshotChunkEncoder();
    encoder
        .wrapAndApplyHeader(buffer, 0, new MessageHeaderEncoder())
        .checksum(chunk.getChecksum())
        .snapshotChecksum(chunk.getSnapshotChecksum())
        .totalLength(chunk.getTotalLength())
        .fileOffset(chunk.getOffset())
        .chunkName(chunk.getChunkName());

    // content 从源缓冲区一次性拷入消息体；手工铺 length 前缀以接受任意 ByteBuffer 源
    final int lengthOffset = MessageHeaderEncoder.ENCODED_LENGTH + encoder.encodedLength();
    buffer.putInt(lengthOffset, contentLength, SnapshotChunkEncoder.BYTE_ORDER);
    encoder.limit(lengthOffset + SnapshotChunkEncoder.contentHeaderLength() + contentLength);
    buffer.putBytes(
        lengthOffset + SnapshotChunkEncoder.contentHeaderLength(), content, contentLength);
    return message;
  }

  /**
   * 从 SBE 消息字节数组零拷贝解码出单个分片，内容区为 payload 的视图。
   *
   * @param payload 完整的消息 payload
   * @return 解码出的分片
   * @throws IllegalArgumentException 消息模板不匹配时抛出
   */
  public static SnapshotChunk decodeChunk(final byte[] payload) {
    final var buffer = new UnsafeBuffer(payload);
    final var headerDecoder = new MessageHeaderDecoder();
    headerDecoder.wrap(buffer, 0);
    if (headerDecoder.templateId() != SnapshotChunkDecoder.TEMPLATE_ID) {
      throw new IllegalArgumentException("Unexpected template id " + headerDecoder.templateId());
    }

    final var decoder = new SnapshotChunkDecoder();
    decoder.wrapAndApplyHeader(buffer, 0, headerDecoder);
    final long checksum = decoder.checksum();
    final long snapshotChecksum = decoder.snapshotChecksum();
    final long totalLength = decoder.totalLength();
    final long fileOffset = decoder.fileOffset();
    final String chunkName = decoder.chunkName();

    final int contentLength = decoder.contentLength();
    final int contentOffset = decoder.limit() + SnapshotChunkDecoder.contentHeaderLength();
    decoder.skipContent();
    final ByteBuffer content = ByteBuffer.wrap(payload, contentOffset, contentLength).slice();

    return new SnapshotChunkImpl(
        chunkName, snapshotChecksum, totalLength, checksum, content, fileOffset);
  }

  /** 分片拉取请求：请求标识 + 命令。 */
  public record Request(String requestId, RequestCommand command) {}
}
