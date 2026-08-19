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

import java.util.Comparator;
import java.util.Objects;

/**
 * 快照身份：{@code <index>-<term>-<processedPosition>-<exportedPosition>-<brokerId>-<checksum>}。
 *
 * <p>排序 index → processedPosition → exportedPosition。解析按前四个 {@code -} 切分，余段归 brokerId（允许 brokerId 含
 * {@code -}），末段为 checksum（暂存期允许缺省）。
 */
public final class SnapshotRef implements Comparable<SnapshotRef> {

  private static final Comparator<SnapshotRef> ORDER =
      Comparator.comparingLong(SnapshotRef::index)
          .thenComparingLong(SnapshotRef::processedPosition)
          .thenComparingLong(SnapshotRef::exportedPosition);

  private final long index;
  private final long term;
  private final long processedPosition;
  private final long exportedPosition;
  private final String brokerId;
  private String checksum;

  public SnapshotRef(
      final long index,
      final long term,
      final long processedPosition,
      final long exportedPosition,
      final String brokerId) {
    this.index = index;
    this.term = term;
    this.processedPosition = processedPosition;
    this.exportedPosition = exportedPosition;
    this.brokerId = Objects.requireNonNull(brokerId);
    this.checksum = "";
  }

  /** 目录名/线上字符串解析；格式不符抛 {@link IllegalArgumentException} */
  public static SnapshotRef parse(final String name) {
    int first = -1;
    int second = -1;
    int third = -1;
    int fourth = -1;
    for (int i = 0; i < name.length(); i++) {
      if (name.charAt(i) == '-') {
        if (first < 0) {
          first = i;
        } else if (second < 0) {
          second = i;
        } else if (third < 0) {
          third = i;
        } else if (fourth < 0) {
          fourth = i;
          break;
        }
      }
    }
    if (first < 0 || second < 0 || third < 0 || fourth < 0) {
      throw new IllegalArgumentException("快照名格式不符(至少 5 段): " + name);
    }
    final long index = Long.parseLong(name.substring(0, first));
    final long term = Long.parseLong(name.substring(first + 1, second));
    final long processed = Long.parseLong(name.substring(second + 1, third));
    final long exported = Long.parseLong(name.substring(third + 1, fourth));
    final String tail = name.substring(fourth + 1);
    final int lastDash = tail.lastIndexOf('-');
    if (lastDash < 0) {
      throw new IllegalArgumentException("快照名缺少 brokerId/checksum 段: " + name);
    }
    final var ref = new SnapshotRef(index, term, processed, exported, tail.substring(0, lastDash));
    ref.checksum = tail.substring(lastDash + 1);
    return ref;
  }

  public long index() {
    return index;
  }

  public long term() {
    return term;
  }

  public long processedPosition() {
    return processedPosition;
  }

  public long exportedPosition() {
    return exportedPosition;
  }

  public String brokerId() {
    return brokerId;
  }

  public String checksum() {
    return checksum;
  }

  /** 落档时写入综合校验值（一次性） */
  void checksum(final String value) {
    this.checksum = value;
  }

  @Override
  public int compareTo(final SnapshotRef other) {
    return ORDER.compare(this, other);
  }

  @Override
  public String toString() {
    return index
        + "-"
        + term
        + "-"
        + processedPosition
        + "-"
        + exportedPosition
        + "-"
        + brokerId
        + "-"
        + checksum;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof SnapshotRef ref)) {
      return false;
    }
    return index == ref.index
        && term == ref.term
        && processedPosition == ref.processedPosition
        && exportedPosition == ref.exportedPosition
        && brokerId.equals(ref.brokerId)
        && checksum.equals(ref.checksum);
  }

  @Override
  public int hashCode() {
    return Objects.hash(index, term, processedPosition, exportedPosition, brokerId, checksum);
  }
}
