/*
 * Copyright © 2026 anyilanxin zxh(anyilanxin@aliyun.com)
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.cluster.raft.snapshot.impl;

import com.anyilanxin.kunpeng.cluster.raft.snapshot.SimpleFileVerificationInfo;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.SimpleFileVerificationStore;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotException;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotFileInfo;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SFV 格式 .sfc 校验文件的默认实现：与镜像目录同名、位于同级，逐文件记录 {@code <name> <size> <crc32-hex>}。
 *
 * @author zxuanhong
 * @since 1.0.0
 */
public final class DefaultSimpleFileVerificationStore implements SimpleFileVerificationStore {

  /** 校验文件后缀：与镜像目录同名、位于同级。 */
  public static final String VERIFICATION_SUFFIX = ".sfc";

  private static final String TEMP_SUFFIX = ".tmp";

  /** 镜像目录对应的 .sfc 校验文件路径。 */
  public static Path sfvPathOf(final Path snapshotPath) {
    return snapshotPath
        .getParent()
        .resolve(snapshotPath.getFileName().toString() + VERIFICATION_SUFFIX);
  }

  @Override
  public SimpleFileVerificationInfo newFileVerificationInfo(
      final Path snapshotPath, final Map<String, SnapshotFileInfo> snapshotFileInfo) {
    final var builder = new StringBuilder();
    builder.append("; kunpeng snapshot verification\n");
    builder.append("; snapshot: ").append(snapshotPath.getFileName()).append('\n');
    snapshotFileInfo.entrySet().stream()
        .sorted(Map.Entry.comparingByKey())
        .forEach(
            entry ->
                builder
                    .append(entry.getKey())
                    .append(' ')
                    .append(entry.getValue().size())
                    .append(' ')
                    .append(String.format("%08x", entry.getValue().checksum()))
                    .append('\n'));

    final Path sfvPath = sfvPathOf(snapshotPath);
    final Path tempPath = sfvPath.resolveSibling(sfvPath.getFileName() + TEMP_SUFFIX);
    // 先写 .tmp 并强制落盘，再原子 move 为正式 .sfc——.sfc 存在即代表校验文件完整写入
    try (final var channel =
        FileChannel.open(
            tempPath,
            StandardOpenOption.CREATE,
            StandardOpenOption.WRITE,
            StandardOpenOption.TRUNCATE_EXISTING)) {
      channel.write(ByteBuffer.wrap(builder.toString().getBytes(StandardCharsets.UTF_8)));
      channel.force(true);
    } catch (final IOException e) {
      throw new UncheckedIOException("Failed to write verification file " + tempPath, e);
    }
    try {
      Files.move(
          tempPath, sfvPath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    } catch (final AtomicMoveNotSupportedException e) {
      try {
        Files.move(tempPath, sfvPath, StandardCopyOption.REPLACE_EXISTING);
      } catch (final IOException ex) {
        throw new UncheckedIOException(
            "Failed to move verification file into place " + sfvPath, ex);
      }
    } catch (final IOException e) {
      throw new UncheckedIOException("Failed to move verification file into place " + sfvPath, e);
    }
    return new SfvFileVerificationInfo(sfvPath, snapshotFileInfo);
  }

  @Override
  public SimpleFileVerificationInfo load(final Path snapshotPath) {
    final Path sfvPath = sfvPathOf(snapshotPath);
    final List<String> lines;
    try {
      lines = Files.readAllLines(sfvPath, StandardCharsets.UTF_8);
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
    final Map<String, SnapshotFileInfo> fileInfos = new HashMap<>();
    for (final String line : lines) {
      if (line.isBlank() || line.startsWith(";")) {
        continue;
      }
      // 行格式：<name> <size> <crc32-hex>；文件名可能含空格，从右侧解析
      final int crcSeparator = line.lastIndexOf(' ');
      final int sizeSeparator = crcSeparator <= 0 ? -1 : line.lastIndexOf(' ', crcSeparator - 1);
      if (sizeSeparator <= 0) {
        throw new SnapshotException("Malformed verification line: " + line);
      }
      fileInfos.put(
          line.substring(0, sizeSeparator),
          new SnapshotFileInfo(
              Long.parseLong(line.substring(crcSeparator + 1), 16),
              Long.parseLong(line.substring(sizeSeparator + 1, crcSeparator))));
    }
    return new SfvFileVerificationInfo(sfvPath, fileInfos);
  }

  @Override
  public boolean exists(final Path snapshotPath) {
    return Files.exists(sfvPathOf(snapshotPath));
  }

  @Override
  public void delete(final Path snapshotPath) {
    try {
      Files.deleteIfExists(sfvPathOf(snapshotPath));
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
