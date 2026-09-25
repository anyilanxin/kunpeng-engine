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
package com.anyilanxin.kunpeng.repository.business.modules.job;

import com.anyilanxin.kunpeng.protocol.business.impl.record.command.job.JobRecord;
import com.anyilanxin.kunpeng.repository.business.ResourceDataSplit;
import com.anyilanxin.kunpeng.repository.business.modules.job.record.DeadlineIndex;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import org.agrona.DirectBuffer;

/**
 * job 域只读仓储接口。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public interface ImmutableJobRepository extends ResourceDataSplit {
  Optional<JobRecord> query(long key);

  JobRecord getRecord(long key);

  void processJobBatch(
      final DirectBuffer type,
      final List<String> tenantIds,
      final BiFunction<Long, JobRecord, Boolean> callback);

  /**
   * 按 deadline 升序遍历已激活且到期时间不晚于 {@code deadlineUpperBound} 的 job。
   *
   * @param deadlineUpperBound 到期上界（UNIX epoch ms，含）
   * @param startAt 断点续扫起点（含），null 从头开始
   * @param visitor 访问器，返回 false 停止本轮遍历（稍后可从断点续扫）
   * @return 本轮最后访问到的条目；全部遍历完成返回 null
   */
  DeadlineIndex forEachTimedOutEntry(
      final long deadlineUpperBound,
      final DeadlineIndex startAt,
      final BiFunction<Long, JobRecord, Boolean> visitor);
}
