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
package com.anyilanxin.kunpeng.scheduler.core;

import com.anyilanxin.kunpeng.scheduler.ActorControl;
import com.anyilanxin.kunpeng.scheduler.clock.ActorClock;

/**
 * 载体线程上下文：runner/虚拟载体在运行期绑定，暴露当前时钟与正在执行的 actor 控制。
 * ActorClock.current()/ActorControl.current()/CompletableActorFuture 的 actor 线程判定都经此。
 */
public final class CarrierContext {

  private static final ThreadLocal<CarrierContext> CURRENT = new ThreadLocal<>();

  private final ActorClock clock;
  private ActorControl control;

  private CarrierContext(final ActorClock clock) {
    this.clock = clock;
  }

  public static CarrierContext bind(final ActorClock clock) {
    final CarrierContext context = new CarrierContext(clock);
    CURRENT.set(context);
    return context;
  }

  public static void unbind() {
    CURRENT.remove();
  }

  public static CarrierContext current() {
    return CURRENT.get();
  }

  public static boolean onActorThread() {
    return CURRENT.get() != null;
  }

  public ActorClock getClock() {
    return clock;
  }

  /** 执行 cell 期间由 runner 设置/清除 */
  public void setCurrentControl(final ActorControl control) {
    this.control = control;
  }

  public ActorControl currentControl() {
    return control;
  }
}
