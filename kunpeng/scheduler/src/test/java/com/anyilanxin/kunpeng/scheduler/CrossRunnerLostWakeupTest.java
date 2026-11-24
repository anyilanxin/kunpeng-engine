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
package com.anyilanxin.kunpeng.scheduler;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * 多 runner 线程下的跨线程唤醒回归：调度门休眠写成 release 写时缺 StoreLoad 屏障，owner 的复查与并发提交者的 CAS
 * 会双向读到旧值而都不入队，消息滞留到该 actor 下一次唤醒。
 *
 * <p>复现形态必须是"2 条载体线程 + 只有一对 actor"：另一条线程空闲自旋，被路由过去的 peer 约百纳秒内即被捡起执行，
 * 正好落进 owner 的收尾窗口。载体都在忙时 peer 要排队等待，碰不上这个窗口。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@DisplayName("多 runner 跨线程唤醒")
class CrossRunnerLostWakeupTest {

  /** 修复前每条链 10 万跳内卡死的概率超过 90%（实测 30 条链卡死 28 条） */
  private static final int CHAINS = 10;
  private static final int HOPS = 300_000;

  @Test
  @Timeout(120)
  @DisplayName("2 条载体线程上两个 actor 互跳，10 条链共 300 万次，没有消息滞留")
  void hopChainsAcrossRunnersAllComplete() throws Exception {
    for (int chain = 1; chain <= CHAINS; chain++) {
      final ActorScheduler scheduler =
          ActorScheduler.newActorScheduler()
              .setCpuBoundActorThreadCount(2)
              .setIoBoundActorThreadCount(1)
              .setSchedulerName("lost-wakeup-" + chain)
              .build();
      scheduler.start();
      try {
        final ActorControl first = submit(scheduler, "a");
        final ActorControl second = submit(scheduler, "b");
        final AtomicInteger left = new AtomicInteger(HOPS);
        final CountDownLatch done = new CountDownLatch(1);
        first.run(() -> hop(first, second, left, done));
        assertThat(done.await(15, TimeUnit.SECONDS))
            .as(
                "第 %d 条链卡死：%d 跳未执行（队列里有任务、门停在 WAITING、没有载体持有该 cell）",
                chain,
                left.get())
            .isTrue();
      } finally {
        scheduler.close();
      }
    }
  }

  private static ActorControl submit(final ActorScheduler scheduler, final String name)
      throws Exception {
    final AtomicReference<ActorControl> control = new AtomicReference<>();
    scheduler.submitActor(
            new Actor() {
              @Override
              public String getName() {
                return name;
              }

              @Override
              protected void onActorStarting() {
                control.set(actor);
              }
            })
        .get(10, TimeUnit.SECONDS);
    return control.get();
  }

  private static void hop(
      final ActorControl from,
      final ActorControl to,
      final AtomicInteger left,
      final CountDownLatch done) {
    if (left.decrementAndGet() <= 0) {
      done.countDown();
      return;
    }
    to.run(() -> hop(to, from, left, done));
  }
}
