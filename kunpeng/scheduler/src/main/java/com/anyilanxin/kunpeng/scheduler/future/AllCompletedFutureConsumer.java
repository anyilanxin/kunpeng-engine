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

/** 计数器：全部完成后回调一次（保留最后一个异常） */
public class AllCompletedFutureConsumer<T> implements BiConsumer<T, Throwable> {

  private final Consumer<Throwable> callback;
  private int pendingFutures;
  private Throwable occuredFailure = null;

  public AllCompletedFutureConsumer(final int pendingFutures, final Consumer<Throwable> callback) {
    this.pendingFutures = pendingFutures;
    this.callback = callback;
  }

  @Override
  public void accept(final T result, final Throwable failure) {
    pendingFutures -= 1;
    if (failure != null) {
      occuredFailure = failure;
    }
    if (pendingFutures == 0) {
      callback.accept(occuredFailure);
    }
  }
}
