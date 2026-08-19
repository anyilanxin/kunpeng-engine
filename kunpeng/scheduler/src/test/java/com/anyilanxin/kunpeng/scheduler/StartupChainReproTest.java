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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/** 复刻 DefaultBrokerClusterConfigurationService.start() 的链形态定位启动停摆 */
@DisplayName("启动链形态复现")
class StartupChainReproTest {

  private ActorScheduler scheduler;

  @AfterEach
  void tearDown() {
    if (scheduler != null) {
      scheduler.close();
    }
  }

  /** 模拟 ClusterConfigurationGossiper: executor.run(job) + schedule 定时器 */
  static class FakeGossiper extends Actor {
    final CountDownLatch syncRan = new CountDownLatch(1);

    ActorFuture<Void> start() {
      final ActorFuture<Void> started = actor.createFuture();
      actor.run(() -> {
        // internalStart 含 scheduleSync: 定时器路径一并验证
        actor.schedule(java.time.Duration.ofMillis(20), syncRan::countDown);
        started.complete(null);
      });
      return started;
    }
  }

  /** 模拟 DefaultBrokerClusterConfigurationService: 三连 submitActor + andThen 链 */
  static class FakeConfigService extends Actor {
    private final ActorSchedulingService scheduling;
    final FakeGossiper gossiper = new FakeGossiper();
    final Actor nodeSource = Actor.newActor().name("nodeSource").build();
    final Actor clusterManage = Actor.newActor().name("clusterManage").build();

    FakeConfigService(final ActorSchedulingService scheduling) {
      this.scheduling = scheduling;
    }

    ActorFuture<Void> start() {
      final var f0 = scheduling.submitActor(gossiper);
      final var f1 = f0.thenApply(v -> scheduling.submitActor(nodeSource));
      final var f2 = f1.thenApply(v -> scheduling.submitActor(clusterManage));
      final var f3 = f2.andThen(gossiper::start, Runnable::run);
      final var f4 = f3.andThen(this::gossiperInit, Runnable::run);
      return f4.andThen(() -> CompletableActorFuture.completed(null), Runnable::run);
    }

    ActorFuture<Void> gossiperInit() {
      final ActorFuture<Void> future = actor.createFuture();
      actor.submit(() -> future.complete(null));
      return future;
    }
  }

  @Test
  @Timeout(30)
  @DisplayName("step-1 链: submitActor×3 + thenApply×2 + andThen×3 全链完成")
  void brokerConfigServiceChainCompletes() throws Exception {
    scheduler = ActorScheduler.newActorScheduler()
        .setCpuBoundActorThreadCount(2).setSchedulerName("chain-repro").build();
    scheduler.start();

    final var configService = new FakeConfigService(scheduler);
    final var stepDone = new CountDownLatch(1);
    // 生产形态: 先 submitActor(configService) 再调 start()
    scheduler.submitActor(configService).get(10, TimeUnit.SECONDS);

    // BrokerStartupProcess 形态: actor.run(() -> runOnCompletion(start(), result))
    final Actor broker = new Actor() {};
    scheduler.submitActor(broker).get(10, TimeUnit.SECONDS);
    broker.run(() -> broker.runOnCompletion(configService.start(), (v, e) -> stepDone.countDown()));

    assertThat(stepDone.await(15, TimeUnit.SECONDS))
        .as("step-1 链应在 15s 内完成（复现 broker 启动停摆）").isTrue();
    // 定时器路径: scheduleSync 20ms 后触发
    assertThat(configService.gossiper.syncRan.await(10, TimeUnit.SECONDS))
        .as("schedule 定时器应触发").isTrue();
  }
}
