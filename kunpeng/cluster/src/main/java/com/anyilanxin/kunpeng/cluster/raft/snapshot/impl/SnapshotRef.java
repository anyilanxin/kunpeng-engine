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

import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.Objects;

/**
 * 快照身份：{@code <index>-<term>-<brokerId Hex>}。
 *
 * <p>业务位置等元数据属于 {@code SnapshotMeta}（业务层），不在通用层出现。
 * 排序 index → term。brokerId 段为 UTF-8 字节的十六进制（字母表仅
 * {@code 0-9a-f}），段内不含分隔符 {@code -} 且为合法文件名，可严格还原。
 */
public final class SnapshotRef implements Comparable<SnapshotRef> {

  private static final Comparator<SnapshotRef> ORDER =
      Comparator.comparingLong(SnapshotRef::index).thenComparingLong(SnapshotRef::term);

  private static final HexFormat HEX = HexFormat.of();

  private final long index;
  private final long term;
  private final String brokerId;

  public SnapshotRef(final long index, final long term, final String brokerId) {
    this.index = index;
    this.term = term;
    this.brokerId = Objects.requireNonNull(brokerId);
  }

  /** 目录名/线上字符串解析；格式不符抛 {@link IllegalArgumentException} */
  public static SnapshotRef parse(final String name) {
    int first = -1;
    int second = -1;
    for (int i = 0; i < name.length(); i++) {
      if (name.charAt(i) == '-') {
        if (first < 0) {
          first = i;
        } else if (second < 0) {
          second = i;
          break;
        }
      }
    }
    if (first < 0 || second < 0) {
      throw new IllegalArgumentException("快照名格式不符(至少 3 段): " + name);
    }
    final long index = Long.parseLong(name.substring(0, first));
    final long term = Long.parseLong(name.substring(first + 1, second));
    return new SnapshotRef(index, term, decodeBrokerId(name.substring(second + 1)));
  }

  public long index() {
    return index;
  }

  public long term() {
    return term;
  }

  public String brokerId() {
    return brokerId;
  }

  @Override
  public int compareTo(final SnapshotRef other) {
    return ORDER.compare(this, other);
  }

  @Override
  public String toString() {
    return index + "-" + term + "-" + encodeBrokerId(brokerId);
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof final SnapshotRef ref)) {
      return false;
    }
    return index == ref.index && term == ref.term && brokerId.equals(ref.brokerId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(index, term, brokerId);
  }

  /** brokerId 段编码：UTF-8 字节十六进制（字母表仅 0-9a-f，不含分隔符） */
  private static String encodeBrokerId(final String brokerId) {
    return HEX.formatHex(brokerId.getBytes(StandardCharsets.UTF_8));
  }

  /** brokerId 段解码；格式不符抛 {@link IllegalArgumentException} */
  private static String decodeBrokerId(final String encoded) {
    try {
      return new String(HEX.parseHex(encoded), StandardCharsets.UTF_8);
    } catch (final IllegalArgumentException e) {
      throw new IllegalArgumentException("brokerId 段 Hex 编码不符: " + encoded, e);
    }
  }
}
