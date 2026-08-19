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

import com.anyilanxin.kunpeng.scheduler.Loggers;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.agrona.LangUtil;
import org.slf4j.Logger;

/** future 工具：checked→unchecked */
public class FutureUtil {

  private FutureUtil() {}

  private static final Logger LOG = Loggers.FUTURE_LOGGER;

  public static <T> T join(final Future<T> f) {
    try {
      return f.get();
    } catch (final Exception e) {
      LangUtil.rethrowUnchecked(e);
    }
    return null;
  }

  public static <T> T join(final Future<T> f, final long timeout, final TimeUnit timeUnit) {
    try {
      return f.get(timeout, timeUnit);
    } catch (final Exception e) {
      LOG.error("join timeout");
      LangUtil.rethrowUnchecked(e);
    }
    return null;
  }

  public static Runnable wrap(final Future<?> future) {
    return () -> {
      try {
        future.get();
      } catch (final Exception e) {
        LOG.error("wrap failed");
        LangUtil.rethrowUnchecked(e);
      }
    };
  }
}
