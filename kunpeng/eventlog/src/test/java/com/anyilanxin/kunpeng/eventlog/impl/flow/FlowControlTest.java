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
package com.anyilanxin.kunpeng.eventlog.impl.flow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.anyilanxin.kunpeng.eventlog.AppendResult.RejectionReason;
import com.anyilanxin.kunpeng.eventlog.impl.EventLogMetrics;
import com.anyilanxin.kunpeng.eventlog.FlowControlParams;
import com.anyilanxin.kunpeng.eventlog.WriteContext;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** 自研流控测试：令牌桶速率数学 / AIMD 升降 / 三态水位转移 / 拒绝语义 / 失败释放 */
@DisplayName("FlowController 自研流控")
class FlowControlTest {

  // ===== 令牌桶（虚拟时钟） =====

  @Test
  @DisplayName("令牌桶: 速率补充 + burst 上限 + 不超发")
  void tokenBucket() {
    final AtomicLong clock = new AtomicLong(0);
    final TokenBucket bucket = new TokenBucket(100, 100, clock::get); // 100/s, burst 100

    assertThat(bucket.tryAcquire(100)).isTrue(); // 突发容量一次耗尽
    assertThat(bucket.tryAcquire(1)).isFalse();

    clock.addAndGet(1_000_000_000L); // +1s → 补 100
    assertThat(bucket.tryAcquire(100)).isTrue();
    assertThat(bucket.tryAcquire(1)).isFalse();

    clock.addAndGet(10_000_000_000L); // +10s → 补 1000, 但 clamp 到 burst
    assertThat(bucket.tryAcquire(100)).isTrue();
    assertThat(bucket.tryAcquire(1)).isFalse(); // 上限 100
  }

  // ===== AIMD 窗口 =====

  @Test
  @DisplayName("AIMD: RTT 稳定线性增, 恶化乘性减, clamp 生效")
  void aimdWindow() {
    final AimdWindow window = new AimdWindow(50, 10, 60, 0.1);
    // 100 次稳定 RTT 成功 → 窗口 +1（节流周期）
    for (int i = 0; i < 100; i++) {
      window.onSuccess(1_000_000L);
    }
    assertThat(window.window()).isEqualTo(51);

    // EMA 抬升（RTT 恶化 10x 持续采样）→ 下个节流点乘性减, 不低于 min
    for (int i = 0; i < 100; i++) {
      window.onSuccess(10_000_000L);
    }
    assertThat(window.window()).isLessThan(51).isGreaterThanOrEqualTo(10);

    final AimdWindow minClamp = new AimdWindow(10, 10, 60, 0.0);
    for (int round = 0; round < 5; round++) {
      for (int i = 0; i < 100; i++) {
        minClamp.onSuccess(100_000_000L);
      }
    }
    assertThat(minClamp.window()).isEqualTo(10); // 降到下限后不再降
  }

  // ===== FlowController 集成 =====

  @Test
  @DisplayName("三态水位: append→write→commit→processed 全程推进")
  void lifecycle() {
    final AtomicLong clock = new AtomicLong(1_000_000L);
    final FlowController controller = new FlowController(FlowControlParams.defaults(),
        clock::get, EventLogMetrics.noop());

    assertThat(controller.tryAcquire(WriteContext.USER_COMMAND, 3)).isNull();
    controller.onAppend(1, 3, 3);
    controller.onWrite(1, 3);
    controller.onCommit(1, 3);
    assertThat(controller.lastWrittenPosition()).isEqualTo(3);
    assertThat(controller.lastCommittedPosition()).isEqualTo(3);
    clock.addAndGet(500_000L);
    controller.onProcessed(3);
    assertThat(controller.lastProcessedPosition()).isEqualTo(3);

    // 窗口占位已释放 → 可再次获取
    assertThat(controller.tryAcquire(WriteContext.USER_COMMAND, 1)).isNull();
    controller.onAppend(4, 4, 1);
    controller.onWrite(4, 4);
    controller.onCommit(4, 4);
    controller.onProcessed(4);
  }

