/*
 * Copyright © 2026 anyilanxin zxh(anyilanxin@aliyun.com)
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.cluster.manager.admin.raft;

import com.anyilanxin.kunpeng.cluster.raft.logentry.EntryValidator;
import com.anyilanxin.kunpeng.cluster.raft.storage.log.entry.ApplicationEntry;

/**
 * 管理分区的 Raft 日志条目校验器，校验应用条目（application entry）位置连续，不允许存在间隙。
 *
 * @author zxuanhong
 * @since
 */
public class AdminEntryValidator implements EntryValidator {
  @Override
  public ValidationResult validateEntry(
      final ApplicationEntry lastEntry, final ApplicationEntry entry) {
    if (lastEntry == null) {
      return ValidationResult.ok();
    }
    if (entry.lowestPosition() != lastEntry.highestPosition() + 1) {
      return ValidationResult.failure(
          String.format(
              "Expected no gaps between application entries, but the last application entry had a "
                  + "highest position of %d and the current entry has a lowest position of %d",
              lastEntry.highestPosition(), entry.lowestPosition()));
    }
    return ValidationResult.ok();
  }
}
