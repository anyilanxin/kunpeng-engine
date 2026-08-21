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
package com.anyilanxin.kunpeng.cluster.raft.snapshot;

import java.util.Map;
import java.util.Objects;

/**
 * 快照的业务元数据清单：由业务系统在拍摄快照时生成并随快照持久化到 manifest。
 *
 * <p>除 Raft 标识三元组（{@code index/term/nodeId}，同时经 {@link SnapshotId} 决定快照目录名）
 * 外，还携带：
 *
 * <ul>
 *   <li>{@code type}——快照类型（常规/引导/合并），决定快照落入的存储子目录；</li>
 *   <li>{@code version}——业务元数据版本号（恒为缺省 1），供消费方按版本演进解析；</li>
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

  /** 业务信息以 {@code Map<String, Object>} 提供、落盘值取 {@code String.valueOf(value)} 的构造。 */
  public static SnapshotMetadata ofBusinessInfo(
      final long index,
      final long term,
      final String nodeId,
      final SnapshotType type,
      final Map<String, Object> businessInfo) {
    final Map<String, String> persisted =
        businessInfo == null
            ? Map.of()
            : businessInfo.entrySet().stream()
                .collect(
                    java.util.stream.Collectors.toUnmodifiableMap(
                        Map.Entry::getKey, entry -> String.valueOf(entry.getValue())));
    return new SnapshotMetadata(index, term, nodeId, type, DEFAULT_VERSION, persisted);
  }

  /**
   * 解析 {@code <index>-<term>-<hex(nodeId)>} 形式的快照目录名（hex 解码走
   * {@link SnapshotId#fromString}）。
   *
   * <p>目录名只承载 Raft 标识，类型/版本/业务信息以 manifest 为准；此处类型占位为
   * {@link SnapshotType#REGULAR}，仅供目录名不可解析场景兜底与接收前的临时引用。
   *
   * @throws IllegalArgumentException if the string is malformed
   */
  public static SnapshotMetadata fromSnapshotId(final String snapshotId) {
    final SnapshotId id = SnapshotId.fromString(snapshotId);
    return SnapshotMetadata.of(id.index(), id.term(), id.nodeId(), SnapshotType.REGULAR);
  }

  /** 本元数据的快照标识三元组。 */
  public SnapshotId getSnapshotId() {
    return new SnapshotId(index, term, nodeId);
  }

  /**
   * 快照标识字符串（即快照目录名）：{@code <index>-<term>-<hex(nodeId)>}；类型与业务信息不
   * 属于标识。
   */
  public String getSnapshotIdAsString() {
    return getSnapshotId().asString();
  }

  @Override
  public String toString() {
    return getSnapshotIdAsString();
  }
}
