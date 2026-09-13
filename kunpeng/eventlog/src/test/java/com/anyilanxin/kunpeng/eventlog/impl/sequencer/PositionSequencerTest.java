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

import static com.anyilanxin.kunpeng.eventlog.TestEntries.entry;
import static com.anyilanxin.kunpeng.eventlog.TestEntries.entriesOfSize;
import static com.anyilanxin.kunpeng.eventlog.TestEntries.simple;
import static org.assertj.core.api.Assertions.assertThat;

import com.anyilanxin.kunpeng.eventlog.AppendResult;
import com.anyilanxin.kunpeng.eventlog.AppendResult.Appended;
import com.anyilanxin.kunpeng.eventlog.AppendResult.Rejected;
import com.anyilanxin.kunpeng.eventlog.impl.EventLogMetrics;
import com.anyilanxin.kunpeng.eventlog.FlowControlParams;
import com.anyilanxin.kunpeng.eventlog.InMemoryEventStore;
import com.anyilanxin.kunpeng.eventlog.WriteContext;
import com.anyilanxin.kunpeng.eventlog.impl.flow.FlowController;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/** 无锁定序器测试：并发唯一性/连续性 + 提交有序 + 拒绝路径 + 烧毁推进 + 看门狗 */
@DisplayName("PositionSequencer 无锁定序")
class PositionSequencerTest {

  private static final class Ctx {
    final PositionSequencer sequencer;
    final FlowController flow;

    Ctx(final PositionSequencer sequencer, final FlowController flow) {
      this.sequencer = sequencer;
      this.flow = flow;
    }
  }

  private static Ctx sequencer(final InMemoryEventStore store) {
    final FlowController flow = new FlowController(FlowControlParams.disabled(),
        System::nanoTime, EventLogMetrics.noop());
    return new Ctx(new PositionSequencer(store, flow, EventLogMetrics.noop(),
        1024 * 1024, 64, 1, Clock.systemUTC()), flow);
  }

  @Test
  @Timeout(60)
  @DisplayName("16 线程并发: 区间并集 = 1..N 无重叠无 gap, 提交序 == firstPosition 升序")
  void concurrentExactUnion() throws Exception {
    final InMemoryEventStore store = new InMemoryEventStore();
    final Ctx ctx = sequencer(store);
    final PositionSequencer sequencer = ctx.sequencer;
    final int threads = 16;
    final int perThread = 150;
    final ExecutorService pool = Executors.newFixedThreadPool(threads);
    final CountDownLatch start = new CountDownLatch(1);
    final List<Future<List<long[]>>> futures = new ArrayList<>();
    for (int t = 0; t < threads; t++) {
      final int threadId = t;
      futures.add(pool.submit(() -> {
        start.await();
        final Random random = new Random(threadId * 31 + 7);
        final List<long[]> ranges = new ArrayList<>();
        for (int i = 0; i < perThread; i++) {
          final int batch = 1 + random.nextInt(3);
          Appended result = null;
          while (result == null) {
            final AppendResult attempt =
                sequencer.tryAppend(WriteContext.INTERNAL, entriesOfSize(batch), -1);
            if (attempt instanceof Appended appended) {
              result = appended;
            } else if (((AppendResult.Rejected) attempt).reason()
                == AppendResult.RejectionReason.REQUEST_WINDOW_EXHAUSTED) {
              Thread.onSpinWait(); // 背压重试（设计内行为, 不消耗 position）
            } else {
              throw new AssertionError("意外拒绝: " + attempt);
            }
          }
          assertThat(result.lastPosition() - result.firstPosition() + 1).isEqualTo(batch);
          ranges.add(new long[] {result.firstPosition(), result.lastPosition()});
          ctx.flow.onProcessed(result.lastPosition()); // 释放流控占位（在途环容量 1024 < 总量, 须消费）
        }
        return ranges;
      }));
    }
    start.countDown();

    final boolean[] seen = new boolean[threads * perThread * 3 + 8];
    int count = 0;
    for (final Future<List<long[]>> future : futures) {
      for (final long[] range : future.get(30, TimeUnit.SECONDS)) {
        for (long p = range[0]; p <= range[1]; p++) {
          assertThat(seen[(int) p]).as("position %d 重复分配", p).isFalse();
          seen[(int) p] = true;
          count++;
        }
      }
    }
    pool.shutdown();
    for (int p = 1; p <= count; p++) {
      assertThat(seen[p]).as("position %d 缺失", p).isTrue();
    }
    // 提交序: 存储收到的 firstPosition 严格升序（有序提交链核心不变量）
    final List<Long> order = store.appendOrderSnapshot();
    for (int i = 1; i < order.size(); i++) {
      assertThat(order.get(i)).as("提交序破坏 @%d", i).isGreaterThan(order.get(i - 1));
    }
    sequencer.close();
  }

