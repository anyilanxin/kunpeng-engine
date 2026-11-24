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

import com.anyilanxin.kunpeng.eventlog.FlowControlParams;
import com.anyilanxin.kunpeng.eventlog.WriteContext;
import com.anyilanxin.kunpeng.eventlog.impl.EventLogMetrics;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 窗口增窗的上界：窗口已在上限时再加一不能溢出成负数，否则所有命令都会被当作“在途窗口耗尽”拒掉。
 *
 * <p>broker 关闭窗口时的取值就是上限 MAX_VALUE（初始、下限、上限同为 MAX_VALUE，容差 0.1），而窗口逻辑照样生效。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@DisplayName("AIMD 窗口增窗上界")
class AimdWindowOverflowTest {

  @Test
  @DisplayName("窗口已在上限时稳定 RTT 不会把它加溢出")
  void windowAtItsLimitDoesNotOverflow() {
    final var window = new AimdWindow(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, 0.1);
    for (int i = 0; i < 300; i++) {
      window.onSuccess(2_000_000L);
    }
    assertThat(window.window()).isEqualTo(Integer.MAX_VALUE);
    assertThat(window.tryAcquire()).as("增窗后仍接纳命令").isTrue();
  }

  @Test
  @DisplayName("窗口关闭（上限 MAX_VALUE）时处理满 100 条稳定 RTT 的命令后仍继续接纳命令")
  void commandsKeepBeingAdmittedWithTheWindowSwitchedOff() {
    // 可控的单调时钟：每条命令从追加到处理恒为 2ms，RTT 梯度恒为 1
    final var nanoTime = new AtomicLong(1_000_000_000L);
    final var controller =
        new FlowController(
            new FlowControlParams(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, 0.1),
            nanoTime::get,
            EventLogMetrics.noop());
    for (int position = 1; position <= 300; position++) {
      assertThat(controller.tryAcquire(WriteContext.USER_COMMAND))
          .as("第 " + position + " 条命令应被接纳")
          .isNull();
      controller.onAppend(position, position, 1, true);
      controller.onWrite(position, position);
      controller.onCommit(position, position);
      nanoTime.addAndGet(2_000_000L);
      controller.onProcessed(position);
    }
    assertThat(controller.canAcquire(WriteContext.USER_COMMAND)).isTrue();
  }
}
