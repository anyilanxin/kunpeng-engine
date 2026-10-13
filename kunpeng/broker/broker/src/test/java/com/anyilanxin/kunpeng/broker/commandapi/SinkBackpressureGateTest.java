/*
 * Copyright © 2026 anyilanxin zxh(anyilanxin@aliyun.com)
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.broker.commandapi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;
import org.junit.jupiter.api.Test;

/**
 * {@link SinkBackpressureGate} 迟滞状态机与无 sink/未绑定语义单测。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
class SinkBackpressureGateTest {
  private static final int PARTITION_ID = 1;
  private static final long REJECT_BACKLOG = 100;
  private static final long RESUME_BACKLOG = 80;

  private final AtomicLong written = new AtomicLong();
  private final AtomicLong lowest = new AtomicLong();

  private SinkBackpressureGate gate(final long reject, final long resume) {
    return new SinkBackpressureGate(PARTITION_ID, written::get, reject, resume, null);
  }

  private SinkBackpressureGate defaultGate() {
    return gate(REJECT_BACKLOG, RESUME_BACKLOG);
  }

  private void lag(final long value) {
    lowest.set(1000);
    written.set(1000 + value);
  }

  @Test
  void unboundSinkPositionSupplierAlwaysAdmits() {
    final SinkBackpressureGate gate = defaultGate();
    lag(REJECT_BACKLOG * 10);
    assertTrue(gate.tryAdmit());
    assertTrue(gate.tryAdmit());
    assertEquals(0, gate.currentLag());
  }

  @Test
  void noSinkConfiguredAlwaysAdmits() {
    final SinkBackpressureGate gate = defaultGate();
    lowest.set(-1);
    lag(0); // written=1000, lowest=-1 由 supplier 返回
    gate.bindSinkPositionSupplier(() -> -1);
    assertTrue(gate.tryAdmit());
    assertEquals(0, gate.currentLag());
  }

  @Test
  void hysteresisTransitionsOnlyAtThresholdCrossings() {
    final SinkBackpressureGate gate = defaultGate();
    gate.bindSinkPositionSupplier(lowest::get);

    // 放行 → 低于拒绝阈值仍放行
    lag(RESUME_BACKLOG);
    assertTrue(gate.tryAdmit());
    lag(REJECT_BACKLOG - 1);
    assertTrue(gate.tryAdmit());

    // 跨过拒绝阈值 → 拒绝
    lag(REJECT_BACKLOG);
    assertFalse(gate.tryAdmit());

    // 迟滞带内维持拒绝（高于恢复阈值、低于拒绝阈值）
    lag(RESUME_BACKLOG + 1);
    assertFalse(gate.tryAdmit());

    // 回落到恢复阈值 → 重开
    lag(RESUME_BACKLOG);
    assertTrue(gate.tryAdmit());

    // 迟滞带内维持放行
    lag(REJECT_BACKLOG - 1);
    assertTrue(gate.tryAdmit());

    // 再次过载 → 再次拒绝（迟滞可重复翻转）
    lag(REJECT_BACKLOG + 5);
    assertFalse(gate.tryAdmit());
  }

  @Test
  void rejectedCounterCountsEveryRejectedCommand() {
    final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    final SinkBackpressureGate gate =
        new SinkBackpressureGate(
            PARTITION_ID, written::get, REJECT_BACKLOG, RESUME_BACKLOG, registry);
    gate.bindSinkPositionSupplier(lowest::get);

    lag(REJECT_BACKLOG);
    gate.tryAdmit();
    gate.tryAdmit();
    gate.tryAdmit();
    final double rejected =
        registry.get("commandapi.backpressure.rejected").counter().count();
    assertEquals(3, rejected, "每条被拒命令计数一次（而非每次转拒绝）");

    // 恢复后放行不再计数
    lag(RESUME_BACKLOG);
    gate.tryAdmit();
    assertEquals(3, registry.get("commandapi.backpressure.rejected").counter().count());
  }

  @Test
  void invalidConfigurationFailsFast() {
    assertThrows(
        IllegalStateException.class, () -> gate(RESUME_BACKLOG, REJECT_BACKLOG));
    assertThrows(IllegalStateException.class, () -> gate(100, 100));
  }

  @Test
  void lowestPositionReadThroughTtlCache() throws InterruptedException {
    final SinkBackpressureGate gate = defaultGate();
    final AtomicInteger reads = new AtomicInteger();
    final LongSupplier countingSink =
        () -> {
          reads.incrementAndGet();
          return lowest.get();
        };
    gate.bindSinkPositionSupplier(countingSink);
    lag(0);

    gate.tryAdmit();
    gate.tryAdmit();
    gate.tryAdmit();
    assertEquals(1, reads.get(), "TTL 窗口内只穿透读一次");

    Thread.sleep(300);
    gate.tryAdmit();
    assertEquals(2, reads.get(), "TTL 过期后重新穿透读");
  }
}
