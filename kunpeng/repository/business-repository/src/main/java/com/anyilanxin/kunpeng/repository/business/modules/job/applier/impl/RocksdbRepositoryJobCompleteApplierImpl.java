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

import com.anyilanxin.kunpeng.protocol.business.impl.record.command.job.JobRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.job.JobLifeCycle;
import com.anyilanxin.kunpeng.repository.business.BusinessRepository;
import com.anyilanxin.kunpeng.repository.business.modules.job.MutableJobRepository;
import com.anyilanxin.kunpeng.repository.business.modules.job.applier.RepositoryJobApplier;

/**
 * job 完成事件应用器。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class RocksdbRepositoryJobCompleteApplierImpl implements RepositoryJobApplier<JobRecord> {

  private final MutableJobRepository job;

  public RocksdbRepositoryJobCompleteApplierImpl(final BusinessRepository repository) {
    job = repository.jobRepository();
  }

  @Override
  public JobLifeCycle valueState() {
    return JobLifeCycle.COMPLETED;
  }

  @Override
  public void applyState(final long key, final JobRecord recordValue) {
    job.delete(key);
  }
}
