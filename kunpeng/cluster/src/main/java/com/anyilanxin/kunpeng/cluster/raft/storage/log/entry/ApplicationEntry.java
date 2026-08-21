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
package com.anyilanxin.kunpeng.cluster.raft.storage.log.entry;

import com.anyilanxin.kunpeng.structpack.buffer.BufferWriter;

/** 承载应用（状态机）数据的 Raft 日志条目，以所覆盖的源事件位置区间来标识。 */
public interface ApplicationEntry extends RaftEntry {

  /** 本条目覆盖的最低源位置。 */
  long lowestPosition();

  /** 本条目覆盖的最高源位置。 */
  long highestPosition();

  /** 产出原始应用负载的写入器。 */
  BufferWriter dataWriter();
}
