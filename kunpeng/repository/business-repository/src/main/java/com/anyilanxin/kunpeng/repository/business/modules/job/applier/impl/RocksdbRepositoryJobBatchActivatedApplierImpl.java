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
package com.anyilanxin.kunpeng.repository.business.modules.job.applier.impl;

import com.anyilanxin.kunpeng.protocol.business.impl.record.command.job.JobBatchRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.job.JobRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.job.JobBatchLifeCycle;
import com.anyilanxin.kunpeng.repository.business.BusinessRepository;
import com.anyilanxin.kunpeng.repository.business.modules.job.MutableJobRepository;
import com.anyilanxin.kunpeng.repository.business.modules.job.applier.RepositoryJobBatchApplier;
import com.anyilanxin.kunpeng.structpack.value.LongValue;
import com.anyilanxin.kunpeng.structpack.value.ValueArray;
import java.util.Optional;
import org.agrona.DirectBuffer;

/**
 * job 批量激活事件应用器。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class RocksdbRepositoryJobBatchActivatedApplierImpl
    implements RepositoryJobBatchApplier<JobBatchRecord> {
  private final MutableJobRepository job;

  public RocksdbRepositoryJobBatchActivatedApplierImpl(final BusinessRepository repository) {
    job = repository.jobRepository();
  }

  @Override
  public JobBatchLifeCycle valueState() {
    return JobBatchLifeCycle.ACTIVATED;
  }

  @Override
  public void applyState(final long key, final JobBatchRecord recordValue) {
    final ValueArray<LongValue> longValues = recordValue.jobKeys();
    final DirectBuffer workerBuffer = recordValue.getWorkerBuffer();
    final long timeout = recordValue.getTimeout();
    for (final LongValue longValue : longValues) {
      final Optional<JobRecord> query = job.query(longValue.getValue());
      if (query.isPresent()) {
        final JobRecord jobRecord = query.get();
        jobRecord.setLockOwner(workerBuffer);
        jobRecord.setDueDate(timeout);
        // 先落记录（lockOwner/dueDate 持久化），再切激活态索引（update 会短暂重建 READY 索引，activated 随即摘除）
        job.update(longValue.getValue(), jobRecord);
        job.activated(longValue.getValue(), jobRecord);
      }
    }
  }
}
