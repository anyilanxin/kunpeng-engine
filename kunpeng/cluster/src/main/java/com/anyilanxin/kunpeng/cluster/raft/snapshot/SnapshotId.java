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

import java.util.HexFormat;
import java.util.Objects;

/**
 * 快照标识三元组：{@code index/term/nodeId}，同时决定快照目录名。
 *
 * <p>字符串（目录名）形式为 {@code <index>-<term>-<hex(nodeId)>}，节点 id 按 UTF-8 字节十六进制
 * 编码，避免节点 id 中含 '-' 时无法反向解析。
 *
 * @param index the Raft log index of the snapshot
 * @param term the Raft term of the snapshot
 * @param nodeId the id of the node which took the snapshot
 */
public record SnapshotId(long index, long term, String nodeId) implements Comparable<SnapshotId> {

  public SnapshotId {
    Objects.requireNonNull(nodeId, "nodeId must not be null");
  }

  /** 从快照元数据提取标识。 */
  public static SnapshotId fromMetadata(final SnapshotMetadata metadata) {
    return new SnapshotId(metadata.index(), metadata.term(), metadata.nodeId());
  }

  /**
   * 解析 {@code <index>-<term>-<hex(nodeId)>} 形式的快照标识（即快照目录名）。
   *
   * @throws IllegalArgumentException if the string is malformed
   */
  public static SnapshotId fromString(final String snapshotId) {
    final String[] parts = Objects.requireNonNull(snapshotId, "snapshotId").split("-");
    if (parts.length != 3) {
      throw new IllegalArgumentException(
          "Expected snapshot id in format <index>-<term>-<hex(nodeId)>, got: " + snapshotId);
    }
    try {
      return new SnapshotId(
          Long.parseLong(parts[0]),
          Long.parseLong(parts[1]),
          decodeNodeId(parts[2]));
    } catch (final NumberFormatException e) {
      throw new IllegalArgumentException(
          "Expected snapshot id in format <index>-<term>-<hex(nodeId)>, got: " + snapshotId, e);
    }
  }

  /** 标识的字符串（目录名）形式：{@code <index>-<term>-<hex(nodeId)>}。 */
  public String asString() {
    return index + "-" + term + "-" + encodeNodeId(nodeId);
  }

  /** 节点 id 的 UTF-8 字节十六进制编码（小写）。 */
  private static String encodeNodeId(final String nodeId) {
    return HexFormat.of().formatHex(nodeId.getBytes(java.nio.charset.StandardCharsets.UTF_8));
  }

  /** 十六进制编码的节点 id 反向解码。 */
  private static String decodeNodeId(final String encoded) {
    return new String(
        HexFormat.of().parseHex(encoded), java.nio.charset.StandardCharsets.UTF_8);
  }

  @Override
  public int compareTo(final SnapshotId other) {
    final int byIndex = Long.compare(index, other.index);
    if (byIndex != 0) {
      return byIndex;
    }
    final int byTerm = Long.compare(term, other.term);
    if (byTerm != 0) {
      return byTerm;
    }
    return nodeId.compareTo(other.nodeId);
  }

  @Override
  public String toString() {
    return asString();
  }
}
