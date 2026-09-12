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
package com.anyilanxin.kunpeng.cluster.raft.snapshot;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 镜像元数据：Raft 标识 + 业务信息键值清单，随镜像持久化到镜像目录内的 {@value #METADATA_FILE_NAME} 文件，并计入 .sfc 校验。
 *
 * <p>文件为行式文本：首行 {@code version=<int>}，随后每行一个 {@code <key>=<value>} 业务条目 （key/value 均不得包含 '='
 * 与换行）。标识三元组由目录名承载，文件内不重复。
 *
 * @author zxuanhong
 * @since 1.0.0
 */
public record SnapshotMetadata(
    String nodeId, long index, long term, int version, Map<String, String> metaInfo) {

  /** 元数据缺省版本。 */
  public static final int DEFAULT_VERSION = 1;

  /** 镜像目录内的元数据文件名。 */
  public static final String METADATA_FILE_NAME = "snapshot.metadata";

  public SnapshotMetadata {
    Objects.requireNonNull(nodeId, "nodeId must not be null");
    metaInfo = metaInfo == null ? Map.of() : Map.copyOf(metaInfo);
  }

  /** 业务信息以 {@code Map<String, Object>} 提供、落盘值取 {@code String.valueOf(value)} 的构造。 */
  public static SnapshotMetadata of(
      final SnapshotId snapshotId, final Map<String, Object> metaInfo) {
    final Map<String, String> persisted =
        metaInfo == null
            ? Map.of()
            : metaInfo.entrySet().stream()
                .collect(
                    Collectors.toUnmodifiableMap(
                        Map.Entry::getKey, entry -> String.valueOf(entry.getValue())));
    return new SnapshotMetadata(
        snapshotId.nodeId(), snapshotId.index(), snapshotId.term(), DEFAULT_VERSION, persisted);
  }

  /** 本元数据的镜像标识三元组。 */
  public SnapshotId snapshotId() {
    return new SnapshotId(nodeId, index, term);
  }

  /** 写入镜像目录的 {@value #METADATA_FILE_NAME} 文件（写侧 fsync），返回文件路径。 */
  public Path writeTo(final Path directory) {
    final var builder = new StringBuilder();
    builder.append("version=").append(version).append('\n');
    metaInfo.forEach((key, value) -> builder.append(key).append('=').append(value).append('\n'));
    final Path metadataPath = directory.resolve(METADATA_FILE_NAME);
    try (final FileChannel channel =
        FileChannel.open(
            metadataPath,
            StandardOpenOption.CREATE,
            StandardOpenOption.WRITE,
            StandardOpenOption.TRUNCATE_EXISTING)) {
      channel.write(ByteBuffer.wrap(builder.toString().getBytes(StandardCharsets.UTF_8)));
      channel.force(true);
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
    return metadataPath;
  }

  /** 读取镜像目录内的 {@value #METADATA_FILE_NAME} 文件，并与目录名解析出的标识合并。 */
  public static SnapshotMetadata readFrom(final Path directory, final SnapshotId snapshotId) {
    final java.util.List<String> lines;
    try {
      lines = Files.readAllLines(directory.resolve(METADATA_FILE_NAME), StandardCharsets.UTF_8);
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
    int version = DEFAULT_VERSION;
    final Map<String, String> metaInfo = new HashMap<>();
    for (final String line : lines) {
      if (line.startsWith("version=")) {
        version = Integer.parseInt(line.substring("version=".length()));
      } else {
        final var separator = line.indexOf('=');
        if (separator > 0) {
          metaInfo.put(line.substring(0, separator), line.substring(separator + 1));
        }
      }
    }
    return new SnapshotMetadata(
        snapshotId.nodeId(), snapshotId.index(), snapshotId.term(), version, Map.copyOf(metaInfo));
  }
}
