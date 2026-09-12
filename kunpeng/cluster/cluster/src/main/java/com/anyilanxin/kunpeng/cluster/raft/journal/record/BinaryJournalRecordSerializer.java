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

import com.anyilanxin.kunpeng.structpack.buffer.BufferWriter;
import com.anyilanxin.kunpeng.utils.Either;
import java.nio.BufferOverflowException;
import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;

/**
 * 日志记录的二进制序列化器，编码逻辑委托给 SBE 生成的 {@link JournalRecordMetadataCodec} 与 {@link JournalRecordDataCodec}。
 */
public final class BinaryJournalRecordSerializer implements JournalRecordSerializer {

  /** 当前元数据帧为固定长度，任何合法位置的元数据长度均相同。 */
  @Override
  public int getMetadataLength() {
    return JournalRecordMetadataCodec.FRAME_LENGTH;
  }

  /** 先校验元数据帧的合法性，再返回其固定长度。 */
  @Override
  public int getMetadataLength(final DirectBuffer buffer, final int offset) {
    JournalRecordMetadataCodec.validate(buffer, offset);
    return JournalRecordMetadataCodec.FRAME_LENGTH;
  }

  /** 把记录数据（索引、序列号与业务写入器内容）编码进缓冲区指定偏移。 */
  @Override
  public Either<BufferOverflowException, Integer> writeData(
      final long index,
      final long asqn,
      final BufferWriter recordDataWriter,
      final MutableDirectBuffer writeBuffer,
      final int offset) {
    return JournalRecordDataCodec.write(index, asqn, recordDataWriter, writeBuffer, offset);
  }

  /** 按指定版本写入记录数据；由于线格式目前只有一个版本，版本参数被忽略，直接按当前布局写出。 */
  @Override
  public Either<BufferOverflowException, Integer> writeDataAtVersion(
      final int version,
      final long index,
      final long asqn,
      final BufferWriter recordDataWriter,
      final MutableDirectBuffer writeBuffer,
      final int offset) {
    return writeData(index, asqn, recordDataWriter, writeBuffer, offset);
  }

  /** 把元数据（校验和与长度）编码进缓冲区指定偏移。 */
  @Override
  public int writeMetadata(
      final JournalRecordMetadata metadata, final MutableDirectBuffer buffer, final int offset) {
    return JournalRecordMetadataCodec.write(buffer, offset, metadata.checksum(), metadata.length());
  }

  /** 从缓冲区指定偏移解析记录元数据。 */
  @Override
  public JournalRecordMetadata readMetadata(final DirectBuffer buffer, final int offset) {
    return JournalRecordMetadataCodec.read(buffer, offset);
  }

  /** 从缓冲区指定偏移解析记录数据。 */
  @Override
  public JournalRecordData readData(final DirectBuffer buffer, final int offset) {
    return JournalRecordDataCodec.read(buffer, offset);
  }
}
