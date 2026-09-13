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

import java.util.List;

/**
 * 追加端：一次调用 = 一个批 = 一个存储块；批内 entry 获得连续 position。
 *
 * <p>成功语义为"已定序"（position 唯一分配）；多写者并发下块帧可能延后提交 （有序提交链），消费方以 commit 通知为准。
 */
public interface EventLogWriter extends AutoCloseable {

  AppendResult tryAppend(WriteContext context, AppendEntry entry);

  AppendResult tryAppend(WriteContext context, List<AppendEntry> entries);

  /**
   * @param sourcePosition 批级源 position（引发本批写入的源条目；无源传 -1）
   */
  AppendResult tryAppend(WriteContext context, List<AppendEntry> entries, long sourcePosition);

  /** 是否可再写（流控余量探测，不做真写入） */
  boolean canAppend(int entryCount, int batchSizeBytes);

  @Override
  void close();
}
