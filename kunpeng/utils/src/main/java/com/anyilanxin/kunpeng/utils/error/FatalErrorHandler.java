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
 * FatalErrorHandler 用于安全、一致地处理所有 {@link Throwable}。实现类解析 throwable， 并在其被认为是致命错误时采取<i>某种</i>措施。
 *
 * @see VirtualMachineErrorHandler
 */
public interface FatalErrorHandler {
  /**
   * 处理任意 {@link Throwable}。在捕获 {@link Throwable} 后、执行常规错误处理之前应调用本方法。
   *
   * <p>{@link VirtualMachineErrorHandler} 会在遇到所有 {@link VirtualMachineError} 时退出。
   *
   * @param e 待处理的 throwable
   */
  void handleError(Throwable e);

  /** 构建一个可用作默认未捕获异常处理器的 {@link FatalErrorHandler}。 */
  static UncaughtExceptionHandler uncaughtExceptionHandler(final Logger logger) {
    return new VirtualMachineErrorHandler(logger);
  }

  /** 构建默认的 {@link FatalErrorHandler}。 */
  static FatalErrorHandler withLogger(final Logger logger) {
    return new VirtualMachineErrorHandler(logger);
  }
}
