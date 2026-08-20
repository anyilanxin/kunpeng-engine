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
package com.anyilanxin.kunpeng.cluster.test;

import java.time.Duration;

/** 测试等待工具 */
public final class TestUtil {

  private TestUtil() {}

  /** 等待条件满足（默认 10 秒超时） */
  public static void waitUntil(final BooleanSupplier condition) {
    waitUntil(condition, Duration.ofSeconds(10));
  }

  public static void waitUntil(final BooleanSupplier condition, final Duration timeout) {
    final long deadline = System.nanoTime() + timeout.toNanos();
    while (!condition.getAsBoolean()) {
      if (System.nanoTime() > deadline) {
        throw new AssertionError("等待超时: " + timeout);
      }
      try {
        Thread.sleep(10);
      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new AssertionError("等待被中断", e);
      }
    }
  }

  @FunctionalInterface
  public interface BooleanSupplier {
    boolean getAsBoolean();
  }
}
