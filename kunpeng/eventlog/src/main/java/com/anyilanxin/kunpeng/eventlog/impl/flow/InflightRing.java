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
package com.anyilanxin.kunpeng.eventlog.impl.flow;

/**
 * 在途批环形表：预分配定长槽位（1024），按 lastPosition 取模索引，O(1) 定位、 零分配、无装箱——替代哈希表 + 全表清扫方案。
 *
 * <p>正确性依据：position 严格递增；在途批数量被有序提交链槽位上限（默认 64）约束， 同一槽位的复用必然发生在旧批释放之后（开发期冲突即抛）。
 *
 * <p>线程约定：add 由提交路径按 position 升序调用；markWritten/markCommitted 由 存储回调线程按提交序调用；release
 * 由处理/失败路径调用。所有状态字段使用 volatile 语义（数组槽位写对其他线程可见性依赖调用方的 happens-before 边界—— 各自单线程顺序推进，本类不做跨槽原子性承诺）。
 */
final class InflightRing {

  private static final int SIZE = 1024;
  private static final int MASK = SIZE - 1;

  private static final byte EMPTY = 0;
  private static final byte APPENDING = 1;
  private static final byte WRITTEN = 2;
  private static final byte COMMITTED = 3;

  private final long[] position = new long[SIZE];
  private final long[] appendedAtNanos = new long[SIZE];
  private final byte[] state = new byte[SIZE];
  private long lastAdded = Long.MIN_VALUE;

  /** 槽位被占时的陈旧检测：占用者滞留超过 maxAge 视为泄漏（消费方未释放）， 返回其 position 供驱逐；否则 -1（真冲突，仍由 add 抛出）。 */
  long staleOccupant(final long lastPosition, final long nowNanos, final long maxAgeNanos) {
    final int slot = slot(lastPosition);
    if (state[slot] == EMPTY) {
      return -1;
    }
    return nowNanos - appendedAtNanos[slot] > maxAgeNanos ? position[slot] : -1;
  }

  void add(final long lastPosition, final long nanos) {
    final int slot = slot(lastPosition);
    if (state[slot] != EMPTY) {
      throw new IllegalStateException(
          "在途槽位复用冲突: position=" + lastPosition + " 占用者=" + position[slot] + "（在途批数超过环形容量?）");
    }
    position[slot] = lastPosition;
    appendedAtNanos[slot] = nanos;
    state[slot] = APPENDING;
    lastAdded = lastPosition;
  }

  void markWritten(final long lastPosition) {
    final int slot = slot(lastPosition);
    if (state[slot] == APPENDING) {
      state[slot] = WRITTEN;
    }
  }

  void markCommitted(final long lastPosition) {
    final int slot = slot(lastPosition);
    if (state[slot] == APPENDING || state[slot] == WRITTEN) {
      state[slot] = COMMITTED;
    }
  }

  boolean isActive(final long lastPosition) {
    return state[slot(lastPosition)] != EMPTY;
  }

  /** 该批追加时刻（RTT 采样）；不在途返回 -1 */
  long appendedAt(final long lastPosition) {
    final int slot = slot(lastPosition);
    return state[slot] != EMPTY ? appendedAtNanos[slot] : -1;
  }

  /** 释放槽位（处理完成/失败烧毁） */
  void release(final long lastPosition) {
    state[slot(lastPosition)] = EMPTY;
  }

  /** >= from 的最小活跃 position；无返回 {@link Long#MAX_VALUE} */
  long nextActive(final long from) {
    for (long p = Math.max(from, lastAdded - SIZE + 1); p <= lastAdded; p++) {
      if (p >= from && state[slot(p)] != EMPTY) {
        return p;
      }
    }
    return Long.MAX_VALUE;
  }

  private static int slot(final long lastPosition) {
    return (int) (lastPosition & MASK);
  }
}
