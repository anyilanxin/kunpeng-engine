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
package com.anyilanxin.kunpeng.scheduler.clock;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * 默认时钟被别的线程读取时也必须前进：broker 把启动步骤执行线程上的时钟对象存成了全局时间源， 引擎与检查器都从别的线程读它。
 * 时钟只在所属载体线程上采样，所属线程忙于一个长任务时读侧如果只拿缓存值，引擎时间就会停住—— 期间创建的定时器到期时间被算早，时钟恢复后立刻触发。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@DisplayName("默认时钟跨线程读取")
class DefaultActorClockTest {

  @Test
  @Timeout(30)
  @DisplayName("所属线程长时间不采样时，其它线程读到的时间仍然跟着真实时间走")
  void timeKeepsAdvancingWhileTheOwningThreadStopsSampling() throws Exception {
    final var clock = new DefaultActorClock();
    final var ownerSampling = new CountDownLatch(1);
    final var ownerMayExit = new CountDownLatch(1);
    final var owner =
        new Thread(
            () -> {
              clock.update();
              ownerSampling.countDown();
              try {
                ownerMayExit.await();
              } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
              }
            });
    owner.start();
    ownerSampling.await();

    Thread.sleep(300);
    final long lag = System.currentTimeMillis() - clock.getTimeMillis();
    ownerMayExit.countDown();
    owner.join();

    assertThat(lag).as("时钟落后真实时间的毫秒数").isLessThan(50);
  }

  @Test
  @DisplayName("连续采样期间读数单调不减，且与真实时间相差不到 5ms")
  void sampledTimeIsMonotonicAndCloseToWallClock() {
    final var clock = new DefaultActorClock();
    long lastSampled = clock.getTimeMillis();
    final long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(200);
    while (System.nanoTime() < deadline) {
      if (clock.update()) {
        final long now = clock.getTimeMillis();
        assertThat(now).isGreaterThanOrEqualTo(lastSampled);
        assertThat(Math.abs(now - System.currentTimeMillis())).isLessThan(5);
        lastSampled = now;
      }
    }
  }
}
