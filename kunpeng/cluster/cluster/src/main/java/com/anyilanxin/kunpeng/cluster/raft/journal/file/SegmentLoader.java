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

import static java.util.Objects.requireNonNull;

import com.anyilanxin.kunpeng.cluster.raft.journal.CorruptedJournalException;
import com.anyilanxin.kunpeng.cluster.raft.journal.JournalException;
import com.anyilanxin.kunpeng.utils.FilePreallocator;
import com.anyilanxin.kunpeng.utils.FileUtil;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import org.agrona.IoUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * segment 文件的磁盘装配层。
 *
 * <p>只关心文件与内存映射两件事：新建 segment（空间预分配、描述符写入与目录刷盘）和打开 既有 segment（按描述符声明的大小对齐映射）。挑选、追踪与滚动 segment 是
 * SegmentsManager 的职责，与本类无关。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
final class SegmentLoader {

  private static final Logger LOG = LoggerFactory.getLogger(SegmentLoader.class);

  /** segment 全部多字节字段均按小端编码。 */
  private static final ByteOrder SEGMENT_BYTE_ORDER = ByteOrder.LITTLE_ENDIAN;

  /** 建段时同名残留文件“删除后重建”的最大尝试次数；目录锁已排除并发写者，超限视为病态场景直接失败。 */
  private static final int MAX_RECREATE_ATTEMPTS = 3;

  private final long minFreeDiskBytes;
  private final JournalMetrics metrics;

  SegmentLoader(final long minFreeDiskSpace, final JournalMetrics metrics) {
    this.minFreeDiskBytes = minFreeDiskSpace;
    this.metrics = metrics;
  }

  /**
   * 新建一个可直接写入的 segment：建文件、预分配、写入描述符并保证描述符落盘。
   *
   * @param segmentFile 目标文件
   * @param descriptor 随文件落盘的描述符
   * @param lastWrittenAsqn 本 segment 启用前全局已见的最大应用层序号
   * @param journalIndex 挂接的稀疏索引
   * @return 就绪的 segment
   */
  Segment createSegment(
      final Path segmentFile,
      final SegmentDescriptor descriptor,
      final long lastWrittenAsqn,
      final JournalIndex journalIndex) {
    final var serializer = SegmentDescriptorSerializer.currentSerializer();
    final MappedByteBuffer mapped = createMappedFile(segmentFile, descriptor);

    try {
      serializer.writeTo(descriptor, mapped);
      mapped.force();
    } catch (final InternalError e) {
      // 映射被撤销时 force 会抛 InternalError，此处统一转成受控异常上抛
      throw new JournalException(
          "segment %s 的描述符 %s 写入后无法确保落盘".formatted(segmentFile, descriptor), e);
    }

    syncDirectory(segmentFile);

    return wireSegment(segmentFile, mapped, descriptor, serializer, lastWrittenAsqn, journalIndex);
  }

  /**
   * 打开磁盘上已有的 segment 文件。
   *
   * <p>先按文件实际大小建立映射并解出描述符；若描述符声明的容量大于当前文件，则撤销原映射、 按声明容量重新映射（文件在建段时已预分配到该容量）。
   */
  Segment loadExistingSegment(
      final Path segmentFile, final long lastWrittenAsqn, final JournalIndex journalIndex) {
    final var serializer = SegmentDescriptorSerializer.currentSerializer();
    try (final var channel =
        FileChannel.open(segmentFile, StandardOpenOption.READ, StandardOpenOption.WRITE)) {
      final long fileSize = Files.size(segmentFile);
      MappedByteBuffer mapped = mapSegment(channel, fileSize);
      final var descriptor = decodeDescriptor(serializer, mapped, fileNameOf(segmentFile));

      if (descriptor.maxSegmentSize() > fileSize) {
        IoUtil.unmap(mapped);
        mapped = mapSegment(channel, descriptor.maxSegmentSize());
      }

      return wireSegment(
          segmentFile, mapped, descriptor, serializer, lastWrittenAsqn, journalIndex);
    } catch (final IOException e) {
      throw new JournalException("打开既有 segment %s 失败".formatted(segmentFile), e);
    }
  }

  /* ---------- 内部步骤 ---------- */

  private static String fileNameOf(final Path file) {
    return file.getFileName().toString();
  }

