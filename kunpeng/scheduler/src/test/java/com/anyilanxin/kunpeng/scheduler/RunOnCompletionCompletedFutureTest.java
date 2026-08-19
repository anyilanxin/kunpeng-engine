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

import com.anyilanxin.kunpeng.scheduler.future.CompletableActorFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/** 回归：runOnCompletion 绑定已完成 future 时必须立即消费（启动链停止事故场景） */
@DisplayName("已完成 future 的续接")
class RunOnCompletionCompletedFutureTest {

  private ActorScheduler scheduler;

  @AfterEach
  void tearDown() {
    if (scheduler != null) {
      scheduler.close();
    }
  }

  @Test
  @Timeout(30)
  @DisplayName("回归: runOnCompletion(已完成 future) —— actor 空闲时回调也必须执行")
  void alreadyCompletedFutureWakesIdleActor() throws Exception {
    scheduler = ActorScheduler.newActorScheduler()
        .setCpuBoundActorThreadCount(1).setSchedulerName("idle-completed").build();
    scheduler.start();

    final var done = new CountDownLatch(3);
    final var order = new CopyOnWriteArrayList<String>();
    // 模拟 StartupProcess 形态: 已完成的 step future 链式续接
    final var step1 = CompletableActorFuture.<String>completed("s1");
    final var step2 = CompletableActorFuture.<String>completed("s2");
    final var step3 = new CompletableActorFuture<String>();
    step3.complete("s3");

    final Actor actor = new Actor() {
      @Override
      protected void onActorStarted() {
        actor.runOnCompletion(step1, (v, e) -> {
          order.add(v);
          done.countDown();
        });
        actor.runOnCompletion(step2, (v, e) -> {
          order.add(v);
          done.countDown();
        });
        actor.runOnCompletion(step3, (v, e) -> {
          order.add(v);
          done.countDown();
        });
        // onActorStarted 返回后 cell 排空休眠——若无即时唤醒, 三个回调全部饿死
      }
    };
    scheduler.submitActor(actor).get(10, TimeUnit.SECONDS);

    assertThat(done.await(10, TimeUnit.SECONDS)).as("已完成 future 的续接必须执行").isTrue();
    assertThat(order).containsExactly("s1", "s2", "s3");
    assertThat(actor.isActorClosed()).isFalse();
  }

  @Test
  @Timeout(30)
  @DisplayName("外部线程在注册之后完成 future（原有路径不回归）")
  void laterCompletedFutureStillWorks() throws Exception {
    scheduler = ActorScheduler.newActorScheduler()
        .setCpuBoundActorThreadCount(1).setSchedulerName("later-completed").build();
    scheduler.start();

    final var pending = new CompletableActorFuture<String>();
    final var done = new CountDownLatch(1);
    final Actor actor = new Actor() {
      @Override
      protected void onActorStarted() {
        actor.runOnCompletion(pending, (v, e) -> done.countDown());
      }
    };
    scheduler.submitActor(actor).get(10, TimeUnit.SECONDS);
    Thread.sleep(100); // 确保 cell 已休眠
    pending.complete("late"); // 外部线程完成
    assertThat(done.await(10, TimeUnit.SECONDS)).isTrue();
  }
}
