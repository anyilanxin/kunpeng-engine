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

import com.anyilanxin.kunpeng.scheduler.future.ActorFuture;
import com.anyilanxin.kunpeng.scheduler.future.CompletableActorFuture;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/** 调度核冒烟：生命周期/内外提交/close/条件/定时器/多 actor 偷取 */
@DisplayName("scheduler 冒烟")
class SmokeTest {

  private ActorScheduler scheduler;

  @AfterEach
  void tearDown() {
    if (scheduler != null) {
      scheduler.close();
    }
  }

  static class RecordingActor extends Actor {
    final List<String> events = new CopyOnWriteArrayList<>();
    final CountDownLatch started = new CountDownLatch(1);
    volatile ActorCondition condition;
    final CountDownLatch conditionFired = new CountDownLatch(1);
    final CountDownLatch timerFired = new CountDownLatch(1);
    final CountDownLatch internalRun = new CountDownLatch(1);

    @Override
    protected void onActorStarting() {
      events.add("starting");
    }

    @Override
    protected void onActorStarted() {
      events.add("started");
      started.countDown();
      condition = actor.onCondition("cond", () -> {
        events.add("condition");
        conditionFired.countDown();
      });
      actor.schedule(java.time.Duration.ofMillis(50), () -> {
        events.add("timer");
        timerFired.countDown();
      });
    }

    @Override
    protected void onActorClosing() {
      events.add("closing");
    }

    @Override
    protected void onActorClosed() {
      events.add("closed");
    }
  }

  @Test
  @Timeout(30)
  @DisplayName("全链路: 启动相位序 + 外部/内部提交 + 条件 + 定时器 + 关闭相位序")
  void fullLifecycle() throws Exception {
    scheduler = ActorScheduler.newActorScheduler()
        .setCpuBoundActorThreadCount(2).setSchedulerName("smoke").build();
    scheduler.start();

    final RecordingActor actor = new RecordingActor();
    final ActorFuture<Void> starting = scheduler.submitActor(actor);
    starting.get(10, TimeUnit.SECONDS);
    assertThat(actor.started.await(10, TimeUnit.SECONDS)).isTrue();
    assertThat(actor.events).containsSubsequence("starting", "started");

    // 外部提交
    final var external = actor.call(() -> 42);
    assertThat(external.get(10, TimeUnit.SECONDS)).isEqualTo(42);

    // 内部提交（fast-lane）
    actor.run(() -> {
      actor.run(() -> actor.internalRun.countDown());
    });
    assertThat(actor.internalRun.await(10, TimeUnit.SECONDS)).isTrue();

    // 条件（外部 signal）
    actor.condition.signal();
    assertThat(actor.conditionFired.await(10, TimeUnit.SECONDS)).isTrue();

    // 定时器
    assertThat(actor.timerFired.await(10, TimeUnit.SECONDS)).isTrue();

    // 关闭
    actor.close();
    assertThat(actor.events).containsSubsequence("closing", "closed");
    assertThat(actor.isActorClosed()).isTrue();

    // close 后提交 → 立即失败
    final var after = actor.call(() -> 1);
    assertThat(after.isCompletedExceptionally()).isTrue();
  }

  @Test
  @Timeout(60)
  @DisplayName("多 actor 并发偷取: 8 actor × 1000 job 全部执行且各自串行")
  void stealingAndSerialExecution() throws Exception {
    scheduler = ActorScheduler.newActorScheduler()
        .setCpuBoundActorThreadCount(3).setSchedulerName("steal").build();
    scheduler.start();

    final int actors = 8;
    final int jobs = 1000;
    final var latches = new CountDownLatch[actors];
    final var counters = new int[actors];
    final var violations = new CopyOnWriteArrayList<String>();

    for (int a = 0; a < actors; a++) {
      latches[a] = new CountDownLatch(jobs);
      final int idx = a;
      final Actor actor = new Actor() {
        int count;
        int inside;

        @Override
        protected void onActorStarted() {
          for (int i = 0; i < jobs; i++) {
            actor.run(() -> {
              // 串行性检查: 重入计数不为 0 说明并发执行
              if (inside != 0) {
                violations.add("actor-" + idx + " concurrent execution");
              }
              inside++;
              count++;
              inside--;
              if (count == jobs) {
                counters[idx] = count;
              }
              latches[idx].countDown();
            });
          }
        }
      };
      scheduler.submitActor(actor);
    }

    for (final var latch : latches) {
      assertThat(latch.await(30, TimeUnit.SECONDS)).isTrue();
    }
    assertThat(violations).isEmpty();
    for (final int count : counters) {
      assertThat(count).isEqualTo(jobs);
    }
  }

  @Test
  @Timeout(30)
  @DisplayName("runOnCompletion: future 完成回投 actor 上下文执行")
  void runOnCompletion() throws Exception {
    scheduler = ActorScheduler.newActorScheduler()
        .setCpuBoundActorThreadCount(1).setSchedulerName("completion").build();
    scheduler.start();

    final var result = new CompletableActorFuture<String>();
    final var latch = new CountDownLatch(1);
    final var threadNames = new CopyOnWriteArrayList<String>();

    final Actor actor = new Actor() {
      @Override
      protected void onActorStarted() {
        actor.runOnCompletion(result, (v, e) -> {
          threadNames.add(Thread.currentThread().getName());
          latch.countDown();
        });
      }
    };
    scheduler.submitActor(actor).get(10, TimeUnit.SECONDS);

    // 外部线程完成 future
    Thread.sleep(100);
    result.complete("ok");

    assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
    // 回调在 actor 的 runner 线程执行（而非完成方线程）
    assertThat(threadNames.get(0)).contains("completion");
  }
}
