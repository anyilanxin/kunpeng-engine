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
package com.anyilanxin.kunpeng.eventlog;

/**
 * 写入上下文：标识条目来源，决定流控行为。
 *
 * <ul>
 *   <li>{@link #USER_COMMAND}：客户端命令——受 AIMD 在途窗口与写入速率限制约束，超限可被拒
 *   <li>{@link #PROCESSING_RESULT}：引擎处理结果——不受流控（引擎自身背压由处理循环天然约束）
 *   <li>{@link #INTER_PARTITION}：跨分区命令转发——不受流控
 *   <li>{@link #SCHEDULED}：定时任务触发——不受流控
 *   <li>{@link #INTERNAL}：内部写入（补写/重放等）——永不拒绝
 * </ul>
 */
public enum WriteContext {
  USER_COMMAND,
  PROCESSING_RESULT,
  INTER_PARTITION,
  SCHEDULED,
  INTERNAL
}