  private Segment wireSegment(
      final Path file,
      final MappedByteBuffer mapped,
      final SegmentDescriptor descriptor,
      final SegmentDescriptorSerializer serializer,
      final long lastWrittenAsqn,
      final JournalIndex journalIndex) {
    final var segmentFile = new SegmentFile(file.toFile());
    return new Segment(
        segmentFile, descriptor, serializer, mapped, lastWrittenAsqn, journalIndex, metrics);
  }

  private MappedByteBuffer mapSegment(final FileChannel channel, final long segmentSize)
      throws IOException {
    final var mapped = channel.map(MapMode.READ_WRITE, 0, segmentSize);
    mapped.order(SEGMENT_BYTE_ORDER);
    return mapped;
  }

  private SegmentDescriptor decodeDescriptor(
      final SegmentDescriptorSerializer serializer,
      final ByteBuffer buffer,
      final String fileName) {
    try {
      return serializer.readFrom(buffer);
    } catch (final UnknownVersionException e) {
      throw new CorruptedJournalException("segment '%s' 的描述符格式版本无法识别".formatted(fileName), e);
    } catch (final IndexOutOfBoundsException e) {
      throw new JournalException("segment '%s' 连一个完整描述符都读不出来".formatted(fileName), e);
    }
  }

  /** 建立全新文件、完成空间预分配并返回读写映射；遇到同名残留文件则先清掉再重建。 */
  private MappedByteBuffer createMappedFile(
      final Path segmentPath, final SegmentDescriptor descriptor) {
    final int segmentBytes = descriptor.maxSegmentSize();
    checkDiskSpace(segmentPath, segmentBytes);

    // 残留同名文件只可能是上次未及清理的产物；上界防御病态重建场景（NFS silly-rename、备份进程等），避免无界自旋
    int recreateAttempts = 0;
    while (true) {
      try {
        Files.createFile(segmentPath);
        break;
      } catch (final FileAlreadyExistsException e) {
        if (++recreateAttempts > MAX_RECREATE_ATTEMPTS) {
          throw new JournalException("segment 文件 %s 删除后仍反复存在，疑似目录锁之外的写者".formatted(segmentPath), e);
        }
        LOG.warn("segment 文件 {} 已存在，按残留文件删除后重建（第 {} 次尝试）", segmentPath, recreateAttempts, e);
        try {
          Files.delete(segmentPath);
        } catch (final IOException deleteFailure) {
          throw new JournalException(
              "残留 segment 文件 %s 删除失败，无法重建".formatted(segmentPath), deleteFailure);
        }
      } catch (final IOException e) {
        throw new JournalException("创建 segment 文件 %s 失败".formatted(segmentPath), e);
      }
    }

    try (final var file = new RandomAccessFile(segmentPath.toFile(), "rw");
        final var channel = file.getChannel()) {
      reserveSpace(segmentPath, segmentBytes);
      return mapSegment(channel, segmentBytes);
    } catch (final IOException e) {
      throw new JournalException(
          "segment 文件 %s 预分配空间失败（目标 %d 字节）".formatted(segmentPath, segmentBytes), e);
    }
  }

  /** 可用磁盘空间低于“新段容量与安全水位二者较大值”时拒绝分配。 */
  private void checkDiskSpace(final Path segmentPath, final int segmentBytes) {
    final var parent =
        requireNonNull(segmentPath.getParent(), () -> "路径 %s 没有父目录".formatted(segmentPath));
    final long usable = parent.toFile().getUsableSpace();
    final long needed = Math.max(segmentBytes, minFreeDiskBytes);
    if (usable < needed) {
      throw new JournalException.OutOfDiskSpace(
          "磁盘空间不足，无法分配新 segment：需要 %d 字节，可用 %d 字节".formatted(needed, usable));
    }
  }

  private void reserveSpace(final Path segmentPath, final int segmentBytes) {
    try (final var ignored = metrics.observeSegmentAllocation()) {
      FilePreallocator.preallocate(segmentPath, segmentBytes);
    } catch (final IOException e) {
      throw new JournalException(
          "segment 文件 %s 预分配空间失败（目标 %d 字节）".formatted(segmentPath, segmentBytes), e);
    }
  }

  /** 刷盘父目录，让新建文件作为目录项持久可见；仅 force 文件内容不足以保证崩溃后仍可见。 */
  private void syncDirectory(final Path segmentFile) {
    try {
      FileUtil.flushDirectory(segmentFile.getParent());
    } catch (final IOException e) {
      throw new JournalException("segment %s 建立后刷盘其所在目录失败".formatted(segmentFile), e);
    }
  }
}
