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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.repository.business.modules.job.applier.impl;

import com.anyilanxin.kunpeng.protocol.business.impl.record.command.job.JobRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.job.JobLifeCycle;
import com.anyilanxin.kunpeng.repository.business.BusinessRepository;
import com.anyilanxin.kunpeng.repository.business.modules.job.MutableJobRepository;
import com.anyilanxin.kunpeng.repository.business.modules.job.applier.RepositoryJobApplier;
import org.agrona.DirectBuffer;

/**
 * 超时事件落库：清除激活痕迹（deadline 索引、激活列族、lockOwner/dueDate）并回 READY 列族， 等待轮询/推送重新消费。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class RocksdbRepositoryJobTimedOutApplierImpl implements RepositoryJobApplier<JobRecord> {
  private static final DirectBuffer EMPTY_WORKER = new org.agrona.concurrent.UnsafeBuffer();
  private final MutableJobRepository job;

  public RocksdbRepositoryJobTimedOutApplierImpl(final BusinessRepository repository) {
    job = repository.jobRepository();
  }

  @Override
  public JobLifeCycle valueState() {
    return JobLifeCycle.TIMED_OUT;
  }

  @Override
  public void applyState(final long key, final JobRecord recordValue) {
    // 先按原 dueDate 摘除到期索引并回 READY，再清空激活字段持久化
    job.unActivated(key, recordValue);
    recordValue.setLockOwner(EMPTY_WORKER);
    recordValue.setDueDate(-1);
    job.update(key, recordValue);
  }
}
