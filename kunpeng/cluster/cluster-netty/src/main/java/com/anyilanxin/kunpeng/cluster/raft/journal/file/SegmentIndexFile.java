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
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.CRC32C;
import org.agrona.IoUtil;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * segment 稀疏索引的 mmap 持久化（Kafka offset index 风格）。
 *
 * <p>文件布局（LITTLE_ENDIAN）：magic "JIDX"(4B) + version(1B) + entryCount(4B) + crc32c(8B) + 索引条目 [(long
 * index, int position) = 12B/条]。文件建段时按容量公式一次性预分配（稀疏文件，不实际占盘）， 整个生命周期内只追加、不重写、无 tmp/rename/fsync：更新
 * = 追加条目后推进头部提交点（先写 crc 再写 count，count 即提交字）。
 *
 * <p>崩溃一致性：恢复时只信任头部 count 覆盖的前缀（CRC + 单调性 + 范围校验），提交点之后的 尾部字节直接忽略、下次覆写；头部撕裂则整文件作废，回退为扫描建索引。
 *
 * <p>容量公式：单条日志最小占用 27B（外帧 1 + 数据帧头 25 + 最小 payload 1），段内最多 maxSegmentSize/27 条记录，除以 density
 * 即最多条目数，另以 1MB 文件上限 clamp——索引本就是稀疏加速器， 容量耗尽时退化为更稀疏。
 */
final class SegmentIndexFile implements AutoCloseable {
  private static final Logger LOG = LoggerFactory.getLogger(SegmentIndexFile.class);

  private static final int MAGIC = 0x5844_4a49; // "JIDX"
  private static final byte VERSION = 1;
  private static final int HEADER_LENGTH = 17; // magic(4) + version(1) + count(4) + crc(8)
  private static final int ENTRY_LENGTH = 12; // index(8) + position(4)
  private static final int CHECKSUM_OFFSET = 9;
  private static final int COUNT_OFFSET = 5;

  /** 单条日志记录的最小字节占用：外帧(1) + 数据帧头(25) + 最小 payload(1)。 */
  private static final int MIN_RECORD_BYTES = 27;

  /** idx 文件容量硬上限（约 1MB），防止 density 极小时预分配失控。 */
  private static final int MAX_FILE_BYTES = 1024 * 1024;

  private static final ByteOrder ENDIANNESS = ByteOrder.LITTLE_ENDIAN;

  private final MappedByteBuffer buffer;

  /** 已提交前缀的增量 CRC 状态；追加条目后续算，无需全量重算。 */
  private final CRC32C crc = new CRC32C();

  private final int capacity;
  private int committedCount;

  private SegmentIndexFile(final MappedByteBuffer buffer, final int capacity) {
    this.buffer = buffer;
    this.capacity = capacity;
  }

  /* ---------- 容量 ---------- */

  /**
   * @return 由段容量与索引密度推出的单段索引条目容量（含 1MB 文件 clamp）
   */
  static int capacityEntries(final int maxSegmentSize, final int density) {
    final long maxRecords = (long) maxSegmentSize / MIN_RECORD_BYTES;
    final long byDensity = maxRecords / Math.max(1, density) + 1;
    return (int) Math.min(byDensity, (long) (MAX_FILE_BYTES - HEADER_LENGTH) / ENTRY_LENGTH);
  }

  /**
   * @return 预分配文件长度
   */
  static int fileBytes(final int capacityEntries) {
    return HEADER_LENGTH + capacityEntries * ENTRY_LENGTH;
  }

  /* ---------- 打开与创建 ---------- */

  /**
   * 打开（或新建）索引文件的写入映射。
   *
   * <p>文件已存在时信任头部 count 续接（调用方应先经 {@link #read} 校验并删除非法文件）；缺失或 过小则扩展到预分配长度并写出空头部。头部不合法时自愈为空索引。
   *
   * @param indexFile 目标 .idx 文件
   * @param capacityEntries 期望条目容量，实际映射长度更大时以文件为准
   * @throws IOException 打开、扩展或映射失败
   */
  static SegmentIndexFile openOrCreate(final Path indexFile, final int capacityEntries)
      throws IOException {
    final int minBytes = fileBytes(capacityEntries);
    try (final var channel =
        FileChannel.open(
            indexFile,
            StandardOpenOption.CREATE,
            StandardOpenOption.READ,
            StandardOpenOption.WRITE)) {
      final boolean fresh = channel.size() == 0;
      if (channel.size() < minBytes) {
        // truncate 无法扩容，写入尾字节触发稀疏扩展
        channel.write(ByteBuffer.wrap(new byte[1]), minBytes - 1);
      }

      final long length = Math.max(channel.size(), minBytes);
      final MappedByteBuffer buffer = channel.map(FileChannel.MapMode.READ_WRITE, 0, length);
      buffer.order(ENDIANNESS);
      final int capacity = (int) (length - HEADER_LENGTH) / ENTRY_LENGTH;
      final var index = new SegmentIndexFile(buffer, capacity);

      final boolean headerValid = !fresh && buffer.getInt(0) == MAGIC && buffer.get(4) == VERSION;
      final int count = headerValid ? buffer.getInt(COUNT_OFFSET) : -1;
      if (headerValid && count >= 0 && HEADER_LENGTH + (long) count * ENTRY_LENGTH <= length) {
        index.committedCount = count;
        index.seedCrc(count);
      } else {
        index.writeHeader();
      }
      return index;
    }
  }

