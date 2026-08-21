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
package io.atomix.raft.journal.file;

import java.nio.MappedByteBuffer;

/**
 * 预创建但尚未写入描述符的 segment。
 *
 * <p>为降低滚动新 segment 时的停顿，管理器会提前在后台创建文件并完成空间分配，但此时文件里
 * 还没有 {@link SegmentDescriptor}——描述符要等到真正启用该 segment 时才知道起始索引，故此
 * 刻只保留文件句柄、映射缓冲等"半成品"材料。
 *
 * @param file segment 文件
 * @param segmentId segment 编号
 * @param maxSegmentSize segment 最大字节数
 * @param buffer 已完成空间分配的内存映射
 * @param journalIndex 该 segment 启用后挂接的稀疏索引
 */
record UninitializedSegment(
    SegmentFile file,
    long segmentId,
    int maxSegmentSize,
    MappedByteBuffer buffer,
    JournalIndex journalIndex) {

  /**
   * 写入描述符，把半成品 segment 升格为可用的 {@link Segment}。
   *
   * @param index 该 segment 的起始日志索引
   * @param lastWrittenAsqn 启用前全局已写入的最大应用层序号
   * @param metrics 日志指标
   * @return 初始化完成的 segment
   */
  public Segment initializeForUse(
      final long index, final long lastWrittenAsqn, final JournalMetrics metrics) {
    final var descriptorForUse =
        SegmentDescriptor.builder()
            .withId(segmentId)
            .withIndex(index)
            .withMaxSegmentSize(maxSegmentSize)
            .build();

    final var serializer = SegmentDescriptorSerializer.currentSerializer();
    serializer.writeTo(descriptorForUse, buffer);

    return new Segment(
        file, descriptorForUse, serializer, buffer, lastWrittenAsqn, journalIndex, metrics);
  }
}
