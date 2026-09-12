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
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * 回归：runOnCompletion(空集合) 必须立即回调（旧调度器语义）。生产事故：全新数据目录下
 * FileBasedSnapshotStoreImpl.abortPendingSnapshots 的 inProgress 为空 → runOnCompletion([])
 * 回调永不触发 → future 永不完成 → Raft 线程在角色切换 stop() 的 join() 永久阻塞 → 分区
 * bootstrap 卡死、集群零分区。
 */
@DisplayName("空集合的 runOnCompletion")
class RunOnCompletionEmptyCollectionTest {

  private ActorScheduler scheduler;

  @AfterEach
  void tearDown() {
    if (scheduler != null) {
      scheduler.close();
    }
  }

  @Test
  @Timeout(30)
  @DisplayName("回归: 空集合立即以 null 回调（abortPendingSnapshots 死锁场景）")
  void emptyCollectionFiresCallbackImmediately() throws Exception {
    scheduler = ActorScheduler.newActorScheduler()
        .setCpuBoundActorThreadCount(1).setSchedulerName("empty-collection").build();
    scheduler.start();

    final var done = new CountDownLatch(1);
    final var errors = new CopyOnWriteArrayList<Throwable>();
    final Actor actor = new Actor() {
      @Override
      protected void onActorStarted() {
        // 模拟 FileBasedSnapshotStoreImpl.abortPendingSnapshots 的空 aborts 形态
        final var aborts = List.<com.anyilanxin.kunpeng.scheduler.future.ActorFuture<Void>>of();
        actor.runOnCompletion(
            aborts,
            err -> {
              if (err != null) {
                errors.add(err);
              }
              done.countDown();
            });
      }
    };
    scheduler.submitActor(actor).get(10, TimeUnit.SECONDS);

    assertThat(done.await(5, TimeUnit.SECONDS))
        .as("空集合必须立即触发回调, 否则上游 join 永久挂起").isTrue();
    assertThat(errors).isEmpty();
    actor.close();
  }

  @Test
  @Timeout(30)
  @DisplayName("对照: 非空集合（含已完成）路径不受影响")
  void nonEmptyCollectionStillWorks() throws Exception {
    scheduler = ActorScheduler.newActorScheduler()
        .setCpuBoundActorThreadCount(1).setSchedulerName("non-empty").build();
    scheduler.start();

    final var done = new CountDownLatch(1);
    final Actor actor = new Actor() {
      @Override
      protected void onActorStarted() {
        actor.runOnCompletion(
            List.of(CompletableActorFuture.<Void>completed(null)),
            err -> done.countDown());
      }
    };
    scheduler.submitActor(actor).get(10, TimeUnit.SECONDS);

    assertThat(done.await(5, TimeUnit.SECONDS)).isTrue();
    actor.close();
  }
}
