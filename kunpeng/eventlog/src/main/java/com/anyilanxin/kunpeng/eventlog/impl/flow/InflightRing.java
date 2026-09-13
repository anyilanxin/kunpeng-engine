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
 * 在途批环形表：预分配定长槽位（1024），按 lastPosition 取模索引，O(1) 定位、零分配、无装箱。
 *
 * <p><b>只登记持有 AIMD 窗口占位的批次</b>（release 路径据此归还占位 + 采样 RTT）——内部上下文 批次不占任何流控状态，永不因本表溢出而失败。持位批次在途数 ≤
 * 窗口上限（准入保证）， 常规配置（默认上限 1000）下小于环形容量 1024；极端配置下的同槽覆盖由 {@link #add} 返回 被驱逐者、调用方归还占位优雅降级，绝不抛异常。
 *
 * <p>线程约定：add 由提交路径按 position 升序调用；markWritten/markCommitted 由存储回调线程 按提交序调用；release
 * 由处理/失败路径调用。各调用方单线程顺序推进，本类不做跨槽原子性承诺。
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

  /** 登记在途批。目标槽位被占（全量回绕）时覆盖并返回被驱逐者的 position（其占位由调用方 归还）；槽位空闲返回 -1。 */
  long add(final long lastPosition, final long nanos) {
    final int slot = slot(lastPosition);
    final long displaced = state[slot] != EMPTY ? position[slot] : -1;
    position[slot] = lastPosition;
    appendedAtNanos[slot] = nanos;
    state[slot] = APPENDING;
    lastAdded = lastPosition;
    return displaced;
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

  /** 释放槽位（处理完成/失败/驱逐） */
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
