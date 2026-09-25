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
 * <p>典型诱因：记录只写入一半、CRC 校验和不一致、磁盘上的编码版本无法识别。损坏被视为 不可自愈——日志自身无法修复，需要人工介入（例如从快照重建）。
 */
public final class CorruptedJournalException extends RuntimeException {

  /** 只携带描述、不带根因。 */
  public CorruptedJournalException(final String message) {
    super(message, null);
  }

  /** 只携带根因、不带额外描述。 */
  public CorruptedJournalException(final Throwable cause) {
    super(null, cause);
  }

  /** 描述与根因同时携带。 */
  public CorruptedJournalException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
