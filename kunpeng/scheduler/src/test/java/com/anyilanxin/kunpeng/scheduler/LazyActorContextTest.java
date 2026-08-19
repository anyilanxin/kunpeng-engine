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
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * 回归：createContext() 必须惰性调用（旧调度器语义）。生产事故：基类构造器调用 createContext()
 * 时子类字段尚未初始化（父类构造器先于子类字段赋值）→ PartitionTransition.createContext 读取
 * this.context 抛 NPE → Partition Transition 步骤失败 → 分区启动中止。
 */
@DisplayName("createContext 惰性初始化")
class LazyActorContextTest {

  private ActorScheduler scheduler;

  @AfterEach
  void tearDown() {
    if (scheduler != null) {
      scheduler.close();
    }
  }

  /** 模拟 PartitionTransition: createContext 读取子类构造器赋值的字段 */
  static class FieldContextActor extends Actor {
    private final String partitionId;

    FieldContextActor(final String partitionId) {
      this.partitionId = partitionId;
    }

    @Override
    protected Map<String, String> createContext() {
      return Map.of("partition-id", partitionId);
    }
  }

  @Test
  @Timeout(30)
  @DisplayName("回归: 子类 createContext 读自身字段, 构造与调度不得抛 NPE")
  void createContextRunsAfterSubclassFieldsInitialized() throws Exception {
    assertThatCode(() -> new FieldContextActor("42"))
        .as("构造期不得调用 createContext（子类字段未初始化）")
        .doesNotThrowAnyException();

    final var constructed = new FieldContextActor("42");
    assertThat(constructed.getContext()).containsEntry("partition-id", "42");

    scheduler = ActorScheduler.newActorScheduler()
        .setCpuBoundActorThreadCount(1).setSchedulerName("lazy-context").build();
    scheduler.start();
    final var done = new java.util.concurrent.CountDownLatch(1);
    final var probe = new FieldContextActor("7") {
      @Override
      protected void onActorStarted() {
        if ("7".equals(getContext().get("partition-id"))) {
          done.countDown();
        }
      }
    };
    scheduler.submitActor(probe).get(10, TimeUnit.SECONDS);
    assertThat(done.await(10, TimeUnit.SECONDS))
        .as("调度后 getContext 应返回子类上下文").isTrue();
    probe.close();
  }
}
