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
package com.anyilanxin.kunpeng.cluster.raft.orchestrator;

import com.anyilanxin.kunpeng.utils.FileDataStoreUtils;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 节点级分区元数据磁盘存储。
 *
 * <p>目录布局：基础目录下 {@code .raft-meta} 存储元数据，{@code <groupName>/} 存储各分区组数据。
 * <pre>
 * data/
 * ├── .raft-meta            ← 节点级分区组元数据（FileDataStoreUtils 崩溃安全写）
 * ├── group-1/              ← 分区组 1 数据
 * │   ├── partition-1/
 * │   └── partition-2/
 * └── group-2/
 * </pre>
 *
 * <p>启动时 {@link #load()} 读取：有本地元数据则按记录恢复分区组；无则由启动参数初始化。
 */
public final class NodeMetadataStore {

  private static final Logger LOG = LoggerFactory.getLogger(NodeMetadataStore.class);
  private static final String METADATA_FILE = ".raft-meta";
  private static final int FORMAT_VERSION = 1;

  private final Path dataDirectory;
  private final Path metadataPath;
  private final Map<String, NodePartitionMetadata> groups = new ConcurrentHashMap<>();
  private volatile boolean loaded;

  public NodeMetadataStore(final Path dataDirectory) {
    this.dataDirectory = dataDirectory;
    this.metadataPath = dataDirectory.resolve(METADATA_FILE);
  }

  /** 分区组数据目录（data/&lt;groupName&gt;/） */
  public Path groupDataDirectory(final String groupName) {
    return dataDirectory.resolve(groupName);
  }

  /** 基础数据目录 */
  public Path dataDirectory() {
    return dataDirectory;
  }

  /** 从磁盘加载全部分区组元数据；文件不存在返回 empty */
  public Optional<Map<String, NodePartitionMetadata>> load() {
    if (loaded) {
      return Optional.of(new LinkedHashMap<>(groups));
    }
    if (!Files.exists(metadataPath)) {
      LOG.info("无本地分区元数据（{}），将由启动参数初始化", metadataPath);
      return Optional.empty();
    }
    try {
      final var bytes = FileDataStoreUtils.readFromFile(metadataPath);
      deserialize(bytes);
      loaded = true;
      LOG.info("从磁盘恢复 {} 个分区组元数据: {}", groups.size(), groups.keySet());
      return Optional.of(new LinkedHashMap<>(groups));
    } catch (final Exception e) {
      LOG.error("分区元数据加载失败: {}", metadataPath, e);
      return Optional.empty();
    }
  }

  /** 保存（或更新）一个分区组的元数据并持久化 */
  public void savePartitionGroup(final NodePartitionMetadata metadata) {
    groups.put(metadata.groupName(), metadata);
    persist();
  }

  /** 移除一个分区组的元数据并持久化 */
  public void removePartitionGroup(final String groupName) {
    groups.remove(groupName);
    persist();
  }

  /** 获取指定分区组的元数据 */
  public Optional<NodePartitionMetadata> getPartitionGroup(final String groupName) {
    return Optional.ofNullable(groups.get(groupName));
  }

  /** 获取全部分区组元数据 */
  public Map<String, NodePartitionMetadata> getAll() {
    return new LinkedHashMap<>(groups);
  }

  /** 持久化到磁盘（FileDataStoreUtils 崩溃安全写） */
  public synchronized void persist() {
    try {
      FileDataStoreUtils.writeToFile(metadataPath, serialize());
      LOG.debug("分区元数据已持久化到 {}", metadataPath);
    } catch (final Exception e) {
      LOG.error("分区元数据持久化失败: {}", metadataPath, e);
      throw new RuntimeException("分区元数据持久化失败", e);
    }
  }

  // ===== 二进制序列化（DataStream，紧凑高效） =====

  private byte[] serialize() throws IOException {
    final var bos = new ByteArrayOutputStream();
    final var out = new DataOutputStream(bos);
    out.writeInt(FORMAT_VERSION);
    out.writeInt(groups.size());
    for (final var meta : groups.values()) {
      writeString(out, meta.groupName());
      writeString(out, meta.groupType());
      out.writeInt(meta.partitionCount());
      out.writeInt(meta.replicationFactor());
      out.writeInt(meta.localPartitions().size());
      for (final var id : meta.localPartitions()) {
        out.writeInt(id);
      }
      out.writeLong(meta.createdAt());
      out.writeLong(meta.updatedAt());
      out.writeInt(meta.storageConfig().size());
      for (final var entry : meta.storageConfig().entrySet()) {
        writeString(out, entry.getKey());
        writeString(out, entry.getValue());
      }
    }
    out.flush();
    return bos.toByteArray();
  }

  private void deserialize(final byte[] bytes) throws IOException {
    final var in = new DataInputStream(new ByteArrayInputStream(bytes));
    final var version = in.readInt();
    if (version != FORMAT_VERSION) {
      throw new IOException("元数据格式版本不符: " + version);
    }
    final var count = in.readInt();
    for (int i = 0; i < count; i++) {
      final var groupName = readString(in);
      final var groupType = readString(in);
      final var partitionCount = in.readInt();
      final var replicationFactor = in.readInt();
      final var localCount = in.readInt();
      final var localPartitions = new ArrayList<Integer>(localCount);
      for (int j = 0; j < localCount; j++) {
        localPartitions.add(in.readInt());
      }
      final var createdAt = in.readLong();
      final var updatedAt = in.readLong();
      final var storageCount = in.readInt();
      final var storageConfig = new LinkedHashMap<String, String>();
      for (int j = 0; j < storageCount; j++) {
        storageConfig.put(readString(in), readString(in));
      }
      final var meta = new NodePartitionMetadata(
          groupName, groupType, partitionCount, replicationFactor,
          localPartitions, storageConfig, createdAt, updatedAt);
      groups.put(groupName, meta);
    }
  }

  private static void writeString(final DataOutputStream out, final String value) throws IOException {
    final var bytes = value.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    out.writeInt(bytes.length);
    out.write(bytes);
  }

  private static String readString(final DataInputStream in) throws IOException {
    final var length = in.readInt();
    final var bytes = new byte[length];
    in.readFully(bytes);
    return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
  }
}
