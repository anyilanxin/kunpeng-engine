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
package com.anyilanxin.kunpeng.sink.runtime;

/**
 * 单个分区 Sink 服务的生命周期阶段。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public enum SinkPhase {
  /** 从日志读取记录并交给 Sink 。 */
  RUNNING,
  /** 不读取记录；该阶段可跨重启保持，直到恢复。 */
  PAUSED,
  /** 记录继续流向 Sink ，但确认的位置只先缓冲，待恢复时一并写回。 适用于短暂的维护窗口，避免 Sink 落后太多。 */
  SOFT_PAUSED,
  /** 服务停止后的终态。 */
  CLOSED
}
