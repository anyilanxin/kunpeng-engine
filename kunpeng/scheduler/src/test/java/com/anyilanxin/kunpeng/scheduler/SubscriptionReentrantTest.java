/*
 * Copyright © 2026 anyilanxin zxh (anyilanxin@aliyun.com)
 *
 * This program is free software: you redistribute it and/or modify
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/** 回归：订阅动作内再注册订阅（runOnCompletion 于条件回调中）不抛 CME */
@DisplayName("订阅重入注册")
class SubscriptionReentrantTest {

  private ActorScheduler scheduler;

  @AfterEach
  void tearDown() {
    if (scheduler != null) {
      scheduler.close();
    }
  }

  @Test
  @Timeout(30)
  @DisplayName("条件回调内 runOnCompletion 注册新订阅——不抛 ConcurrentModificationException")
  void reentrantSubscriptionRegistration() throws Exception {
    scheduler = ActorScheduler.newActorScheduler()
        .setCpuBoundActorThreadCount(1).setSchedulerName("reentrant").build();
    scheduler.start();

    final var conditionFired = new CountDownLatch(1);
    final var nestedDone = new CountDownLatch(1);
    final var result = com.anyilanxin.kunpeng.scheduler.future.CompletableActorFuture.<String>completed("ok");

    final Actor actor = new Actor() {
      @Override
      protected void onActorStarted() {
        final var cond = actor.onCondition("cond", () -> {
          conditionFired.countDown();
          // 重入：订阅动作执行中再注册订阅（旧迭代器实现抛 CME → actor FAILED）
          actor.runOnCompletion(result, (v, e) -> nestedDone.countDown());
        });
        cond.signal();
      }
    };
    scheduler.submitActor(actor).get(10, TimeUnit.SECONDS);

    assertThat(conditionFired.await(10, TimeUnit.SECONDS)).isTrue();
    assertThat(nestedDone.await(10, TimeUnit.SECONDS)).isTrue();
    assertThat(actor.isActorClosed()).isFalse();
  }
}
