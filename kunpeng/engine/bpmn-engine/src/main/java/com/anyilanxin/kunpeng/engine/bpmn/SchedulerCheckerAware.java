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
package com.anyilanxin.kunpeng.engine.bpmn;

import com.anyilanxin.kunpeng.engine.bpmn.scheduling.SchedulerContext;

/**
 * 调度检查器感知接口：向组件注入调度检查器。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public interface SchedulerCheckerAware {

  /** 重放处理成功后、常规处理开始前的回调 */
  default void onRecovered(final SchedulerContext context) {}

  /** StreamProcessor 进入关闭阶段时的回调。 */
  default void onClose() {}

  /** StreamProcessor 在启动或处理过程中失败时的回调。 */
  default void onFailed() {}

  /** 处理被暂停时的回调；只会在 onRecovered 之后被调用。 */
  default void onPaused() {}

  /** 处理被恢复时的回调；只会在 onPaused 之后被调用。 */
  default void onResumed() {}

  /**
   * 车道 actor 重建后回调：清除此 checker 可能残留的幻影排程并强制重排一次。
   *
   * <p>幻影排程指提交被底层调度器静默丢弃（如车道 actor FAILED）后残留的"看似已排程"记录， 合并守卫会因它永久拒绝重排。默认空实现，仅有幻影风险的 checker 需要覆写。
   */
  default void rearm() {}
}
