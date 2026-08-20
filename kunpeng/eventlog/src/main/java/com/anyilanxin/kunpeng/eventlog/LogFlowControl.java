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

/** 流控对外窄接口：消费方标记处理进度 + 读取水位快照 */
public interface LogFlowControl {

  /** 标记某 position（批的 lastPosition）已处理完毕（处理 actor 调用，推进 AIMD 窗口） */
  void onProcessed(long position);

  /** 标记某 position 已导出（exporter 调用，推进积压水位） */
  void onExported(long position);

  long lastWrittenPosition();

  long lastCommittedPosition();

  long lastProcessedPosition();
}
