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

/** 载体统一接口：平台 runner 与虚拟载体共用（定时器路由/阻塞外包/回调队列/线程判定） */
public interface CellRunner {

  TimerHub getTimers();

  BlockingRunner getBlocking();

  CallbackQueue getCallbacks();

  /**
   * @return 当前线程是否本载体
   */
  boolean isOnOwnerThread();

  /** 有新工作提示（idle backoff 复位 / unpark） */
  void hint();

  /** 载体名（诊断） */
  String getName();
}
