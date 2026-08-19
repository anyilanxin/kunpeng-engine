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
package com.anyilanxin.kunpeng.utils.jar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/** {@link CheckedRunnable} 单元测试 */
class CheckedRunnableTest {

  @Test
  void toCallableRunsAndReturnsNull() throws Exception {
    final AtomicInteger executions = new AtomicInteger();
    final Callable<Void> callable = CheckedRunnable.toCallable(executions::incrementAndGet);

    assertNull(callable.call());
    assertEquals(1, executions.get());
  }

  @Test
  void toCallablePropagatesCheckedException() {
    final IOException error = new IOException("boom");
    final Callable<Void> callable = CheckedRunnable.toCallable(() -> {
      throw error;
    });

    assertSame(error, assertThrows(IOException.class, callable::call));
  }

  @Test
  void toUncheckedRunsNormally() {
    final AtomicInteger executions = new AtomicInteger();
    final Runnable runnable = CheckedRunnable.toUnchecked(executions::incrementAndGet);

    runnable.run();

    assertEquals(1, executions.get());
  }

  @Test
  void toUncheckedSneakyThrowsOriginalException() {
    final IOException error = new IOException("boom");
    final Runnable runnable = CheckedRunnable.toUnchecked(() -> {
      throw error;
    });

    // 偷抛语义：异常实例原样抛出，类型仍为 IOException 而非 RuntimeException 包装
    assertSame(error, assertThrows(IOException.class, runnable::run));
  }
}
