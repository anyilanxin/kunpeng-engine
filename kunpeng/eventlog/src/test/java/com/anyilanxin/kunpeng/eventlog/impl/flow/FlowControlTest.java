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

import com.anyilanxin.kunpeng.eventlog.AppendResult;
import com.anyilanxin.kunpeng.eventlog.AppendResult.RejectionReason;
import com.anyilanxin.kunpeng.eventlog.FlowControlParams;
import com.anyilanxin.kunpeng.eventlog.InMemoryEventStore;
import com.anyilanxin.kunpeng.eventlog.WriteContext;
import com.anyilanxin.kunpeng.eventlog.impl.EventLogMetrics;
import com.anyilanxin.kunpeng.eventlog.impl.sequencer.PositionSequencer;
import java.time.Clock;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 自研流控测试：AIMD 升降 / 三态水位转移 / 拒绝语义 / 失败与准入归还 / 槽位回绕驱逐。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@DisplayName("FlowController AIMD 流控")
class FlowControlTest {

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
    final FlowController controller =
        new FlowController(FlowControlParams.defaults(), clock::get, EventLogMetrics.noop());

    assertThat(controller.tryAcquire(WriteContext.USER_COMMAND)).isNull();
    controller.onAppend(1, 3, 3, true);
    controller.onWrite(1, 3);
    controller.onCommit(1, 3);
    assertThat(controller.lastWrittenPosition()).isEqualTo(3);
    assertThat(controller.lastCommittedPosition()).isEqualTo(3);
    clock.addAndGet(500_000L);
    controller.onProcessed(3);
    assertThat(controller.lastProcessedPosition()).isEqualTo(3);

