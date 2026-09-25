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

import java.io.IOException;
import org.jspecify.annotations.Nullable;

/**
 * 受检型日志异常。
 *
 * <p>与 {@link RuntimeException} 体系相对：这一族异常代表“调用方必须显式决策的运维性失败”， 编译期强制处理，不允许静默吞掉。目前唯一的子类是刷盘失败。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public sealed class CheckedJournalException extends Exception {

  /**
   * 基类构造入口，仅限子类使用。
   *
   * @param message 失败描述
   * @param cause 底层根因，允许为 null
   */
  public CheckedJournalException(final String message, @Nullable final Throwable cause) {
    super(message, cause);
  }

  /** 把映射内存中的脏页写入磁盘失败时抛出。 */
  public static final class FlushException extends CheckedJournalException {

    /**
     * @param cause 触发失败的底层 IO 异常，允许为 null
     */
    public FlushException(@Nullable final IOException cause) {
      super("journal 刷盘失败", cause);
    }
  }
}
