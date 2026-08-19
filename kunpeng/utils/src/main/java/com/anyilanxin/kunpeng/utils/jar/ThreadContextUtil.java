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

import java.util.concurrent.Callable;
import java.util.function.Supplier;

/** 线程上下文类加载器切换工具：外部 jar 侧载的代码可能经由 {@link Thread#getContextClassLoader()} 取类，执行期间需临时切换为本进程暴露的加载器。 */
public final class ThreadContextUtil {

  private ThreadContextUtil() {}

  /** 以指定加载器执行（不抛受检异常形态；异常原样偷抛） */
  public static void runWithClassLoader(final Runnable runnable, final ClassLoader classLoader) {
    try (final var ignore = swapContextLoader(classLoader)) {
      runnable.run();
    } catch (final Exception e) {
      throw CheckedRunnable.SneakyThrow.rethrow(e);
    }
  }

  /** 以指定加载器执行（受检异常形态；无论成败都恢复原加载器） */
  public static void runCheckedWithClassLoader(
      final CheckedRunnable runnable, final ClassLoader classLoader) throws Exception {
    try (final var ignore = swapContextLoader(classLoader)) {
      runnable.run();
    }
  }

  /** 以指定加载器求值并返回结果 */
  public static <T> T callWithClassLoader(final Callable<T> callable, final ClassLoader classLoader)
      throws Exception {
    try (final var ignore = swapContextLoader(classLoader)) {
      return callable.call();
    }
  }

  /** 以指定加载器求值并返回结果（不抛受检异常形态） */
  public static <T> T getWithClassLoader(
      final Supplier<T> supplier, final ClassLoader classLoader) {
    try (final var ignore = swapContextLoader(classLoader)) {
      return supplier.get();
    }
  }

  private static ContextLoaderSwap swapContextLoader(final ClassLoader classLoader) {
    return new ContextLoaderSwap(classLoader);
  }

  /** TCCL 交换作用域：try-with-resources 保证恢复 */
  private static final class ContextLoaderSwap implements AutoCloseable {

    private final Thread thread;
    private final ClassLoader previous;

    private ContextLoaderSwap(final ClassLoader replacement) {
      this.thread = Thread.currentThread();
      this.previous = thread.getContextClassLoader();
      thread.setContextClassLoader(replacement);
    }

    @Override
    public void close() {
      thread.setContextClassLoader(previous);
    }
  }
}
