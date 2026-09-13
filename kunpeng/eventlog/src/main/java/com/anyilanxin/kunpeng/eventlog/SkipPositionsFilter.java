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

import java.util.Set;
import org.agrona.collections.LongHashSet;

/** 按 position 集合跳过条目的过滤器（导出回放时跳过已导出的批） */
public final class SkipPositionsFilter implements EntryFilter {

  private final LongHashSet positionsToSkip;

  private SkipPositionsFilter(final LongHashSet positionsToSkip) {
    this.positionsToSkip = positionsToSkip;
  }

  public static SkipPositionsFilter of(final Set<Long> positionsToSkip) {
    final LongHashSet longHashSet = new LongHashSet(positionsToSkip.size());
    longHashSet.addAll(positionsToSkip);
    return new SkipPositionsFilter(longHashSet);
  }

  /**
   * @return 条目 position 不在跳过集合中时为 true
   */
  @Override
  public boolean applies(final LoggedEntry entry) {
    return positionsToSkip.isEmpty() || !positionsToSkip.contains(entry.getPosition());
  }
}