    // 窗口占位已释放 → 可再次获取
    assertThat(controller.tryAcquire(WriteContext.USER_COMMAND)).isNull();
    controller.onAppend(4, 4, 1, true);
    controller.onWrite(4, 4);
    controller.onCommit(4, 4);
    controller.onProcessed(4);
  }

  @Test
  @DisplayName("UserCommand 每批一占位占满即拒; Internal 永不拒绝不占位")
  void rejectionSemantics() {
    final FlowController controller =
        new FlowController(
            new FlowControlParams(2, 1, 2, 0.1), System::nanoTime, EventLogMetrics.noop());

    assertThat(controller.tryAcquire(WriteContext.USER_COMMAND)).isNull();
    assertThat(controller.tryAcquire(WriteContext.USER_COMMAND)).isNull();
    assertThat(controller.windowInflight()).isEqualTo(2);
    // 窗口=2 已占满
    assertThat(controller.tryAcquire(WriteContext.USER_COMMAND))
        .isEqualTo(RejectionReason.REQUEST_WINDOW_EXHAUSTED);
    // 非 USER_COMMAND 永不受限、不占窗口
    assertThat(controller.tryAcquire(WriteContext.INTERNAL)).isNull();
    assertThat(controller.tryAcquire(WriteContext.PROCESSING_RESULT)).isNull();
    assertThat(controller.windowInflight()).isEqualTo(2);
    assertThat(controller.canAcquire(WriteContext.INTERNAL)).isTrue();
    assertThat(controller.canAcquire(WriteContext.USER_COMMAND)).isFalse();
  }

  @Test
  @DisplayName("onFailure 烧毁占位（窗口可复用）")
  void failureRelease() {
    final FlowController controller =
        new FlowController(
            new FlowControlParams(1, 1, 2, 0.1), System::nanoTime, EventLogMetrics.noop());

    assertThat(controller.tryAcquire(WriteContext.USER_COMMAND)).isNull();
    assertThat(controller.tryAcquire(WriteContext.USER_COMMAND))
        .isEqualTo(RejectionReason.REQUEST_WINDOW_EXHAUSTED);
    controller.onAppend(1, 2, 2, true);
    controller.onFailure(2, new RuntimeException("raft 拒绝"));
    // 占位已烧毁释放
    assertThat(controller.windowInflight()).isZero();
    assertThat(controller.tryAcquire(WriteContext.USER_COMMAND)).isNull();
  }

  @Test
  @DisplayName("abandonAdmission: 准入放行后未入日志的归还路径")
  void abandonAdmission() {
    final FlowController controller =
        new FlowController(
            new FlowControlParams(1, 1, 2, 0.1), System::nanoTime, EventLogMetrics.noop());

    assertThat(controller.tryAcquire(WriteContext.USER_COMMAND)).isNull();
    controller.abandonAdmission(WriteContext.USER_COMMAND);
    // 临界区内拒绝后占位归还, 不触在途环
    assertThat(controller.windowInflight()).isZero();
    assertThat(controller.tryAcquire(WriteContext.USER_COMMAND)).isNull();
  }

  @Test
  @DisplayName("回归: 消费方未释放时槽位回绕驱逐兜底——同槽覆盖并归还被驱逐者占位（线上事故场景）")
  void slotWraparoundEvictsAndReleasesDisplaced() {
    final FlowController controller =
        new FlowController(
            new FlowControlParams(10, 1, 10, 0.1), System::nanoTime, EventLogMetrics.noop());

    // 模拟线上: 注册后无人调 onProcessed（滞留占位）
    assertThat(controller.tryAcquire(WriteContext.USER_COMMAND)).isNull();
    controller.onAppend(1, 1, 1, true);
    assertThat(controller.windowInflight()).isEqualTo(1);

    // 同槽位（1024 后回绕）覆盖驱逐而非失败, 被驱逐者占位被归还
    assertThat(controller.tryAcquire(WriteContext.USER_COMMAND)).isNull();
    controller.onAppend(1025, 1025, 1, true);
    assertThat(controller.windowInflight()).isEqualTo(1);

    // 关闭全量释放: 环内滞留批（1025）占位归还
    controller.releaseAll();
    assertThat(controller.windowInflight()).isZero();
  }

  @Test
  @DisplayName("释放游标: 跨过未登记的内部事件, 迟到的失败释放与重复的处理水位都不重复归还占位")
  void releaseCursorDoesNotDoubleRelease() {
    final FlowController controller =
        new FlowController(
            new FlowControlParams(10, 1, 10, 0.1), System::nanoTime, EventLogMetrics.noop());

    // 在途批 1、2、5；3、4 是内部事件（不登记在途环）
    for (final long position : new long[] {1, 2, 5}) {
      assertThat(controller.tryAcquire(WriteContext.USER_COMMAND)).isNull();
      controller.onAppend(position, position, 1, true);
    }
    assertThat(controller.windowInflight()).isEqualTo(3);

    // 处理到 2：只释放 1、2
    controller.onProcessed(2);
    assertThat(controller.windowInflight()).isEqualTo(1);

    // 迟到的失败释放（position 1 已释放）与重复到达的处理水位都不再归还
    controller.onFailure(1, new RuntimeException("late failure"));
    controller.onProcessed(2);
    assertThat(controller.windowInflight()).isEqualTo(1);

    controller.onProcessed(100);
    assertThat(controller.windowInflight()).isZero();
  }

  @Test
  @DisplayName("存储失败拒绝只经 onFailure 归还一次准入，不重复扣减在途计数")
  void storageFailureReleasesAdmissionExactlyOnce() {
    final FlowController controller =
        new FlowController(
            new FlowControlParams(1, 1, 2, 0.1), System::nanoTime, EventLogMetrics.noop());
    final InMemoryEventStore store = new InMemoryEventStore();
    store.failNextAppends(1);
    final var sequencer =
        new PositionSequencer(
            store, controller, EventLogMetrics.noop(), 1024, 1, Clock.systemUTC());

    final var result =
        sequencer.tryAppend(
            WriteContext.USER_COMMAND, List.of(com.anyilanxin.kunpeng.eventlog.TestEntries.simple()));

    assertThat(result).isInstanceOf(AppendResult.Rejected.class);
    assertThat(((AppendResult.Rejected) result).reason())
        .isEqualTo(RejectionReason.STORAGE_FAILURE);
    assertThat(controller.windowInflight()).isZero();
    sequencer.close();
    store.shutdown();
  }
}
