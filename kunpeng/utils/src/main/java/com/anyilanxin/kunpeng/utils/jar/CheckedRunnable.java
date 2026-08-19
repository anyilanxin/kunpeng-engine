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

/** 可抛受检异常的 Runnable */
@FunctionalInterface
@SuppressWarnings("java:S112")
public interface CheckedRunnable {

  void run() throws Exception;

  /** 包装为不抛受检异常的 Runnable（异常原样偷抛，不包装） */
  static Runnable toUnchecked(final CheckedRunnable runnable) {
    return () -> {
      try {
        runnable.run();
      } catch (final Exception e) {
        throw SneakyThrow.rethrow(e);
      }
    };
  }

  /** 包装为 Callable */
  static Callable<Void> toCallable(final CheckedRunnable runnable) {
    return () -> {
      runnable.run();
      return null;
    };
  }

  /** 泛型欺骗式偷抛：保留原始受检异常类型，调用方无需显式 catch */
  @SuppressWarnings("unchecked")
  final class SneakyThrow {

    private SneakyThrow() {}

    static <E extends Throwable> RuntimeException rethrow(final Throwable error) throws E {
      throw (E) error;
    }
  }
}
