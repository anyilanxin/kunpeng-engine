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

import com.anyilanxin.kunpeng.scheduler.SchedulerLoggers;
import com.anyilanxin.kunpeng.scheduler.core.CarrierContext;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.agrona.LangUtil;
import org.slf4j.Logger;

/** future 工具：checked→unchecked；join 禁止在 actor 线程等待未完成 future */
public class FutureUtil {

  private FutureUtil() {}

  private static final Logger LOG = SchedulerLoggers.FUTURE_LOGGER;

  public static <T> T join(final Future<T> f) {
    ensureNotBlockingActorThread(f);
    try {
      return f.get();
    } catch (final Exception e) {
      LangUtil.rethrowUnchecked(e);
    }
    return null;
  }

  public static <T> T join(final Future<T> f, final long timeout, final TimeUnit timeUnit) {
    ensureNotBlockingActorThread(f);
    try {
      return f.get(timeout, timeUnit);
    } catch (final Exception e) {
      LOG.error("join 等待 future 完成异常", e);
      LangUtil.rethrowUnchecked(e);
    }
    return null;
  }

  /** actor 线程等待未完成 future 会永久占用载体线程, 必须改为 runOnCompletion 续接 */
  private static void ensureNotBlockingActorThread(final Future<?> f) {
    if (CarrierContext.onActorThread() && !f.isDone()) {
      final var error =
          new IllegalStateException(
              "Actor 在 future 未完成时调用了 join()。actor 必须非阻塞，请改用 actor.runOnCompletion()。");
      LOG.error("join 在 actor 线程上等待未完成的 future", error);
      throw error;
    }
  }
}
