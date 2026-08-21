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
package io.atomix.raft.journal.file;

import io.atomix.raft.journal.CheckedJournalException.FlushException;

/** 可刷盘 segment 的最小能力契约：批量刷盘逻辑只依赖这两个方法。 */
interface FlushableSegment {

  /** @return 当前 segment 已写入的最后索引，可作为刷盘索引的下界 */
  long lastIndex();

  /**
   * 将 segment 中的修改落盘。
   *
   * <p>方法正常返回即代表本 segment 的脏页已（按底层文件系统的语义）写入磁盘。
   *
   * @throws FlushException 刷盘因任何原因失败时抛出
   */
  void flush() throws FlushException;
}
