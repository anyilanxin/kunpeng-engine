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
package com.anyilanxin.kunpeng.cluster.raft.snapshot.impl;

import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotMetadata;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotType;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 快照的 manifest 文件：业务元数据清单（{@link SnapshotMetadata}）加上逐文件 size/CRC32 清单。
 * 行式文本格式：
 *
 * <pre>
 * index=&lt;long&gt;
 * term=&lt;long&gt;
 * nodeId=&lt;string&gt;
 * type=&lt;REGULAR|BOOTSTRAP|MERGE&gt;
 * version=&lt;int&gt;                (缺省 1，兼容旧 manifest)
 * biz.&lt;key&gt;=&lt;value&gt;            (业务信息，任意多行)
 * file,&lt;size&gt;,&lt;crc32&gt;,&lt;name&gt;
 * </pre>
 */
final class SnapshotManifest {

  static final String MANIFEST_FILE_NAME = "manifest";
  private static final String BUSINESS_PREFIX = "biz.";

  final SnapshotMetadata metadata;
  final List<FileEntry> files;

  record FileEntry(String name, long size, long checksum) {}

  SnapshotManifest(final SnapshotMetadata metadata, final List<FileEntry> files) {
    this.metadata = metadata;
    this.files = files;
  }

  /** 快照类型（随元数据持久化）。 */
  SnapshotType type() {
    return metadata.type();
  }

  /**
   * 为给定目录构建 manifest：除 manifest 自身外的所有常规文件计入清单，记录 size 与 CRC32。
   */
  static SnapshotManifest of(final SnapshotMetadata metadata, final Path directory) {
    try (final var stream = Files.walk(directory)) {
      final List<FileEntry> files = new ArrayList<>();
      stream
          .filter(Files::isRegularFile)
          .map(directory::relativize)
          .map(Path::toString)
          .filter(name -> !name.equals(MANIFEST_FILE_NAME))
          .sorted()
          .forEach(
              name -> {
                final Path file = directory.resolve(name);
                try {
                  files.add(new FileEntry(name, Files.size(file), checksumOf(file)));
                } catch (final IOException e) {
                  throw new UncheckedIOException(e);
                }
              });
      return new SnapshotManifest(metadata, List.copyOf(files));
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /** 把 manifest 写入快照目录（写侧 fsync）。 */
  void write(final Path directory) {
    final var builder = new StringBuilder();
    builder.append("index=").append(metadata.index()).append('\n');
    builder.append("term=").append(metadata.term()).append('\n');
    builder.append("nodeId=").append(metadata.nodeId()).append('\n');
    builder.append("type=").append(metadata.type().name()).append('\n');
    builder.append("version=").append(metadata.version()).append('\n');
    metadata.businessInfo().forEach(
        (key, value) -> builder.append(BUSINESS_PREFIX).append(key).append('=')
            .append(value).append('\n'));
    for (final FileEntry entry : files) {
      builder
          .append("file,")
          .append(entry.size())
          .append(',')
          .append(entry.checksum())
          .append(',')
          .append(entry.name())
          .append('\n');
    }
    try (final FileChannel channel =
        FileChannel.open(
            directory.resolve(MANIFEST_FILE_NAME),
            StandardOpenOption.CREATE,
            StandardOpenOption.WRITE,
            StandardOpenOption.TRUNCATE_EXISTING)) {
      channel.write(ByteBuffer.wrap(builder.toString().getBytes(StandardCharsets.UTF_8)));
      channel.force(true);
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /** 逐文件校验目录内容与 manifest 记录的 size 与 CRC32 是否一致。 */
  boolean verifyContent(final Path directory) {
    for (final FileEntry entry : files) {
      final Path file = directory.resolve(entry.name());
      try {
        if (!Files.isRegularFile(file)
            || Files.size(file) != entry.size()
            || checksumOf(file) != entry.checksum()) {
          return false;
        }
      } catch (final IOException e) {
        return false;
      }
    }
    return true;
  }

  /** 读取并解析指定快照目录的 manifest；type/version/biz 缺失时按缺省值兼容旧格式。 */
  static SnapshotManifest read(final Path directory) {
    final List<String> lines;
    try {
      lines = Files.readAllLines(directory.resolve(MANIFEST_FILE_NAME), StandardCharsets.UTF_8);
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }

    long index = -1;
    long term = -1;
    String nodeId = null;
    SnapshotType type = SnapshotType.REGULAR;
    int version = SnapshotMetadata.DEFAULT_VERSION;
    final Map<String, String> businessInfo = new HashMap<>();
    final List<FileEntry> files = new ArrayList<>();
    for (final String line : lines) {
      if (line.startsWith("index=")) {
        index = Long.parseLong(line.substring("index=".length()));
      } else if (line.startsWith("term=")) {
        term = Long.parseLong(line.substring("term=".length()));
      } else if (line.startsWith("nodeId=")) {
        nodeId = line.substring("nodeId=".length());
      } else if (line.startsWith("type=")) {
        type = SnapshotType.valueOf(line.substring("type=".length()));
      } else if (line.startsWith("version=")) {
        version = Integer.parseInt(line.substring("version=".length()));
      } else if (line.startsWith(BUSINESS_PREFIX)) {
        final var separator = line.indexOf('=', BUSINESS_PREFIX.length());
        businessInfo.put(
            line.substring(BUSINESS_PREFIX.length(), separator),
            line.substring(separator + 1));
      } else if (line.startsWith("file,")) {
        final var firstComma = line.indexOf(',', 5);
        final var secondComma = line.indexOf(',', firstComma + 1);
        files.add(
            new FileEntry(
                line.substring(secondComma + 1),
                Long.parseLong(line.substring(5, firstComma)),
                Long.parseLong(line.substring(firstComma + 1, secondComma))));
      }
    }
    return new SnapshotManifest(
        new SnapshotMetadata(index, term, nodeId, type, version, Map.copyOf(businessInfo)),
        files);
  }

  /** 计算文件内容的 CRC32。 */
  static long checksumOf(final Path file) {
    final var crc = new java.util.zip.CRC32();
    try (final var input = Files.newInputStream(file)) {
      final byte[] buffer = new byte[8192];
      int read;
      while ((read = input.read(buffer)) != -1) {
        crc.update(buffer, 0, read);
      }
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
    return crc.getValue();
  }

  /** 内容文件总大小（不含 manifest 自身）。 */
  long totalSize() {
    return files.stream().mapToLong(FileEntry::size).sum();
  }

  /** 清单覆盖的文件名（排序）。 */
  List<String> fileNames() {
    return files.stream().map(FileEntry::name).sorted(Comparator.naturalOrder()).toList();
  }
}
