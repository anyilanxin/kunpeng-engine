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
package com.anyilanxin.kunpeng.cluster.raft.journal;

/**
 * 日志内容损坏异常。
 *
 * <p>典型触发场景：记录只写了一半、校验和不匹配、磁盘上的编码版本无法识别等。该异常被视为
 * 不可自愈：日志自身无法修复，通常需要人工介入（例如从快照恢复）。
 */
public final class CorruptedJournalException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  /** 仅携带损坏详情描述、无根因的构造入口。 */
  public CorruptedJournalException(final String message) {
    this(message, null);
  }

  /** 仅携带根因、无额外描述的构造入口。 */
  public CorruptedJournalException(final Throwable cause) {
    this(null, cause);
  }

  /** 完整构造入口：所有其他构造形态最终都汇聚到这里。 */
  public CorruptedJournalException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