  @Test
  @DisplayName("拒绝路径: 空批 / 超限 / 关闭后")
  void rejections() {
    final InMemoryEventStore store = new InMemoryEventStore();
    final PositionSequencer sequencer = sequencer(store).sequencer;

    assertThat(sequencer.tryAppend(WriteContext.USER_COMMAND, List.of()))
        .isInstanceOf(Rejected.class);

    final PositionSequencer tiny = new PositionSequencer(store,
        new FlowController(FlowControlParams.disabled(), System::nanoTime,
            EventLogMetrics.noop()),
        EventLogMetrics.noop(), 10, 64, 1, Clock.systemUTC());
    assertThat(tiny.tryAppend(WriteContext.INTERNAL,
        List.of(entry(1, -1, false, "meta-123456", "value-12345678"))))
        .isInstanceOf(Rejected.class);

    sequencer.close();
    assertThat(sequencer.tryAppend(WriteContext.INTERNAL, List.of(simple())))
        .isInstanceOf(Rejected.class);
  }

  @Test
  @Timeout(30)
  @DisplayName("失败烧毁: 注入同步失败产生 gap, 后续批继续推进")
  void burnOnFailure() {
    final InMemoryEventStore store = new InMemoryEventStore();
    final PositionSequencer sequencer = sequencer(store).sequencer;

    assertThat(sequencer.tryAppend(WriteContext.INTERNAL, List.of(simple())))
        .isInstanceOf(Appended.class);
    store.failNextAppends(1);
    // 第二批存储同步失败 → position 2 烧毁（定序仍成功, 与旧实现语义一致）
    assertThat(sequencer.tryAppend(WriteContext.INTERNAL, List.of(simple())))
        .isInstanceOf(Appended.class);
    // 第三批从 3 继续
    final AppendResult third =
        sequencer.tryAppend(WriteContext.INTERNAL, List.of(simple()));
    assertThat(((Appended) third).firstPosition()).isEqualTo(3);
    sequencer.close();
  }

  @Test
  @Timeout(30)
  @DisplayName("看门狗: 过期 RESERVED 槽位被烧毁并放行后续批")
  void watchdogBurn() {
    final InMemoryEventStore store = new InMemoryEventStore();
    final PositionSequencer sequencer = sequencer(store).sequencer;
    sequencer.reserveExpiredSlotForTest(1); // 模拟写线程卡死于 reserve 后

    // 正常批从 2 起号; drain 遇到过期槽 1 → 烧毁推进 → 提交本批
    final AppendResult result =
        sequencer.tryAppend(WriteContext.INTERNAL, List.of(simple()));
    assertThat(result).isInstanceOf(Appended.class);
    assertThat(((Appended) result).firstPosition()).isEqualTo(2);
    assertThat(store.appendOrderSnapshot()).containsExactly(2L);
    sequencer.close();
  }

  @Test
  @DisplayName("canAppend 探测与关闭")
  void canAppend() {
    final InMemoryEventStore store = new InMemoryEventStore();
    final PositionSequencer sequencer = sequencer(store).sequencer;
    assertThat(sequencer.canAppend(1, 16)).isTrue();
    sequencer.close();
    assertThat(sequencer.canAppend(1, 16)).isFalse();
  }
}
