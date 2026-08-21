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

import static java.util.Objects.requireNonNull;

import com.anyilanxin.kunpeng.utils.FileUtil;
import io.atomix.raft.journal.CorruptedJournalException;
import io.atomix.raft.journal.JournalException;
import java.io.FileDescriptor;
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
 * segment 文件的物理层装配器。
 *
 * <p>职责仅限磁盘与内存映射操作：创建新 segment 文件（含空间预分配、描述符落盘与目录
 * fsync）、加载既有 segment 文件（含按描述符声明大小重映射）。segment 的选择与追踪由
 * SegmentsManager 负责。
 */
final class SegmentLoader {

  private static final Logger LOG = LoggerFactory.getLogger(SegmentLoader.class);
  private static final ByteOrder SEGMENT_BYTE_ORDER = ByteOrder.LITTLE_ENDIAN;

  private final SegmentAllocator diskAllocator;
  private final long diskSpaceThreshold;
  private final JournalMetrics metrics;

  SegmentLoader(
      final long minFreeDiskSpace, final JournalMetrics metrics, final SegmentAllocator allocator) {
    this.diskSpaceThreshold = minFreeDiskSpace;
    this.metrics = metrics;
    this.diskAllocator = allocator;
  }

  /**
   * 创建一个立即可用的 segment：分配文件、写入并落盘描述符。
   *
   * @param segmentFile 目标文件路径
   * @param descriptor 待写入的描述符
   * @param lastWrittenAsqn 启用前全局已写入的最大应用层序号
   * @param journalIndex 该 segment 挂接的稀疏索引
   * @return 就绪的 segment
   */
  Segment createSegment(
      final Path segmentFile,
      final SegmentDescriptor descriptor,
      final long lastWrittenAsqn,
      final JournalIndex journalIndex) {
    final var serializer = SegmentDescriptorSerializer.currentSerializer();
    final MappedByteBuffer mapped = allocateMappedFile(segmentFile, descriptor);

    try {
      serializer.writeTo(descriptor, mapped);
      mapped.force();
    } catch (final InternalError e) {
      // force 可能因映射失效抛出 InternalError，此时需回滚
      throw new JournalException(
          String.format(
              "Failed to ensure durability of segment %s with descriptor %s, rolling back",
              segmentFile, descriptor),
          e);
    }

    syncParentDirectory(segmentFile);

    return assembleSegment(segmentFile, mapped, descriptor, serializer, lastWrittenAsqn, journalIndex);
  }

  /**
   * 创建一个仅完成文件与空间分配、尚未写入描述符的 segment。
   *
   * <p>描述符的起始索引要到真正启用时才确定，故此处只准备"半成品"。
   */
  UninitializedSegment createUninitializedSegment(
      final Path segmentFile, final SegmentDescriptor descriptor, final JournalIndex journalIndex) {
    final MappedByteBuffer mapped = allocateMappedFile(segmentFile, descriptor);

    syncParentDirectory(segmentFile);

    return new UninitializedSegment(
        new SegmentFile(segmentFile.toFile()),
        descriptor.id(),
        descriptor.maxSegmentSize(),
        mapped,
        journalIndex);
  }

  /**
   * 加载磁盘上已存在的 segment 文件。
   *
   * <p>先按文件当前大小建立映射并读出描述符，若描述符声明的 segment 上限更大，则解除
   * 映射并按声明大小重新映射。
   */
  Segment loadExistingSegment(
      final Path segmentFile, final long lastWrittenAsqn, final JournalIndex journalIndex) {
    final var serializer = SegmentDescriptorSerializer.currentSerializer();
    try (final var channel =
        FileChannel.open(segmentFile, StandardOpenOption.READ, StandardOpenOption.WRITE)) {
      final long fileSizeOnDisk = Files.size(segmentFile);
      MappedByteBuffer mapped = mapReadableWritable(channel, fileSizeOnDisk);
      final var descriptor =
          readDescriptor(serializer, mapped, segmentFile.getFileName().toString());

      if (descriptor.maxSegmentSize() > fileSizeOnDisk) {
        IoUtil.unmap(mapped);
        mapped = mapReadableWritable(channel, descriptor.maxSegmentSize());
      }

      return assembleSegment(
          segmentFile, mapped, descriptor, serializer, lastWrittenAsqn, journalIndex);
    } catch (final IOException e) {
      throw new JournalException(
          String.format("Failed to load existing segment %s", segmentFile), e);
    }
  }

