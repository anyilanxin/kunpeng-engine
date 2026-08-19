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
package com.anyilanxin.kunpeng.scheduler.clock;

import com.anyilanxin.kunpeng.scheduler.core.CarrierContext;
import java.time.Instant;
import java.time.InstantSource;

/** actor 时钟：由载体线程持有, 每毫秒至多一次系统调用 */
public interface ActorClock extends InstantSource {

  /**
   * @return 跨毫秒返回 true（调度器用它决定定时轮 tick）
   */
  boolean update();

  long getTimeMillis();

  long getNanosSinceLastMillisecond();

  long getNanoTime();

  static ActorClock current() {
    final CarrierContext current = CarrierContext.current();
    return current != null ? current.getClock() : null;
  }

  static long currentTimeMillis() {
    final ActorClock clock = current();
    return clock != null ? clock.getTimeMillis() : System.currentTimeMillis();
  }

  @Override
  default Instant instant() {
    return Instant.ofEpochMilli(getTimeMillis());
  }

  default long millis() {
    return getTimeMillis();
  }
}
