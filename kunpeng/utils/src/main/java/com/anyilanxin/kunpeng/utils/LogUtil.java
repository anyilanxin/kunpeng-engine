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
package com.anyilanxin.kunpeng.utils;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.MDC;

/** 日志工具类，提供 MDC 上下文包装与异常捕获日志等静态方法。 */
public final class LogUtil {
  private LogUtil() {}

  /**
   * 在给定的 MDC 上下文中执行 runnable，执行完毕后恢复原有的 MDC 上下文。
   *
   * <p>参见 https://logback.qos.ch/manual/mdc.html
   */
  public static void doWithMDC(final Map<String, String> context, final Runnable r) {
    final Map<String, String> currentContext = MDC.getCopyOfContextMap();
    MDC.setContextMap(context);
    try {
      r.run();
    } finally {
      if (currentContext != null) {
        MDC.setContextMap(currentContext);
      } else {
        MDC.clear();
      }
    }
  }

  /** 执行 runnable，若抛出异常则记录错误日志。 */
  public static void catchAndLog(final Logger log, final CheckedRunnable r) {
    try {
      r.run();
    } catch (final Exception e) {
      log.error("执行任务时发生异常", e);
    }
  }
}