  @Test
  @DisplayName("UserCommand 窗口拒绝; Internal 永不拒绝")
  void rejectionSemantics() {
    final FlowController controller = new FlowController(
        new FlowControlParams(2, 1, 2, 0.1, -1, 0, 10, 100, Duration.ofSeconds(10)),
        System::nanoTime, EventLogMetrics.noop());

    assertThat(controller.tryAcquire(WriteContext.USER_COMMAND, 1)).isNull();
    assertThat(controller.tryAcquire(WriteContext.USER_COMMAND, 1)).isNull();
    // 窗口=2 已占满
    assertThat(controller.tryAcquire(WriteContext.USER_COMMAND, 1))
        .isEqualTo(RejectionReason.REQUEST_WINDOW_EXHAUSTED);
    // 非 USER_COMMAND 永不受限
    assertThat(controller.tryAcquire(WriteContext.INTERNAL, 100)).isNull();
    assertThat(controller.tryAcquire(WriteContext.PROCESSING_RESULT, 100)).isNull();
    assertThat(controller.canAcquire(WriteContext.INTERNAL, 100)).isTrue();
    assertThat(controller.canAcquire(WriteContext.USER_COMMAND, 1)).isFalse();
  }

  @Test
  @DisplayName("写入速率拒绝（虚拟时钟）")
  void rateRejection() {
    final AtomicLong clock = new AtomicLong(0);
    final FlowController controller = new FlowController(
        new FlowControlParams(100, 10, 1000, 0.1, 10, 10, 1, 100, Duration.ofSeconds(10)),
        clock::get, EventLogMetrics.noop());
    assertThat(controller.tryAcquire(WriteContext.USER_COMMAND, 10)).isNull(); // 桶 10 耗尽
    assertThat(controller.tryAcquire(WriteContext.USER_COMMAND, 1))
        .isEqualTo(RejectionReason.WRITE_RATE_EXHAUSTED);
    clock.addAndGet(1_000_000_000L);
    assertThat(controller.tryAcquire(WriteContext.USER_COMMAND, 10)).isNull();
  }

  @Test
  @DisplayName("onFailure 释放占位（窗口可复用）")
  void failureRelease() {
    final FlowController controller = new FlowController(
        new FlowControlParams(1, 1, 1, 0.1, -1, 0, 10, 100, Duration.ofSeconds(10)),
        System::nanoTime, EventLogMetrics.noop());

    assertThat(controller.tryAcquire(WriteContext.USER_COMMAND, 2)).isNull();
    controller.onAppend(1, 2, 2);
    controller.onFailure(2, new RuntimeException("raft 拒绝"));
    // 占位已释放
    assertThat(controller.tryAcquire(WriteContext.USER_COMMAND, 1)).isNull();
  }

  @Test
  @DisplayName("onAppendRolledBack: 定序后回滚烧毁")
  void rolledBack() {
    final FlowController controller = new FlowController(
        new FlowControlParams(1, 1, 1, 0.1, -1, 0, 10, 100, Duration.ofSeconds(10)),
        System::nanoTime, EventLogMetrics.noop());
    assertThat(controller.tryAcquire(WriteContext.USER_COMMAND, 5)).isNull();
    controller.onAppend(1, 5, 5);
    controller.onAppendRolledBack(1, 5);
    assertThat(controller.tryAcquire(WriteContext.USER_COMMAND, 1)).isNull();
    assertThat(controller.lastCommittedPosition()).isZero();
  }
  @Test
  @DisplayName("回归: 消费方未释放时陈旧驱逐兜底——跨 1024 回绕不崩溃（线上事故场景）")
  void staleEvictionPreventsRingCollision() {
    final AtomicLong clock = new AtomicLong(0);
    final FlowController controller = new FlowController(FlowControlParams.defaults(),
        clock::get, EventLogMetrics.noop());

    // 模拟线上: 注册后无人调 onProcessed（旧实现为无界表静默泄漏）
    assertThat(controller.tryAcquire(WriteContext.USER_COMMAND, 1)).isNull();
    controller.onAppend(1, 1, 1);
    // 同槽位（1024 后回绕）且未超期 → 仍应显式冲突（真完整性违规）
    assertThatThrownBy(() -> controller.onAppend(1025, 1025, 1))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("复用冲突");

    // 超过 60s 陈旧阈值 → 驱逐兜底而非崩溃, 且窗口占位被释放
    clock.addAndGet(61_000_000_000L);
    controller.onAppend(1025, 1025, 1);
    assertThat(controller.lastWrittenPosition()).isZero();

    // 驱逐后窗口可用（释放了滞留占位）
    assertThat(controller.tryAcquire(WriteContext.USER_COMMAND, 1)).isNull();
  }
}
