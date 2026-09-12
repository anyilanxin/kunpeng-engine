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
package com.anyilanxin.kunpeng.utils.error;

import java.lang.Thread.UncaughtExceptionHandler;
import org.slf4j.Logger;

/**
 * 处理所有 Throwable，并在遇到 {@link VirtualMachineError} 时退出 JVM。它也可作为 {@link UncaughtExceptionHandler
 * 未捕获异常处理器}使用，例如用作 {@link Thread#setDefaultUncaughtExceptionHandler 默认未捕获异常处理器}。
 */
public final class VirtualMachineErrorHandler
    implements FatalErrorHandler, UncaughtExceptionHandler {
  private static final int EXIT_CODE = 156; // ASCII 码 Z + B
  private final Logger log;

  VirtualMachineErrorHandler(final Logger log) {
    this.log = log;
  }

  /**
   * 处理任意 {@link Throwable}，若为不可恢复的错误（即 {@link VirtualMachineError}）则彻底终止 JVM。 在捕获 {@link Throwable}
   * 后、执行常规错误处理之前应调用本方法。
   *
   * <p>{@link VirtualMachineError} 的常见例子包括 {@link OutOfMemoryError}、{@link StackOverflowError} 和
   * {@link InternalError}。我们认为这些错误不可恢复，因为无法采取任何措施来解决它们， 终止进程交由部署环境重启更安全。
   *
   * @param e 待处理的 throwable
   */
  @Override
  public void handleError(final Throwable e) {
    if (e instanceof VirtualMachineError) {
      tryLogging(e);
      System.exit(EXIT_CODE);
    }
  }

  private void tryLogging(final Throwable e) {
    try {
      if (e instanceof OutOfMemoryError) {
        log.error("内存不足，因无法从 OOM 中恢复，现在退出。请考虑调整内存限制。", e);
      } else {
        log.error("因无法从 JVM 错误中恢复，正在关闭。若属临时性问题，请考虑重启该进程。", e);
      }
    } catch (final Throwable loggingError) {
      loggingError.printStackTrace();
      // 已忽略！我们已尝试记录有用的错误信息，但失败了，此时已无能为力。
    }
  }

  @Override
  public void uncaughtException(final Thread t, final Throwable e) {
    handleError(e);
  }
}