  /** 为已提交前缀重建增量 CRC 状态（打开既有文件时调用）。 */
  private void seedCrc(final int count) {
    crc.reset();
    final var view = buffer.duplicate();
    view.position(HEADER_LENGTH).limit(HEADER_LENGTH + count * ENTRY_LENGTH);
    crc.update(view);
  }

  /** 写出空头部：magic + version + count=0 + 空 CRC。 */
  private void writeHeader() {
    crc.reset();
    buffer.putInt(0, MAGIC).put(4, VERSION).putInt(COUNT_OFFSET, 0).putLong(CHECKSUM_OFFSET, 0L);
    committedCount = 0;
  }

  /* ---------- 增量持久化 ---------- */

  /**
   * 追加 {@code entries} 中超出已提交前缀的条目并推进提交点。
   *
   * <p>条目数比已提交前缀少（日志截断）时全量重写；超出容量时截到容量为止（索引退化为更稀疏）。
   *
   * @param entries 该段按索引升序的全量条目
   */
  void persist(final List<IndexInfo> entries) {
    if (entries.size() < committedCount) {
      rewrite(entries);
      return;
    }

    final var entryBuffer = ByteBuffer.allocate(ENTRY_LENGTH).order(ENDIANNESS);
    final int newCount = Math.min(entries.size(), capacity);
    for (var i = committedCount; i < newCount; i++) {
      final var entry = entries.get(i);
      final int offset = HEADER_LENGTH + i * ENTRY_LENGTH;
      entryBuffer.clear().putLong(entry.index()).putInt(entry.position());
      buffer.put(offset, entryBuffer.array(), 0, ENTRY_LENGTH);
      crc.update(entryBuffer.array(), 0, ENTRY_LENGTH);
    }
    commitHeader(newCount);
  }

  /** 全量重写（截断自愈）：从零重放全部条目。 */
  private void rewrite(final List<IndexInfo> entries) {
    final var entryBuffer = ByteBuffer.allocate(ENTRY_LENGTH).order(ENDIANNESS);
    final int newCount = Math.min(entries.size(), capacity);
    crc.reset();
    for (var i = 0; i < newCount; i++) {
      final var entry = entries.get(i);
      final int offset = HEADER_LENGTH + i * ENTRY_LENGTH;
      entryBuffer.clear().putLong(entry.index()).putInt(entry.position());
      buffer.put(offset, entryBuffer.array(), 0, ENTRY_LENGTH);
      crc.update(entryBuffer.array(), 0, ENTRY_LENGTH);
    }
    committedCount = 0;
    commitHeader(newCount);
  }

  /** 推进头部提交点：先写 crc 再写 count，count 为提交字。 */
  private void commitHeader(final int count) {
    buffer.putLong(CHECKSUM_OFFSET, crc.getValue());
    buffer.putInt(COUNT_OFFSET, count);
    committedCount = count;
  }

  int committedCount() {
    return committedCount;
  }

  @Override
  public void close() {
    try {
      buffer.force();
    } catch (final Exception e) {
      // 索引只是加速缓存，关闭时 force 失败不影响正确性
      LOG.debug("Failed to force segment index file when closing", e);
    }
    IoUtil.unmap(buffer);
  }

  /* ---------- 读取与校验 ---------- */

  /**
   * 读取并校验索引文件的已提交前缀。
   *
   * <p>预分配使文件可长于头部 count 覆盖的范围，多余尾部字节直接忽略。
   *
   * @param indexFile 目标 .idx 文件
   * @param firstIndex 该 segment 的首日志索引（含）
   * @param lastIndex 该 segment 的末日志索引（含）
   * @param minPosition 合法位置下界（描述符长度）
   * @param maxPosition 合法位置上界（segment 容量）
   * @return 校验通过的条目列表（空索引返回空列表）；文件缺失或任何校验失败返回 null
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

    final var buffer = ByteBuffer.wrap(content).order(ENDIANNESS);
    if (buffer.getInt(0) != MAGIC || buffer.get(4) != VERSION) {
      return null;
    }

    final var entryCount = buffer.getInt(COUNT_OFFSET);
    if (entryCount < 0 || content.length < HEADER_LENGTH + entryCount * ENTRY_LENGTH) {
      return null;
    }

    final var checksum = new ChecksumGenerator();
    if (checksum.compute(buffer, HEADER_LENGTH, entryCount * ENTRY_LENGTH)
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
