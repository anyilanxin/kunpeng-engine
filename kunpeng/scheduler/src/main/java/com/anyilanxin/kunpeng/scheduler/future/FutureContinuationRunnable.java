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
import org.slf4j.Logger;

/** future 完成续接：成功取值回调, 失败传异常 */
public class FutureContinuationRunnable<T> implements Runnable {

  private final ActorFuture<T> future;
  private final BiConsumer<T, Throwable> consumer;
  private static final Logger LOG = com.anyilanxin.kunpeng.scheduler.Loggers.FUTURE_LOGGER;

  public FutureContinuationRunnable(
      final ActorFuture<T> future, final BiConsumer<T, Throwable> consumer) {
    this.future = future;
    this.consumer = consumer;
  }

  @Override
  public void run() {
    if (!future.isCompletedExceptionally()) {
      try {
        final T res = future.get();
        consumer.accept(res, null);
      } catch (final Throwable e) {
        LOG.error("Continuing on future completion failed", e);
      }
    } else {
      consumer.accept(null, future.getException());
    }
  }
}
