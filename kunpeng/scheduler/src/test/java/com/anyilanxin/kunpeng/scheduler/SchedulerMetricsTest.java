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

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/** 调度器指标采集验证（生命周期/job/定时器/拒绝） */
@DisplayName("scheduler 指标采集")
class SchedulerMetricsTest {

  private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
  private ActorScheduler scheduler;

  @AfterEach
  void tearDown() {
    if (scheduler != null) {
      scheduler.close();
    }
  }

  @Test
  @Timeout(30)
  @DisplayName("actor 生命周期 + job 提交/执行按池打标")
  void actorAndJobMetrics() throws Exception {
    scheduler = ActorScheduler.newActorScheduler()
        .setCpuBoundActorThreadCount(1)
        .setSchedulerName("metrics-test")
        .setMeterRegistry(registry)
        .build();
    scheduler.start();

    final var done = new CountDownLatch(3);
    final Actor actor = new Actor() {
      @Override
      protected void onActorStarted() {
        actor.run(done::countDown);
        actor.run(done::countDown);
        actor.call(() -> {
          done.countDown();
          return null;
        });
      }
    };
    scheduler.submitActor(actor).get(10, TimeUnit.SECONDS);
    assertThat(done.await(10, TimeUnit.SECONDS)).isTrue();
    actor.close();

    // 生命周期
    assertThat(registry.find("scheduler.actor.submitted").tag("scheduler", "metrics-test")
        .counter().count()).isEqualTo(1.0);
    assertThat(registry.find("scheduler.actor.started").counter().count()).isEqualTo(1.0);
    assertThat(registry.find("scheduler.actor.closed").counter().count()).isEqualTo(1.0);

    // job 仅计用户作业（run×2 + call×1）; 生命周期钩子信封不计
    assertThat(registry.find("scheduler.job.submitted").tag("pool", "cpu").counter().count())
        .isEqualTo(3.0);
    assertThat(registry.find("scheduler.job.executed").tag("pool", "cpu").counter().count())
        .isEqualTo(3.0);
    // 队列深度 gauge 已注册
    assertThat(registry.find("scheduler.queue.depth").tag("pool", "cpu").gauge()).isNotNull();
  }

  @Test
  @Timeout(30)
  @DisplayName("拒绝计数: 关闭后提交按 ACTOR_CLOSED 打标")
  void rejectionMetrics() throws Exception {
    scheduler = ActorScheduler.newActorScheduler()
        .setCpuBoundActorThreadCount(1)
        .setSchedulerName("reject-test")
        .setMeterRegistry(registry)
        .build();
    scheduler.start();

    final Actor actor = new Actor() {};
    scheduler.submitActor(actor).get(10, TimeUnit.SECONDS);
    actor.close();
    final var rejected = actor.call(() -> null);

    assertThat(rejected.isCompletedExceptionally()).isTrue();
    assertThat(registry.find("scheduler.job.rejected")
        .tag("pool", "cpu").tag("reason", "ACTOR_CLOSED").counter().count()).isEqualTo(1.0);
  }

  @Test
  @Timeout(30)
  @DisplayName("定时器调度/触发计数")
  void timerMetrics() throws Exception {
    scheduler = ActorScheduler.newActorScheduler()
        .setCpuBoundActorThreadCount(1)
        .setSchedulerName("timer-test")
        .setMeterRegistry(registry)
        .build();
    scheduler.start();

    final var fired = new CountDownLatch(1);
    final var cancelFired = new CopyOnWriteArrayList<String>();
    final Actor actor = new Actor() {
      @Override
      protected void onActorStarted() {
        actor.schedule(java.time.Duration.ofMillis(30), fired::countDown);
        final var toCancel = actor.schedule(java.time.Duration.ofDays(1), () -> cancelFired.add("no"));
        toCancel.cancel();
      }
    };
    scheduler.submitActor(actor).get(10, TimeUnit.SECONDS);
    assertThat(fired.await(10, TimeUnit.SECONDS)).isTrue();

    assertThat(registry.find("scheduler.timer.scheduled").counter().count()).isEqualTo(2.0);
    assertThat(registry.find("scheduler.timer.fired").counter().count()).isEqualTo(1.0);
    assertThat(registry.find("scheduler.timer.cancelled").counter().count()).isEqualTo(1.0);
    assertThat(cancelFired).isEmpty();
  }

  @Test
  @Timeout(30)
  @DisplayName("虚拟线程池: job 按 virtual 池打标")
  void virtualPoolMetrics() throws Exception {
    scheduler = ActorScheduler.newActorScheduler()
        .setSchedulerName("vt-metrics")
        .setMeterRegistry(registry)
        .build();
    scheduler.start();

    final var done = new CountDownLatch(1);
    final Actor actor = new Actor() {
      @Override
      protected void onActorStarted() {
        actor.run(done::countDown);
      }
    };
    scheduler.submitActor(actor, SchedulingHints.VIRTUAL_THREAD).get(10, TimeUnit.SECONDS);
    assertThat(done.await(10, TimeUnit.SECONDS)).isTrue();
    actor.close();

    assertThat(registry.find("scheduler.job.submitted").tag("pool", "virtual").counter().count())
        .isEqualTo(1.0);
    assertThat(registry.find("scheduler.job.executed").tag("pool", "virtual").counter().count())
        .isEqualTo(1.0);
  }
}
