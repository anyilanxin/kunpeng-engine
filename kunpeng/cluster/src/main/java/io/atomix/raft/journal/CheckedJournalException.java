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
package io.atomix.raft.journal;

import java.io.IOException;
import org.jspecify.annotations.Nullable;

/**
 * 受检型日志异常基类。
 *
 * <p>与 {@link RuntimeException} 体系不同，本类及其子类强制调用方显式处理失败场景，用于
 * “失败属于可预期运维事件、调用方必须做出决策”的场合（例如刷盘失败）。
 */
public sealed class CheckedJournalException extends Exception {

  /** 刷盘（将映射内存中的脏页落盘）失败时抛出。 */
  public static final class FlushException extends CheckedJournalException {

    /**
     * 以底层 {@link IOException} 为根因构造刷盘异常。
     *
     * @param cause 触发失败的底层 IO 异常，可为 null
     */
    public FlushException(@Nullable final IOException cause) {
      super("Error when flushing", cause);
    }
  }

  /**
   * 基类构造方法，供子类复用。
   *
   * @param message 异常描述
   * @param cause 根因，可为 null
   */
  public CheckedJournalException(final String message, @Nullable final Throwable cause) {
    super(message, cause);
  }
}
