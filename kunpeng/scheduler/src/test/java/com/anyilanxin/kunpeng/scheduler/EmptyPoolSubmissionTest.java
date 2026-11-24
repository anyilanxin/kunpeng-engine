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
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * 线程数配成 0 的池可以存在（如网关不设 IO 池），但不能承接对应类型的 actor：此前路由会数组越界，cell 卡在 WAKING_UP。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@DisplayName("空池提交")
class EmptyPoolSubmissionTest {

  @Test
  @Timeout(30)
  @DisplayName("IO 线程数为 0 时提交 IO_BOUND actor 得到明确的配置错误，CPU_BOUND 不受影响")
  void submittingToPoolWithoutRunnersFailsWithClearMessage() throws Exception {
    final ActorScheduler scheduler =
        ActorScheduler.newActorScheduler()
            .setCpuBoundActorThreadCount(1)
            .setIoBoundActorThreadCount(0)
            .setSchedulerName("empty-pool")
            .build();
    scheduler.start();
    try {
      final IllegalStateException error =
          assertThrows(
              IllegalStateException.class,
              () -> scheduler.submitActor(named("io"), SchedulingHints.IO_BOUND));
      assertThat(error)
          .hasMessageContaining("empty-pool-io-scheduler")
          .hasMessageContaining("no runner threads");

      scheduler.submitActor(named("cpu"), SchedulingHints.CPU_BOUND).get(10, TimeUnit.SECONDS);
    } finally {
      scheduler.close();
    }
  }

  private static Actor named(final String name) {
    return new Actor() {
      @Override
      public String getName() {
        return name;
      }
    };
  }
}
