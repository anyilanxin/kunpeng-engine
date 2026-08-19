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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * 复刻 ReplicaPullTransfer.closeAsync 形态: 依赖 future 的续接经自身 actor 排队, 且该 actor 在
 * closeAsync 内自关。修复前: closeFuture 完成于 actor 终态后, andThen 结果经已关闭 actor 路由被静默丢弃
 * → closeAsync future 永不完成（缩容离席卡在 Shutdown Snapshot Store 的根因 11）。
 */
@DisplayName("andThen 自关闭形态完成不丢失")
class AndThenSelfCloseHangTest {

  private ActorScheduler scheduler;

  @AfterEach
  void tearDown() {
    if (scheduler != null) {
      scheduler.close();
    }
  }

  /** 与 ReplicaPullTransfer 同构: 依赖 actor 关闭后, andThen(自身 executor) 触发 super.closeAsync() */
  static final class SelfClosingActor extends Actor {
    private final Actor dependency = Actor.wrap(control -> {});

    @Override
    public String getName() {
      return "self-closing";
    }

    @Override
    public ActorFuture<Void> closeAsync() {
      return dependency.closeAsync().andThen(ignored -> super.closeAsync(), actor);
    }
  }

  @Test
  @Timeout(30)
  @DisplayName("依赖 actor 关闭 → 自身 andThen 关闭, closeAsync future 必须完成")
  void selfCloseViaAndThenCompletes() throws Exception {
    scheduler =
        ActorScheduler.newActorScheduler()
            .setCpuBoundActorThreadCount(2)
            .setSchedulerName("andthen-selfclose")
            .build();
    scheduler.start();

    final var pull = new SelfClosingActor();
    // 依赖 actor 须已提交调度, 其 closeAsync 才能真正走关闭相位
    final var dependency = pull.dependency;
    scheduler.submitActor(dependency).get(10, TimeUnit.SECONDS);
    scheduler.submitActor(pull).get(10, TimeUnit.SECONDS);

    final var closed = new CountDownLatch(1);
    pull.closeAsync().onComplete((v, e) -> closed.countDown());

    assertThat(closed.await(15, TimeUnit.SECONDS))
        .as("closeAsync 必须完成（修复前: 结果完成经已终态 actor 路由被丢弃, 永挂）")
        .isTrue();
  }
}
