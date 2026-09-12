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

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Objects;

/**
 * 镜像标识三元组：{@code nodeId/index/term}，同时决定镜像目录名。
 *
 * <p>字符串（目录名）形式为 {@code <hex(nodeId)>-<index>-<term>}：nodeId 置于最前便于人工阅读； 新旧比较只看 (index, term)，见
 * compareTo；节点 id 按 UTF-8 字节十六进制编码，避免 nodeId 含 '-' 时无法反向解析。
 *
 * @author zxuanhong
 * @since 1.0.0
 */
public record SnapshotId(String nodeId, long index, long term) implements Comparable<SnapshotId> {

  public SnapshotId {
    Objects.requireNonNull(nodeId, "nodeId must not be null");
  }

  /**
   * 解析 {@code <hex(nodeId)>-<index>-<term>} 形式的镜像标识（即镜像目录名）。
   *
   * @throws IllegalArgumentException 格式非法
   */
  public static SnapshotId fromString(final String snapshotId) {
    final String[] parts = Objects.requireNonNull(snapshotId, "snapshotId").split("-");
    if (parts.length != 3) {
      throw new IllegalArgumentException(
          "Expected snapshot id in format <hex(nodeId)>-<index>-<term>, got: " + snapshotId);
    }
    try {
      return new SnapshotId(
          decodeNodeId(parts[0]), Long.parseLong(parts[1]), Long.parseLong(parts[2]));
    } catch (final IllegalArgumentException e) {
      throw new IllegalArgumentException(
          "Expected snapshot id in format <hex(nodeId)>-<index>-<term>, got: " + snapshotId, e);
    }
  }

  /** 标识的字符串（目录名）形式：{@code <hex(nodeId)>-<index>-<term>}。 */
  public String asString() {
    return encodeNodeId(nodeId) + "-" + index + "-" + term;
  }

  /** 节点 id 的 UTF-8 字节十六进制编码（小写）。 */
  private static String encodeNodeId(final String nodeId) {
    return HexFormat.of().formatHex(nodeId.getBytes(StandardCharsets.UTF_8));
  }

  /** 十六进制编码的节点 id 反向解码。 */
  private static String decodeNodeId(final String encoded) {
    return new String(HexFormat.of().parseHex(encoded), StandardCharsets.UTF_8);
  }

  /**
   * 仅按 {@code (index, term)} 比较新旧，不掺入 nodeId：拍摄（自身 nodeId）与接收（leader nodeId） 的镜像共用同一 store，nodeId
   * 参与排序会导致 leader 变更后新旧判定系统性错误 （见 CODE_REVIEW_FINDINGS.md F-01）。注意 compareTo 相等不再蕴含 equals（nodeId
   * 可能不同）。
   */
  @Override
  public int compareTo(final SnapshotId other) {
    final int byIndex = Long.compare(index, other.index);
    if (byIndex != 0) {
      return byIndex;
    }
    return Long.compare(term, other.term);
  }

  @Override
  public String toString() {
    return asString();
  }
}
