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
import com.anyilanxin.kunpeng.structpack.buffer.DirectBufferWriter;
import com.anyilanxin.kunpeng.utils.Either;
import java.nio.BufferOverflowException;
import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;

/**
 * 日志记录在 segment 缓冲上的编解码契约。
 *
 * <p>一条记录按“头部（定长）+ 体部（变长）”两段编码；本接口同时覆盖两段的双向转换。头部 长度恒定，由 {@link #getMetadataLength()} 给出。
 */
public interface JournalRecordSerializer {

  /**
   * 把 {@code entry} 的体部写入 buffer。
   *
   * <p>等价于用 {@link DirectBufferWriter} 包装负载后走 {@link #writeData(long, long,
   * BufferWriter, MutableDirectBuffer, int)}。
   *
   * @param entry 提供索引、序号与负载的记录体
   * @param buffer 写入目标
   * @param offset 写入起点
   * @return 空间不足时返回 {@link BufferOverflowException}，否则返回写入字节数
   */
  default Either<BufferOverflowException, Integer> writeData(
      final JournalRecordData entry, final MutableDirectBuffer buffer, final int offset) {
    final DirectBufferWriter payload = DirectBufferWriter.writerFor(entry.data());
    return writeData(entry.index(), entry.asqn(), payload, buffer, offset);
  }

  /**
   * 以流式写入器为负载来源编码体部（免去中间拷贝）。
   *
   * @param index 该记录的日志索引
   * @param asqn 应用层序号
   * @param payload 负载写入器
   * @param buffer 写入目标
   * @param offset 写入起点
   * @return 空间不足时返回 {@link BufferOverflowException}，否则返回写入字节数
   */
  Either<BufferOverflowException, Integer> writeData(
      long index,
      long asqn,
      BufferWriter payload,
      MutableDirectBuffer buffer,
      int offset);

  /**
   * 写入头部。字节数恒为 {@link #getMetadataLength()}。
   *
   * @return 实际写入字节数
   */
  int writeMetadata(JournalRecordMetadata metadata, MutableDirectBuffer buffer, int offset);

  /** @return 当前编码版本下头部的固定字节数 */
  int getMetadataLength();

  /**
   * 从 buffer 的 {@code offset} 处解码头部。
   *
   * <p>调用方须保证该处存在完整帧；否则实现抛 {@link
   * com.anyilanxin.kunpeng.cluster.raft.journal.CorruptedJournalException}。
   */
  JournalRecordMetadata readMetadata(DirectBuffer buffer, int offset);

  /**
   * 从 buffer 的 {@code offset} 处解码体部（{@code offset} 应紧跟头部之后）。
   *
   * <p>调用方须保证该处存在完整帧；否则实现抛 {@link
   * com.anyilanxin.kunpeng.cluster.raft.journal.CorruptedJournalException}。
   */
  JournalRecordData readData(DirectBuffer buffer, int offset);

  /** @return buffer 中实际编码出的头部长度（与版本相关时以缓冲内容为准） */
  int getMetadataLength(DirectBuffer buffer, int offset);
}
