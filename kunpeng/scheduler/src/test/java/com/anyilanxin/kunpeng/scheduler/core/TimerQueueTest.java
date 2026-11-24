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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 带索引最小堆的顺序不变量：随机插入、按槽位删除、弹出之后，堆顶始终是（截止时间，入堆序号）最小的那个，下标始终与位置一致。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@DisplayName("定时器最小堆")
class TimerQueueTest {

  @Test
  @DisplayName("随机插入与删除后堆顶、下标、出堆顺序始终正确")
  void randomOperationsKeepTheHeapOrdered() {
    final var random = new Random(20261124);
    for (int round = 0; round < 10; round++) {
      final var queue = new TimerQueue();
      // 参照用同样的比较规则；截止时间只取很小的范围，保证大量相同截止时间
      final var reference =
          new java.util.PriorityQueue<TimerSlot>(
              Comparator.comparingLong(TimerSlot::getDeadlineMillis)
                  .thenComparingLong(TimerSlot::getSequence));
      final List<TimerSlot> live = new ArrayList<>();
      final List<TimerSlot> gone = new ArrayList<>();
      long sequence = 0;
      final int operations = 4_000;
      for (int op = 0; op < operations; op++) {
        final int choice = random.nextInt(100);
        if (live.isEmpty() || choice < 55) {
          final var slot = new TimerSlot(() -> {});
          slot.setDeadlineMillis(random.nextInt(8), sequence++);
          queue.offer(slot);
          reference.add(slot);
          live.add(slot);
        } else if (choice < 90) {
          final var victim = live.remove(random.nextInt(live.size()));
          queue.remove(victim);
          reference.remove(victim);
          gone.add(victim);
          assertThat(victim.getHeapIndex()).as("删除后不在堆中").isEqualTo(-1);
        } else {
          final var head = queue.poll();
          live.remove(head);
          assertThat(head).isSameAs(reference.poll());
          gone.add(head);
        }
        if (!gone.isEmpty() && random.nextInt(4) == 0) {
          // 重复删除已出堆的槽位：什么都不发生
          final int before = queue.size();
          queue.remove(gone.get(random.nextInt(gone.size())));
          assertThat(queue.size()).as("重复删除不改变堆").isEqualTo(before);
        }
        assertThat(queue.size()).isEqualTo(reference.size());
        if (!reference.isEmpty()) {
          assertThat(queue.peek()).as("堆顶").isSameAs(reference.peek());
        }
        if (op % 200 == 0) {
          for (final var slot : live) {
            assertThat(queue.slotAt(slot.getHeapIndex())).as("下标与位置一致").isSameAs(slot);
          }
        }
      }
      // 全部弹出：顺序与参照一致，且都标记为不在堆中
      final var drained = new ArrayList<TimerSlot>();
      final var expected = new ArrayList<TimerSlot>();
      TimerSlot slot;
      while ((slot = queue.poll()) != null) {
        drained.add(slot);
        expected.add(reference.poll());
        assertThat(slot.getHeapIndex()).isEqualTo(-1);
      }
      assertThat(drained).containsExactlyElementsOf(expected);
      assertThat(queue.isEmpty()).isTrue();
      assertThat(reference.isEmpty()).isTrue();
    }
  }
}
