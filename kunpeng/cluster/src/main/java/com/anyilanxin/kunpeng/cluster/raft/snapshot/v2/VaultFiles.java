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
package com.anyilanxin.kunpeng.cluster.raft.snapshot.v2;

import com.anyilanxin.kunpeng.utils.FileUtil;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;
import java.util.zip.CRC32C;

/** 快照文件 IO：CRC32C（JDK intrinsics）、排序遍历、持久写 */
public final class VaultFiles {

  private static final int CRC_BUFFER_BYTES = 4096;

  private VaultFiles() {}

  /** 逐文件 CRC32C（FileChannel 4KB 循环） */
  public static long fileCrc(final Path file) throws IOException {
    final CRC32C crc = new CRC32C();
    try (FileChannel channel = FileChannel.open(file, StandardOpenOption.READ)) {
      final ByteBuffer buffer = ByteBuffer.allocateDirect(CRC_BUFFER_BYTES);
      while (channel.read(buffer) != -1) {
        buffer.flip();
        crc.update(buffer);
        buffer.clear();
      }
    }
    return crc.getValue();
  }

  /** 目录下全部常规文件（相对路径名按字典序） */
  public static TreeSet<String> listFilesSorted(final Path directory) throws IOException {
    final TreeSet<String> names = new TreeSet<>();
    try (var walk = Files.walk(directory)) {
      walk.filter(Files::isRegularFile)
          .forEach(file -> names.add(directory.relativize(file).toString().replace('\\', '/')));
    }
    return names;
  }

  /** 覆盖式持久写：临时文件 + fsync + 原子改名 */
  public static void writeDurably(final Path target, final byte[] bytes) throws IOException {
    final Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
    try (FileChannel channel =
        FileChannel.open(tmp, StandardOpenOption.CREATE, StandardOpenOption.WRITE,
            StandardOpenOption.TRUNCATE_EXISTING)) {
      channel.write(ByteBuffer.wrap(bytes));
      channel.force(true);
    }
    FileUtil.moveDurably(tmp, target, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
  }

  /** 定位写（接收端乱序分块） */
  public static void positionedWrite(final Path file, final long offset, final byte[] payload)
      throws IOException {
    try (RandomAccessFile raf = new RandomAccessFile(file.toFile(), "rw")) {
      raf.seek(offset);
      raf.write(payload);
      raf.getFD().sync();
    }
  }

  public static void ensureDirectory(final Path directory) throws IOException {
    FileUtil.ensureDirectory(directory);
  }

  public static void deleteRecursively(final Path path) throws IOException {
    FileUtil.deleteTreeIfExists(path);
  }

  /** 目录树拷贝（副本区复制） */
  public static void copySnapshot(final Path source, final Path target) throws IOException {
    FileUtil.copyTree(source, target);
  }

  public static List<Path> sortedSubDirectories(final Path directory) throws IOException {
    try (var stream = Files.list(directory)) {
      return stream
          .filter(Files::isDirectory)
          .sorted(Comparator.comparing(p -> p.getFileName().toString()))
          .toList();
    }
  }
}
