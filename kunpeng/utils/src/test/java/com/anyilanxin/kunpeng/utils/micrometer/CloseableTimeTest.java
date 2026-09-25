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
package com.anyilanxin.kunpeng.utils.micrometer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

/**
 * 可关闭计时测试。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
final class CloseableTimeTest {

  @Test
  void startAndCloseRecordsDurationToTimer() {
    final var registry = new SimpleMeterRegistry();
    final var timer = Timer.builder("test.timer").register(registry);

    try (final var time = new CloseableTime(timer, registry).start()) {
      // 计时区间
    }

    assertEquals(1, timer.count());
  }

  @Test
  void closeWithoutStartDoesNothing() {
    final var registry = new SimpleMeterRegistry();
    final var timer = Timer.builder("test.timer").register(registry);

    assertDoesNotThrow(() -> new CloseableTime(timer, registry).close());

    assertEquals(0, timer.count());
  }
}
