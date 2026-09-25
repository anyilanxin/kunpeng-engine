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
package com.anyilanxin.kunpeng.cluster.raft.journal.record;

import com.anyilanxin.kunpeng.cluster.raft.journal.CorruptedJournalException;
import com.anyilanxin.kunpeng.cluster.raft.journal.JournalException.InvalidIndex;
import com.anyilanxin.kunpeng.cluster.raft.journal.JournalRecord;
import com.anyilanxin.kunpeng.cluster.raft.journal.util.ChecksumGenerator;
import java.nio.ByteBuffer;
import org.agrona.concurrent.UnsafeBuffer;

/**
 * 按当前编码格式从 segment 缓冲解出单条记录。
 *
 * <p>SegmentWriter 重放校验与 SegmentReader 顺序读取共用本工具。解析顺序：剩余空间判定 -> 读头部 -> （可选）CRC32C 复核 ->
 * 读体部并对齐索引。除头部越界外，任何失败都会把 position 回退到进入方法前的值；全部通过后 position 停在下一条记录起点。
 */
public final class JournalRecordReaderUtil {

  private final ChecksumGenerator crc = new ChecksumGenerator();
  private final JournalRecordSerializer serializer;

  /**
   * @param serializer 与写盘格式匹配的编解码器
   */
  public JournalRecordReaderUtil(final JournalRecordSerializer serializer) {
    this.serializer = serializer;
  }

  /**
   * 读取并校验（含校验和复核）一条记录。
   *
   * @see #read(ByteBuffer, long, int, boolean)
   */
  public JournalRecord read(
      final ByteBuffer buffer, final long expectedIndex, final int frameLength) {
    return read(buffer, expectedIndex, frameLength, true);
  }

  /**
   * 读取一条记录，可跳过校验和复核。
   *
   * <p>端到端链路已有校验时，热读路径可用 {@code verifyChecksum=false} 省一次 CRC 计算； 恢复与重放扫描必须保持 {@code true}。
   *
   * @param buffer 待解析的缓冲
   * @param expectedIndex 该位置应当出现的日志索引（连续性校验用）
   * @param frameLength 记录前置帧字段占用的字节数
   * @param verifyChecksum 是否重算并比对体部 CRC32C
   * @return 解析完成的记录
   * @throws CorruptedJournalException 剩余字节不足或校验和不一致
   * @throws InvalidIndex 实际索引与 {@code expectedIndex} 不一致
   */
  public JournalRecord read(
      final ByteBuffer buffer,
      final long expectedIndex,
      final int frameLength,
      final boolean verifyChecksum) {
    // 记住进入点，失败路径据此回退
    buffer.mark();
    final int recordOffset = buffer.position();

    final UnsafeBuffer recordView = new UnsafeBuffer(buffer.slice());
    final var header = readHeader(buffer, recordView, recordOffset);
    final int headerLength = serializer.getMetadataLength(recordView, 0);
    final int bodyLength = header.length();

    if (verifyChecksum) {
      assertChecksum(buffer, recordOffset, headerLength, bodyLength, header);
    }

    final JournalRecordData body = serializer.readData(recordView, headerLength);
    if (body != null && body.index() != expectedIndex) {
      buffer.reset();
      throw new InvalidIndex("索引不连续：此处应为 %d，实际为 %d".formatted(expectedIndex, body.index()));
    }

    buffer.position(recordOffset + headerLength + bodyLength);

    // raw 视图按内存地址直接指向映射页；segment 被 unmap 后继续访问会令 JVM 崩溃，
    // 见 https://github.com/camunda/camunda/issues/57609
    final var raw = new UnsafeBuffer(buffer, recordOffset + headerLength, bodyLength);
    return new PersistedJournalRecord(header, body, raw, frameLength + headerLength + bodyLength);
  }

  /** 解码头部并完成两级边界判定：剩余空间须先装下定长头部，头部声明的体部长度加上头部后 不得越出缓冲末尾。两级失败均视为日志损坏（调用方理论上已通过 hasNext() 预检过）。 */
  private JournalRecordMetadata readHeader(
      final ByteBuffer buffer, final UnsafeBuffer recordView, final int recordOffset) {
    final int remaining = buffer.limit() - recordOffset;
    final int fixedHeaderLength = serializer.getMetadataLength();
    if (remaining < fixedHeaderLength) {
      throw new CorruptedJournalException(
          "剩余 %d 字节不足以容纳 %d 字节的记录头，日志已损坏".formatted(remaining, fixedHeaderLength));
    }

    final var header = serializer.readMetadata(recordView, 0);

    final int declaredTotal = serializer.getMetadataLength(recordView, 0) + header.length();
    if (declaredTotal > remaining) {
      throw new CorruptedJournalException(
          "偏移 %d 处的记录声明共 %d 字节（头部信息 %s），超出剩余 %d 字节，记录不完整"
              .formatted(recordOffset, declaredTotal, header, remaining));
    }
    return header;
  }

  /** 对体部重算 CRC32C 并与头部声明比对；不一致则回退 position 后抛出损坏异常。 */
  private void assertChecksum(
      final ByteBuffer buffer,
      final int recordOffset,
      final int headerLength,
      final int bodyLength,
      final JournalRecordMetadata header) {
    final long expected = header.checksum();
    final long actual = crc.compute(buffer, recordOffset + headerLength, bodyLength);
    if (expected != actual) {
      buffer.reset();
      throw new CorruptedJournalException("体部校验和不一致：重算值 %d，头部声明值 %d".formatted(actual, expected));
    }
  }
}
