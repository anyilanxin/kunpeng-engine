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
package com.anyilanxin.kunpeng.cluster.dispatch.scheduling;

/**
 * 一份在持有其队列的线程上执行的已调度工作。
 *
 * <p>产出的命令经由传入的 {@link CommandCollector} 交付而非直接返回，调度器因此无需关心 命令如何缓冲与刷写。
 */
public interface TimerJob {

  /**
   * 执行本任务，收集其产出的命令。
   *
   * @param collector 产出命令应追加到的收集器
   * @return 本次执行期间收集到的命令批次
   */
  CommandBatch run(CommandCollector collector);
}