  /* ---------- 以下为装配与映射的内部步骤 ---------- */

  private Segment assembleSegment(
      final Path file,
      final MappedByteBuffer buffer,
      final SegmentDescriptor descriptor,
      final SegmentDescriptorSerializer serializer,
      final long lastWrittenAsqn,
      final JournalIndex journalIndex) {
    return new Segment(
        new SegmentFile(file.toFile()),
        descriptor,
        serializer,
        buffer,
        lastWrittenAsqn,
        journalIndex,
        metrics);
  }

  private MappedByteBuffer mapReadableWritable(final FileChannel channel, final long segmentSize)
      throws IOException {
    final var mapped = channel.map(MapMode.READ_WRITE, 0, segmentSize);
    mapped.order(SEGMENT_BYTE_ORDER);
    return mapped;
  }

  private SegmentDescriptor readDescriptor(
      final SegmentDescriptorSerializer serializer, final ByteBuffer buffer, final String fileName) {
    try {
      return serializer.readFrom(buffer);
    } catch (final IndexOutOfBoundsException e) {
      throw new JournalException(
          String.format(
              "Expected to read descriptor of segment '%s', but nothing was read.", fileName),
          e);
    } catch (final UnknownVersionException e) {
      throw new CorruptedJournalException(
          String.format("Couldn't read or recognize version of segment '%s'.", fileName), e);
    }
  }

  /** 创建全新文件、预分配空间并返回其读写映射。 */
  private MappedByteBuffer allocateMappedFile(
      final Path segmentPath, final SegmentDescriptor descriptor) throws JournalException {
    final int targetSize = descriptor.maxSegmentSize();
    ensureEnoughDiskSpace(segmentPath, targetSize);

    try {
      Files.createFile(segmentPath);
    } catch (final FileAlreadyExistsException e) {
      // 残留的未使用文件直接删除后重建
      LOG.warn(
          "Failed to create segment {}: an unused file already existed, and will be replaced",
          segmentPath,
          e);
      try {
        Files.delete(segmentPath);
      } catch (final IOException deleteFailed) {
        throw new JournalException(
            String.format("Failed to replace existing segment file %s", segmentPath),
            deleteFailed);
      }
      return allocateMappedFile(segmentPath, descriptor);
    } catch (final IOException e) {
      throw new JournalException(
          String.format("Failed to create new segment file %s", segmentPath), e);
    }

    try (final var raf = new RandomAccessFile(segmentPath.toFile(), "rw");
        final var channel = raf.getChannel(); ) {
      preallocate(targetSize, channel, raf.getFD());
      raf.setLength(targetSize);
      return mapReadableWritable(channel, targetSize);
    } catch (final IOException e) {
      throw new JournalException(
          String.format("Failed to create new segment file %s", segmentPath), e);
    }
  }

  /** 磁盘可用空间不足以容纳新 segment（或低于安全水位）时直接失败。 */
  private void ensureEnoughDiskSpace(final Path segmentPath, final int targetSize) {
    final var parent =
        requireNonNull(
            segmentPath.getParent(),
            () -> String.format("Expected file %s to have a parent but it was null", segmentPath));
    final var usableBytes = parent.toFile().getUsableSpace();
    final var requiredBytes = Math.max(targetSize, diskSpaceThreshold);
    if (usableBytes < requiredBytes) {
      throw new JournalException.OutOfDiskSpace(
          "Not enough space to allocate a new journal segment. Required: %s, Available: %s"
              .formatted(requiredBytes, usableBytes));
    }
  }

  private void preallocate(
      final int targetSize, final FileChannel channel, final FileDescriptor fileDescriptor)
      throws IOException {
    try (final var ignored = metrics.observeSegmentAllocation()) {
      diskAllocator.allocate(channel, fileDescriptor, targetSize);
    }
  }

  /**
   * 刷盘父目录，确保新创建的文件在崩溃恢复后依然作为目录项可见（仅 force 文件内容不够）。
   */
  private void syncParentDirectory(final Path segmentFile) {
    try {
      FileUtil.flushDirectory(segmentFile.getParent());
    } catch (final IOException e) {
      throw new JournalException(
          String.format("Failed to flush journal directory after creating segment %s", segmentFile),
          e);
    }
  }
}
