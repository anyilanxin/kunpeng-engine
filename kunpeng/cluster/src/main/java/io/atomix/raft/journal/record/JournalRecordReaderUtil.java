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
package io.atomix.raft.journal.record;

import io.atomix.raft.journal.CorruptedJournalException;
import io.atomix.raft.journal.JournalException.InvalidIndex;
import io.atomix.raft.journal.JournalRecord;
import io.atomix.raft.journal.util.ChecksumGenerator;
import java.nio.ByteBuffer;
import org.agrona.concurrent.UnsafeBuffer;

/**
 * 从缓冲区中解码单条日志记录的通用工具。
 *
 * <p>读取流程分四步：校验头部长度边界 -> 解析记录头 -> 校验体部 CRC32C 校验和 -> 解析
 * 记录体并核对索引连续性。任一步失败都会回滚缓冲区 position 并抛出相应异常。供
 * SegmentWriter（重放校验）与 SegmentReader（顺序读取）共用。
 */
public final class JournalRecordReaderUtil {

  private final ChecksumGenerator checksum = new ChecksumGenerator();
  private final JournalRecordSerializer serializer;

  /**
   * @param serializer 按当前编码版本解析记录头与记录体的序列化器
   */
  public JournalRecordReaderUtil(final JournalRecordSerializer serializer) {
    this.serializer = serializer;
  }

  /**
   * 读取缓冲区当前位置处的日志记录。
   *
   * <p>方法返回时，{@code buffer} 的 position 已推进到下一条记录的起始处；若中途校验失败，
   * position 会被恢复到进入方法时的值。
   *
   * @param buffer 待读取的缓冲区
   * @param expectedIndex 期望该记录携带的日志索引（用于连续性校验）
   * @param frameLength 记录前面的帧版本/长度字段占用的字节数
   * @return 解析完成的记录
   * @throws CorruptedJournalException 边界越界或校验和不匹配
   * @throws InvalidIndex 记录索引与期望索引不一致
   */
  public JournalRecord read(
      final ByteBuffer buffer, final long expectedIndex, final int frameLength) {
    // 打标记，失败时可以回退到进入前的位置
    buffer.mark();
    final int frameStart = buffer.position();

    final UnsafeBuffer frameView = new UnsafeBuffer(buffer.slice());
    final RecordMetadata metadata = readHeader(buffer, frameView, frameStart);
    final int headerSize = serializer.getMetadataLength(frameView, 0);
    final int bodySize = metadata.length();

    verifyChecksum(buffer, frameStart, headerSize, bodySize, metadata);

    final RecordData body = serializer.readData(frameView, headerSize);
    if (body != null && expectedIndex != body.index()) {
      buffer.reset();
      throw new InvalidIndex(
          String.format(
              "Expected to read a record with next index %d, but found %d",
              expectedIndex, body.index()));
    }

    buffer.position(frameStart + headerSize + bodySize);

    // 注意：UnsafeBuffer 通过内存地址直接包装 ByteBuffer；一旦底层缓冲被 unmap，
    // 再访问 serializedRecord 会导致 JVM 崩溃
    // 参考：https://github.com/camunda/camunda/issues/57609
    return new PersistedJournalRecord(
        metadata,
        body,
        new UnsafeBuffer(buffer, frameStart + headerSize, bodySize),
        frameLength + headerSize + bodySize);
  }

  /** 解析并返回记录头，同时校验头部与整体记录的边界。 */
  private RecordMetadata readHeader(
      final ByteBuffer buffer, final UnsafeBuffer frameView, final int frameStart) {
    if (buffer.position() + serializer.getMetadataLength() > buffer.limit()) {
      // 正常情况下调用方会先通过 hasNext() 确认存在记录，走到这里说明日志已损坏
      throw new CorruptedJournalException(
          "Expected to read a record, but reached the end of the segment.");
    }

    final RecordMetadata metadata = serializer.readMetadata(frameView, 0);

    if (buffer.position() + serializer.getMetadataLength(frameView, 0) + metadata.length()
        > buffer.limit()) {
      // 每条记录前都写有帧头，若帧头声称的长度超出剩余空间，说明记录不完整
      throw new CorruptedJournalException(
          String.format(
              "Expected to read a record at position %d, with metadata %s, but reached the end of the segment.",
              buffer.position(), metadata));
    }
    return metadata;
  }

  /** 对记录体重新计算校验和并与头部声明值比对，不一致则回退 position 并抛出异常。 */
  private void verifyChecksum(
      final ByteBuffer buffer,
      final int frameStart,
      final int headerSize,
      final int bodySize,
      final RecordMetadata metadata) {
    final long actual = checksum.compute(buffer, frameStart + headerSize, bodySize);
    final long expected = metadata.checksum();
    if (actual != expected) {
      buffer.reset();
      throw new CorruptedJournalException(
          "Record's checksum (%d) doesn't match checksum stored in metadata (%d)."
              .formatted(actual, expected));
    }
  }
}
