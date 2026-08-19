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
package com.anyilanxin.kunpeng.scheduler.future;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** 首个成功者触发回调；其余成功结果交 closer；全部失败才异常回调 */
public class FirstSuccessfullyCompletedFutureConsumer<T> implements BiConsumer<T, Throwable> {

  private final BiConsumer<T, Throwable> callback;
  private final Consumer<T> closer;
  private boolean isCompleted = false;
  private int pendingFutures;

  public FirstSuccessfullyCompletedFutureConsumer(
      final int pendingFutures, final BiConsumer<T, Throwable> callback, final Consumer<T> closer) {
    this.pendingFutures = pendingFutures;
    this.callback = callback;
    this.closer = closer;
  }

  @Override
  public void accept(final T result, final Throwable failure) {
    pendingFutures -= 1;
    if (failure == null) {
      if (!isCompleted) {
        isCompleted = true;
        callback.accept(result, null);
      } else if (closer != null) {
        closer.accept(result);
      }
    } else {
      if (pendingFutures == 0) {
        callback.accept(null, failure);
      }
    }
  }
}
