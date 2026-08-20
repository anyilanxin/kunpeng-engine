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

import java.util.Iterator;

/**
 * 拉取式条目读游标：跨块迭代 + seek 族。
 *
 * <p>{@code next()} 返回的 {@link LoggedEntry} 是复用视图——到下一次 {@code next()} 前有效。 空 position 区间（失败烧毁产生的
 * gap）在迭代中表现为 position 跳跃，不报错。
 */
public interface EventLogReader extends Iterator<LoggedEntry>, AutoCloseable {

  @Override
  void close();

  /** 定位到包含 position 的块后返回首条目是否可读（position 之前最近条目成为当前项） */
  boolean seek(long position);

  /** 定位到 position 之后的首条目（严格大于）；到尾返回 false */
  boolean seekToNextEntry(long position);

  void seekToFirstEntry();

  /** 定位到末尾并返回最后一条 position；空日志返回 0（position 从 1 起） */
  long seekToEnd();

  /** 当前游标 position（最后读出条目的 position；未读返回 0） */
  long getPosition();

  /** 预览下一条而不推进游标；无下一条返回 null */
  LoggedEntry peekNext();
}
