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
package com.anyilanxin.kunpeng.eventlog.storage;

import java.util.Iterator;
import org.agrona.DirectBuffer;

/**
 * 存储读游标：按块迭代（块 = 批帧字节，载荷不透明）。
 *
 * <p>{@code next()} 返回的 buffer 视图仅保证到下一次 {@code next()} 前有效。
 */
public interface EventStoreReader extends Iterator<DirectBuffer>, AutoCloseable {

  @Override
  void close();

  /**
   * 定位到包含给定 position 的块（不存在时定位到其之前最近的块；超出末尾时定位到 最后一块——供 seekToEnd 使用）。 实现（Raft 桥）按 journal 条目的 ASQN
   * 区间匹配。
   */
  void seek(long position);
}
