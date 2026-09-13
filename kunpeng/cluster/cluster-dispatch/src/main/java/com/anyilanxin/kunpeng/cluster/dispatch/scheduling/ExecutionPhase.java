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
 * 流处理器消费其日志时推进的生命周期阶段。
 *
 * <p>处理器从 {@link #INITIAL} 出发，进入 {@link #REPLAY} 依据历史重建状态，随后稳定在 {@link #RUNNING} 处理实时命令。{@link
 * #FAILED} 表示处理器因不可恢复错误而中止； {@link #PAUSED} 表示其消费被临时挂起。
 */
public enum ExecutionPhase {
  INITIAL,
  REPLAY,
  RUNNING,
  FAILED,
  PAUSED
}
