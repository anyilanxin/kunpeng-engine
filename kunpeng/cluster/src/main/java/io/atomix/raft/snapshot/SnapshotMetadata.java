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
package io.atomix.raft.snapshot;

import java.util.Map;
import java.util.Objects;

/**
 * 快照的业务元数据清单：由业务系统在拍摄快照时生成并随快照持久化到 manifest。
 *
 * <p>除 Raft 标识三元组（{@code index/term/nodeId}，同时是快照目录名）外，还携带：
 *
 * <ul>
 *   <li>{@code type}——镜像类型（常规/引导/合并），决定快照落入的存储子目录；</li>
 *   <li>{@code version}——业务元数据版本号，供消费方按版本演进解析；</li>
 *   <li>{@code businessInfo}——业务信息键值清单（不可变，key/value 均不得包含 '=' 与换行）。</li>
 * </ul>
 *
 * @param index the Raft log index of the snapshot
 * @param term the Raft term of the snapshot
 * @param nodeId the id of the node which took the snapshot
 * @param type the type of the snapshot
 * @param version the business metadata version
 * @param businessInfo the business key-value manifest
 */
public record SnapshotMetadata(
    long index,
    long term,
    String nodeId,
    SnapshotType type,
    int version,
    Map<String, String> businessInfo) {

  /** 业务元数据缺省版本。 */
  public static final int DEFAULT_VERSION = 1;

  public SnapshotMetadata {
    Objects.requireNonNull(nodeId, "nodeId must not be null");
    Objects.requireNonNull(type, "type must not be null");
    businessInfo = businessInfo == null ? Map.of() : Map.copyOf(businessInfo);
  }

  /** 便捷构造：无业务信息、使用缺省版本。 */
  public static SnapshotMetadata of(
      final long index, final long term, final String nodeId, final SnapshotType type) {
    return new SnapshotMetadata(index, term, nodeId, type, DEFAULT_VERSION, Map.of());
  }

  /**
   * 解析 {@code <index>-<term>-<nodeId>} 形式的快照目录名。
   *
   * <p>目录名只承载 Raft 标识，类型/版本/业务信息以 manifest 为准；此处类型占位为
   * {@link SnapshotType#REGULAR}，仅供目录名不可解析场景兜底与接收前的临时引用。
   *
   * @throws IllegalArgumentException if the string is malformed
   */
  public static SnapshotMetadata fromSnapshotId(final String snapshotId) {
    final String[] parts = Objects.requireNonNull(snapshotId, "snapshotId").split("-");
    if (parts.length < 3) {
      throw new IllegalArgumentException(
          "Expected snapshot id in format <index>-<term>-<nodeId>, got: " + snapshotId);
    }
    try {
      return SnapshotMetadata.of(
          Long.parseLong(parts[0]), Long.parseLong(parts[1]), parts[2], SnapshotType.REGULAR);
    } catch (final NumberFormatException e) {
      throw new IllegalArgumentException(
          "Expected snapshot id in format <index>-<term>-<nodeId>, got: " + snapshotId, e);
    }
  }

  /**
   * 快照标识字符串（即快照目录名）：{@code <index>-<term>-<nodeId>}；类型与业务信息不属于标识。
   */
  public String getSnapshotIdAsString() {
    return index + "-" + term + "-" + nodeId;
  }

  @Override
  public String toString() {
    return getSnapshotIdAsString();
  }
}
