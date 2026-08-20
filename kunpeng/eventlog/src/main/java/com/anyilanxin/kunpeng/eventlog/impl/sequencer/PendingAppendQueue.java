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
package com.anyilanxin.kunpeng.eventlog.impl.sequencer;

import com.anyilanxin.kunpeng.structpack.buffer.DirectBufferWriter;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 有序提交链：预分配开放寻址槽位表（容量 = 槽数/2，负载 ≤50% 探测短）。
 *
 * <p>三段式生命周期：{@code tryClaim}（原子领取槽位，容量检查内聚——消除 check-then-act 竞态）→ {@code reserve}（position
 * 预约后回填区间与截止时间）→ {@code fulfill}（帧就绪，payload=null 表示烧毁）。提交线程按 watermark+1 的 firstPosition
 * 找槽逐个推进：READY 提交、RESERVED 未到期等待、超期看门狗烧毁、 无槽停止。
 *
 * <p>槽位按领取序开放寻址分配（非 firstPosition 取模——批跨度可超过槽数， 取模会碰撞）；{@code slotOfFirst} 供 drain 反查。
 */
final class PendingAppendQueue {

  private static final byte EMPTY = 0;
  private static final byte CLAIMED = 1; // 已领槽, position 未回填
  private static final byte RESERVED = 2; // position 已回填, 帧未就绪
  private static final byte READY = 3; // 帧就绪(payload 可为 null=烧毁)
  private static final byte SUBMITTING = 4;

  private final int capacity;
  private final int mask;
  private final byte[] states;
  private final long[] firstPositions;
  private final long[] lastPositions;
  private final long[] deadlines;
  private final DirectBufferWriter[] payloads;
  private final AtomicInteger used = new AtomicInteger();
  private long claimCursor;

  PendingAppendQueue(final int capacity) {
    if (capacity < 1) {
      throw new IllegalArgumentException("槽数必须 ≥1: " + capacity);
    }
    this.capacity = capacity;
    int size = Integer.highestOneBit(Math.max(capacity * 2, 4)) << 1; // 2 的幂 > 2×capacity
    this.mask = size - 1;
    this.states = new byte[size];
    this.firstPositions = new long[size];
    this.lastPositions = new long[size];
    this.deadlines = new long[size];
    this.payloads = new DirectBufferWriter[size];
  }

  /** 原子领取槽位（容量检查内聚, 消除竞态）；无容量返回 -1 */
  synchronized int tryClaim() {
    if (used.get() >= capacity) {
      return -1;
    }
    int slot = (int) (claimCursor & mask);
    while (states[slot] != EMPTY) {
      slot = (slot + 1) & mask;
    }
    claimCursor++;
    states[slot] = CLAIMED;
    used.incrementAndGet();
    return slot;
  }

  /** 领取后放弃（流控拒绝等路径） */
  synchronized void releaseClaim(final int slot) {
    states[slot] = EMPTY;
    used.decrementAndGet();
  }

  /** position 预约后回填（deadline 内必须 fulfill, 否则看门狗烧毁） */
  synchronized void reserve(
      final int slot, final long first, final long last, final long deadlineNanos) {
    if (states[slot] != CLAIMED) {
      throw new IllegalStateException("reserve 状态违例: slot=" + slot);
    }
    firstPositions[slot] = first;
    lastPositions[slot] = last;
    deadlines[slot] = deadlineNanos;
    payloads[slot] = null;
    states[slot] = RESERVED;
  }

  /** 帧就绪；payload = null 表示该区间烧毁 */
  synchronized void fulfill(final long first, final DirectBufferWriter frame) {
    final int slot = slotOfFirst(first);
    if (slot < 0 || states[slot] != RESERVED) {
      throw new IllegalStateException("fulfill 状态违例: first=" + first);
    }
    payloads[slot] = frame;
    states[slot] = READY;
  }

  /** 按 firstPosition 反查槽位（drain 用）；不存在返回 -1 */
  synchronized int slotOfFirst(final long first) {
    for (int slot = 0; slot < states.length; slot++) {
      if (states[slot] >= RESERVED && firstPositions[slot] == first) {
        return slot;
      }
    }
    return -1;
  }

  synchronized boolean isReady(final int slot) {
    return states[slot] == READY;
  }

  synchronized long deadline(final int slot) {
    return deadlines[slot];
  }

  synchronized long lastOf(final int slot) {
    return lastPositions[slot];
  }

  /** 取走帧载荷并进入提交态（drain 独占语义） */
  synchronized DirectBufferWriter take(final int slot) {
    final DirectBufferWriter frame = payloads[slot];
    states[slot] = SUBMITTING;
    return frame;
  }

  /** 释放槽位 */
  synchronized void free(final int slot) {
    payloads[slot] = null;
    states[slot] = EMPTY;
    used.decrementAndGet();
  }

  synchronized boolean hasCapacity() {
    return used.get() < capacity;
  }

  synchronized int used() {
    return used.get();
  }
}
