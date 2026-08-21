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
package io.atomix.raft.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AutoClose;
import org.junit.jupiter.api.Test;

/** 并发压测 {@link RaftRequestMetrics} 的计数接口，确认内部计数器无线程安全问题。 */
public class RaftRequestMetricsTest {

  private static final int CONCURRENT_ROUNDS = 7_500;
  private static final int TYPE_BUCKET = 64;
  private static final int MEMBER_BUCKET = 3;

  @AutoClose private MeterRegistry registry = new SimpleMeterRegistry();
  @AutoClose private ExecutorService pool = Executors.newFixedThreadPool(8);

  private final RaftRequestMetrics victim = new RaftRequestMetrics("9", registry);
  private final AtomicInteger failures = new AtomicInteger();

  @Test
  public void countersSurviveConcurrentAccess() throws Exception {
    // given：每一轮交替产生“收到一条消息”与“发出一条消息”两个任务
    final List<Callable<Void>> jobs = new ArrayList<>(CONCURRENT_ROUNDS * 2);
    for (int round = 1; round <= CONCURRENT_ROUNDS; round++) {
      final int i = round;
      jobs.add(() -> quietReceive(String.valueOf(i % TYPE_BUCKET)));
      jobs.add(() -> quietSend(String.valueOf(i % MEMBER_BUCKET), String.valueOf(i % TYPE_BUCKET)));
    }

    // when：批量提交并等待全部结束
    final List<Future<Void>> pending = pool.invokeAll(jobs);
    for (final Future<Void> job : pending) {
      job.get();
    }

    // then：任何任务都不应抛出异常
    assertThat(failures.get()).isZero();
  }

  /** 吞掉异常并计数，模拟并发竞争下的失败探测。 */
  private Void quietReceive(final String type) {
    try {
      victim.receivedMessage(type);
    } catch (final Exception boom) {
      failures.incrementAndGet();
    }
    return null;
  }

  private Void quietSend(final String to, final String type) {
    try {
      victim.sendMessage(to, type);
    } catch (final Exception boom) {
      failures.incrementAndGet();
    }
    return null;
  }
}
