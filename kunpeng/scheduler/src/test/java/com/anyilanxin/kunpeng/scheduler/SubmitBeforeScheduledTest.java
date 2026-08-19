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
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * 回归：submitActor 之前的早期提交是合法形态（旧调度器语义）。生产场景：集群拓扑事件在
 * ClusterConfigurationStep 之前就把 update 回调打到 gossiper actor——job 必须在首次调度时
 * 被捞起执行，而不是被拒绝或永久挂起。
 */
@DisplayName("submitActor 之前的早期提交")
class SubmitBeforeScheduledTest {

  private ActorScheduler scheduler;

  @AfterEach
  void tearDown() {
    if (scheduler != null) {
      scheduler.close();
    }
  }

  @Test
  @Timeout(30)
  @DisplayName("回归: 早期提交的 job 在 submitActor 后被捞起执行")
  void earlyJobExecutesOnceScheduled() throws Exception {
    scheduler = ActorScheduler.newActorScheduler()
        .setCpuBoundActorThreadCount(1).setSchedulerName("early-submit").build();
    scheduler.start();

    final Actor actor = Actor.newActor().name("late-submitted").build();
    final ActorFuture<String> early = actor.call(() -> "early");

    assertThat(early.isDone()).as("submitActor 前仅排队").isFalse();

    scheduler.submitActor(actor).get(10, TimeUnit.SECONDS);
    assertThat(early.get(10, TimeUnit.SECONDS)).as("首次调度后必须执行").isEqualTo("early");

    final ActorFuture<String> later = actor.call(() -> "later");
    assertThat(later.get(10, TimeUnit.SECONDS)).isEqualTo("later");
    actor.close();
  }

  @Test
  @Timeout(30)
  @DisplayName("回归: 从未提交的 actor close 时, 早期排队 job 的 future 被释放而非挂死")
  void neverSubmittedCloseReleasesQueuedJobs() throws Exception {
    final Actor actor = Actor.newActor().name("never-submitted").build();
    final ActorFuture<String> early = actor.call(() -> "never");

    actor.closeAsync().get(10, TimeUnit.SECONDS);

    assertThat(early.isCompletedExceptionally()).as("排队 job 必须异常完成").isTrue();
    assertThat(early.getException())
        .hasMessageContaining("Actor is closed");
  }
}
