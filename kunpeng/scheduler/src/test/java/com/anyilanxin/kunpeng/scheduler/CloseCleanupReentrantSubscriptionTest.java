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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * 回归：actor 关闭清理（runOnActorClosedAndCleanup）对未消费 future 订阅做 failConsumer 时，
 * 消费方在同步回调里可能再次向本 cell 注册订阅（生产场景：StartupProcess 关闭链某步失败后，
 * 继续下一步时 runOnCompletion 重入）。迭代器遍历会抛 ConcurrentModificationException 并把
 * actor 打成 FAILED；索引循环可容忍遍历中增删，且尾部新增的订阅同样被兜底异常完成，续接链
 * 不会静默丢失。
 */
@DisplayName("actor 关闭清理时的重入订阅")
class CloseCleanupReentrantSubscriptionTest {

  private ActorScheduler scheduler;

  @AfterEach
  void tearDown() {
    if (scheduler != null) {
      scheduler.close();
    }
  }

  @Test
  @Timeout(30)
  @DisplayName("回归: 清理回调内再注册的订阅被异常完成而非 CME")
  void cleanupSurvivesReentrantSubscription() throws Exception {
    scheduler = ActorScheduler.newActorScheduler()
        .setCpuBoundActorThreadCount(1).setSchedulerName("close-reentry").build();
    scheduler.start();

    final Actor actor = Actor.newActor().name("cleanup-reentry").build();
    scheduler.submitActor(actor).get(10, TimeUnit.SECONDS);

    final AtomicReference<Throwable> firstError = new AtomicReference<>();
    final AtomicReference<Throwable> secondError = new AtomicReference<>();
    final AtomicReference<Throwable> lateError = new AtomicReference<>();

    actor.run(
            () -> {
              final var pending1 = actor.createFuture();
              final var pending2 = actor.createFuture();
              final var late = actor.createFuture();
              actor.runOnCompletion(
                  pending1,
                  (v, e) -> {
                    firstError.set(e);
                    // 模拟关闭链失败回调里继续下一步: 同步向本 cell 再注册一个订阅
                    actor.runOnCompletion(late, (v2, e2) -> lateError.set(e2));
                  });
              actor.runOnCompletion(pending2, (v, e) -> secondError.set(e));
            })
        .get(10, TimeUnit.SECONDS);

    actor.closeAsync().get(10, TimeUnit.SECONDS);

    assertThat(firstError.get())
        .as("清理时未消费订阅必须被异常完成")
        .isInstanceOf(IllegalStateException.class);
    assertThat(secondError.get())
        .as("清理时未消费订阅必须被异常完成")
        .isInstanceOf(IllegalStateException.class);
    // 关键: 重入注册的订阅同样被兜底异常完成, 且清理过程不得抛 CME 把 actor 打成 FAILED
    assertThat(lateError.get())
        .as("清理期间重入注册的订阅也必须被异常完成(而非静默丢弃或 CME)")
        .isInstanceOf(IllegalStateException.class);
  }
}
