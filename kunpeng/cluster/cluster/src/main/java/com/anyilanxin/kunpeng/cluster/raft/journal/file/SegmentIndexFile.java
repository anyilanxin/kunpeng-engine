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
package com.anyilanxin.kunpeng.cluster.raft.journal.file;

import com.anyilanxin.kunpeng.cluster.raft.journal.util.ChecksumGenerator;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * segment 稀疏索引的磁盘持久化。
 *
 * <p>文件布局（LITTLE_ENDIAN）：magic "JIDX"(4B) + version(1B) + entryCount(4B) + crc32c(8B) + 索引条目 [(long
 * index, int position) = 12B/条]。写入先落 {@code .tmp} 再原子改名，读取时按 长度、CRC、单调性与取值范围校验，任何不一致都返回 null
 * 交由调用方回退为扫描建索引。
 */
final class SegmentIndexFile {

  private static final int MAGIC = 0x5844_4a49; // "JIDX"
  private static final byte VERSION = 1;
  private static final int HEADER_LENGTH = 17; // magic(4) + version(1) + count(4) + crc(8)
  private static final int ENTRY_LENGTH = 12; // index(8) + position(4)
  private static final int CHECKSUM_OFFSET = 9;
  private static final String TMP_SUFFIX = ".tmp";

  private SegmentIndexFile() {}

  /**
   * 把索引条目原子写入目标文件。
   *
   * @param indexFile 目标 .idx 文件
   * @param entries 按索引升序排列的条目，为空时不产生文件
   * @throws IOException 写入或改名失败
   */
  static void write(final Path indexFile, final List<IndexInfo> entries) throws IOException {
    if (entries.isEmpty()) {
      return;
    }

    final var buffer =
        ByteBuffer.allocate(HEADER_LENGTH + entries.size() * ENTRY_LENGTH)
            .order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(MAGIC).put(VERSION).putInt(entries.size()).putLong(0L);
    for (final var entry : entries) {
      buffer.putLong(entry.index()).putInt(entry.position());
    }

    final var checksum = new ChecksumGenerator();
    buffer.putLong(
        CHECKSUM_OFFSET,
        checksum.compute(buffer, HEADER_LENGTH, buffer.capacity() - HEADER_LENGTH));

    final var tmpFile = indexFile.resolveSibling(indexFile.getFileName() + TMP_SUFFIX);
    buffer.position(0);
    try (final var channel =
        FileChannel.open(
            tmpFile,
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING,
            StandardOpenOption.WRITE)) {
      while (buffer.hasRemaining()) {
        channel.write(buffer);
      }
      channel.force(false);
    }

    try {
      Files.move(
          tmpFile, indexFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    } catch (final AtomicMoveNotSupportedException e) {
      Files.move(tmpFile, indexFile, StandardCopyOption.REPLACE_EXISTING);
    }
  }

  /**
   * 读取并校验索引文件。
   *
   * @param indexFile 目标 .idx 文件
   * @param firstIndex 该 segment 的首日志索引（含）
   * @param lastIndex 该 segment 的末日志索引（含）
   * @param minPosition 合法位置下界（描述符长度）
   * @param maxPosition 合法位置上界（segment 容量）
   * @return 校验通过的条目列表；文件缺失或任何校验失败返回 null
   */
  static @Nullable List<IndexInfo> read(
      final Path indexFile,
      final long firstIndex,
      final long lastIndex,
      final int minPosition,
      final int maxPosition) {
    final byte[] content;
    try {
      content = Files.readAllBytes(indexFile);
    } catch (final IOException e) {
      return null;
    }

    if (content.length < HEADER_LENGTH) {
      return null;
    }

    final var buffer = ByteBuffer.wrap(content).order(ByteOrder.LITTLE_ENDIAN);
    if (buffer.getInt(0) != MAGIC || buffer.get(4) != VERSION) {
      return null;
    }

    final var entryCount = buffer.getInt(5);
    if (entryCount <= 0 || content.length != HEADER_LENGTH + entryCount * ENTRY_LENGTH) {
      return null;
    }

    final var checksum = new ChecksumGenerator();
    if (checksum.compute(buffer, HEADER_LENGTH, content.length - HEADER_LENGTH)
        != buffer.getLong(CHECKSUM_OFFSET)) {
      return null;
    }

    final List<IndexInfo> entries = new ArrayList<>(entryCount);
    var previousIndex = Long.MIN_VALUE;
    for (var i = 0; i < entryCount; i++) {
      final var offset = HEADER_LENGTH + i * ENTRY_LENGTH;
      final var index = buffer.getLong(offset);
      final var position = buffer.getInt(offset + 8);
      if (index <= previousIndex || index < firstIndex || index > lastIndex) {
        return null;
      }
      if (position < minPosition || position >= maxPosition) {
        return null;
      }
      entries.add(new IndexInfo(index, position));
      previousIndex = index;
    }
    return entries;
  }
}
