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
package com.anyilanxin.kunpeng.cluster.dispatch;

import com.anyilanxin.kunpeng.cluster.dispatch.scheduling.SchedulerContext;

/**
 * @author zxuanhong
 * @since
 */
public interface SchedulerCheckerAware {

  /** 重新处理成功后、常规处理开始之前的回调 */
  default void onRecovered(final SchedulerContext context) {}

  /** StreamProcessor 处于关闭阶段时的回调。 */
  default void onClose() {}

  /** StreamProcessor 在启动或处理过程中失败时的回调。 */
  default void onFailed() {}

  /** 处理被暂停时的回调；仅在此前已调用过 onRecovered 的前提下才会被调用。 */
  default void onPaused() {}

  /** 处理恢复时的回调；仅在此前已调用过 onPaused 的前提下才会被调用。 */
  default void onResumed() {}

  /**
   * 车道 actor 重建后回调：清除此 checker 可能残留的幻影排程并强制重排一次。
   *
   * <p>幻影排程指提交被底层调度器静默丢弃（如车道 actor FAILED）后残留的"看似已排程"记录， 合并守卫会因它永久拒绝重排。默认空实现，仅有幻影风险的 checker 需要覆写。
   */
  default void rearm() {}
}
