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
package com.anyilanxin.kunpeng.scheduler.core;

import java.util.Arrays;

/**
 * 带索引的二叉最小堆：按（截止时间，入堆序号）排序，同截止时间先入堆的在前。
 *
 * <p>槽位记住自己的下标，按槽位删除是 O(log n)；最早到期直接看堆顶。非线程安全，只供 owner runner 线程使用。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
final class TimerQueue {

  private TimerSlot[] heap = new TimerSlot[64];
  private int size;

  boolean isEmpty() {
    return size == 0;
  }

  int size() {
    return size;
  }

  /** 最早到期的槽位；空堆为 null */
  TimerSlot peek() {
    return isEmpty() ? null : heap[0];
  }

  /** 堆内下标处的槽位（测试核对下引用） */
  TimerSlot slotAt(final int index) {
    return heap[index];
  }

  void offer(final TimerSlot slot) {
    if (size == heap.length) {
      heap = Arrays.copyOf(heap, size * 2);
    }
    size++;
    siftUp(slot, size - 1);
  }

  /** 取出最早到期的槽位；空堆为 null */
  TimerSlot poll() {
    final TimerSlot head = peek();
    if (head != null) {
      removeAt(0);
    }
    return head;
  }

  /** 按槽位删除；不在堆中则什么都不做 */
  void remove(final TimerSlot slot) {
    final int index = slot.getHeapIndex();
    if (index >= 0) {
      removeAt(index);
    }
  }

  private void removeAt(final int index) {
    heap[index].setHeapIndex(-1);
    size--;
    if (index == size) {
      heap[size] = null;
      return;
    }
    // 末尾的叶子顶到被删位置：它可能比原位置的孩子大（向下沉），也可能比新的父节点小（向上浮）
    final TimerSlot moved = heap[size];
    heap[size] = null;
    siftDown(moved, index);
    if (heap[index] == moved) {
      siftUp(moved, index);
    }
  }

  private void siftUp(final TimerSlot slot, int index) {
    while (index > 0) {
      final int parent = (index - 1) >>> 1;
      if (!precedes(slot, heap[parent])) {
        break;
      }
      heap[index] = heap[parent];
      heap[index].setHeapIndex(index);
      index = parent;
    }
    heap[index] = slot;
    slot.setHeapIndex(index);
  }

  private void siftDown(final TimerSlot slot, int index) {
    final int half = size >>> 1;
    while (index < half) {
      int child = index * 2 + 1;
      final int right = child + 1;
      if (right < size && precedes(heap[right], heap[child])) {
        child = right;
      }
      if (!precedes(heap[child], slot)) {
        break;
      }
      heap[index] = heap[child];
      heap[index].setHeapIndex(index);
      index = child;
    }
    heap[index] = slot;
    slot.setHeapIndex(index);
  }

  private static boolean precedes(final TimerSlot a, final TimerSlot b) {
    return a.getDeadlineMillis() < b.getDeadlineMillis()
        || (a.getDeadlineMillis() == b.getDeadlineMillis() && a.getSequence() < b.getSequence());
  }
}
