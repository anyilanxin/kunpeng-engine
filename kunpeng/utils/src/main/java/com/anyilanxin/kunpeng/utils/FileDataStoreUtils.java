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
package com.anyilanxin.kunpeng.utils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.Optional;
import java.util.zip.CRC32C;

/**
 * 带版本与 CRC32C 校验头的崩溃安全文件存取。文件格式：版本(1B) + CRC32C 校验(8B) + body。
 *
 * <ul>
 *   <li>写：gather-write（头直写 + body 零拷贝 wrap，单 syscall 单 DSYNC），无需整体拼接 HEADER+body 的大数组
 *   <li>读：大文件（阈值以上）走 mmap 只读映射，校验直接在映射上计算（零堆拷贝），仅返回 body 时拷贝一次， 读毕立即解除映射
 *   <li>小文件（阈值以下）单通道读入，原位校验，单次拷出
 * </ul>
 *
 * <p>崩溃安全策略：同目录 {@code <file>.tmp} + DSYNC 落盘 → 原子 rename 覆盖并 fsync 父目录；目标文件任意时刻要么完整旧版要么完整新版。
 */
public final class FileDataStoreUtils {

  private static final byte VERSION = 1;

  /** 版本(1B) + CRC32C(8B) */
  private static final int HEADER_LENGTH = 9;

  /** 超过该字节数走 mmap 读路径 */
  private static final long MMAP_THRESHOLD_BYTES = 64 * 1024;

  private static final String TEMP_FILE_SUFFIX = ".tmp";

  private FileDataStoreUtils() {}

  /** 原子写入（版本头 + CRC32C 校验头 + body）；失败时清理临时文件并抛 {@link UncheckedIOException} */
  public static void writeToFile(final Path targetFile, final byte[] body) {
    final Path tempFile = targetFile.resolveSibling(targetFile.getFileName() + TEMP_FILE_SUFFIX);
    try {
      try (final FileChannel channel =
          FileChannel.open(
              tempFile,
              StandardOpenOption.CREATE,
              StandardOpenOption.WRITE,
              StandardOpenOption.TRUNCATE_EXISTING,
              StandardOpenOption.DSYNC)) {
        final ByteBuffer header = ByteBuffer.allocate(HEADER_LENGTH).order(ByteOrder.LITTLE_ENDIAN);
        header.put(VERSION);
        header.putLong(checksum(body, 0, body.length));
        header.flip();
        // gather-write: 头 + body 零拷贝, 单次 syscall; 循环兜底部分写
        final ByteBuffer[] buffers = {header, ByteBuffer.wrap(body)};
        while (buffers[0].hasRemaining() || buffers[1].hasRemaining()) {
          channel.write(buffers);
        }
      }
      moveDurably(tempFile, targetFile);
    } catch (final IOException e) {
      try {
        Files.deleteIfExists(tempFile);
      } catch (final IOException ignored) {
        // 清理失败不影响主错误上抛
      }
      throw new UncheckedIOException(e);
    }
  }

  private static void moveDurably(final Path tempFile, final Path targetFile) throws IOException {
    try {
      FileUtil.moveDurably(
          tempFile,
          targetFile,
          StandardCopyOption.ATOMIC_MOVE,
          StandardCopyOption.REPLACE_EXISTING);
    } catch (final AtomicMoveNotSupportedException e) {
      FileUtil.moveDurably(tempFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
    }
  }

  /** 读取并校验；文件不存在返回 {@link Optional#empty()}，头/版本/校验不符抛对应异常 */
  public static Optional<byte[]> readFromFile(final Path file) throws IOException {
    try (final FileChannel channel = FileChannel.open(file, StandardOpenOption.READ)) {
      final long fileSize = channel.size();
      if (fileSize < HEADER_LENGTH) {
        throw new MissingHeader(file, fileSize);
      }
      return Optional.of(
          fileSize >= MMAP_THRESHOLD_BYTES
              ? readMapped(file, channel, fileSize)
              : readSmallFile(file, channel, (int) fileSize));
    } catch (final NoSuchFileException e) {
      return Optional.empty();
    }
  }

  /** mmap 路径: 校验零堆拷贝, 仅返回 body 一次拷贝 */
  private static byte[] readMapped(final Path file, final FileChannel channel, final long fileSize)
      throws IOException {
    final MappedByteBuffer mapped = channel.map(FileChannel.MapMode.READ_ONLY, 0, fileSize);
    try {
      mapped.order(ByteOrder.LITTLE_ENDIAN);
      final byte version = mapped.get(0);
      final long expectedChecksum = mapped.getLong(1);
      rejectBadHeader(file, version);
      final int bodyLength = (int) (fileSize - HEADER_LENGTH);
      // 定位 body 起点, 校验直接在映射上算(内联, 零拷贝)
      mapped.position(HEADER_LENGTH).limit((int) fileSize);
      final var checksum = new CRC32C();
      checksum.update(mapped);
      final long actualChecksum = checksum.getValue();
      if (expectedChecksum != actualChecksum) {
        throw new ChecksumMismatch(file, expectedChecksum, actualChecksum);
      }
      final byte[] body = new byte[bodyLength];
      mapped.position(HEADER_LENGTH);
      mapped.get(body, 0, bodyLength);
      return body;
    } finally {
      // 与本包 IoUtil 重名, 用全限定名调用 agrona 的显式解除映射, 避免依赖 GC Cleaner 回收
      org.agrona.IoUtil.unmap(mapped);
    }
  }

  /** 小文件路径: 单通道读入, 原位校验, 单次拷出 */
  private static byte[] readSmallFile(
      final Path file, final FileChannel channel, final int fileSize) throws IOException {
    final ByteBuffer content = ByteBuffer.allocate(fileSize);
    while (content.hasRemaining()) {
      if (channel.read(content) < 0) {
        break;
      }
    }
    content.flip();
    if (content.remaining() < HEADER_LENGTH) {
      throw new MissingHeader(file, content.remaining());
    }
    content.order(ByteOrder.LITTLE_ENDIAN);
    final byte version = content.get();
    final long expectedChecksum = content.getLong();
    rejectBadHeader(file, version);
    final ByteBuffer body = content.slice();
    final var checksum = new CRC32C();
    checksum.update(body.duplicate());
    final long actualChecksum = checksum.getValue();
    if (expectedChecksum != actualChecksum) {
      throw new ChecksumMismatch(file, expectedChecksum, actualChecksum);
    }
    final byte[] result = new byte[body.remaining()];
    body.get(result);
    return result;
  }

  private static void rejectBadHeader(final Path file, final byte version) {
    if (version != VERSION) {
      throw new UnexpectedVersion(file, version);
    }
  }

  private static long checksum(final byte[] bytes, final int offset, final int length) {
    final var checksum = new CRC32C();
    checksum.update(bytes, offset, length);
    return checksum.getValue();
  }

  public static final class UnexpectedVersion extends RuntimeException {
    public UnexpectedVersion(final Path file, final byte version) {
      super("文件 %s 版本 '%s' 不符, 期望 '%s'".formatted(file, version, VERSION));
    }
  }

  public static final class MissingHeader extends RuntimeException {
    public MissingHeader(final Path file, final Object fileSize) {
      super("文件 %s 过小(%s 字节), 无法容纳校验头".formatted(file, fileSize));
    }
  }

  public static final class ChecksumMismatch extends RuntimeException {
    public ChecksumMismatch(final Path file, final long expected, final long actual) {
      super("文件损坏: %s, 期望校验 '%d', 实际 '%d'".formatted(file, expected, actual));
    }
  }
}
