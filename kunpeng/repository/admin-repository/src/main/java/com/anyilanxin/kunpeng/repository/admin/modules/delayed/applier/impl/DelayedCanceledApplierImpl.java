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
package com.anyilanxin.kunpeng.repository.admin.modules.delayed.applier.impl;

import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.delayed.DelayedRecord;
import com.anyilanxin.kunpeng.protocol.admin.record.command.delayed.DelayedLifeCycle;
import com.anyilanxin.kunpeng.repository.admin.AdminRepository;
import com.anyilanxin.kunpeng.repository.admin.modules.delayed.MutableRepositoryDelayed;
import com.anyilanxin.kunpeng.repository.admin.modules.delayed.applier.DelayedApplier;

/**
 * @author zxuanhong
 * @since
 */
public class DelayedCanceledApplierImpl implements DelayedApplier {
  private final MutableRepositoryDelayed repositoryDelayed;

  public DelayedCanceledApplierImpl(final AdminRepository repository) {
    repositoryDelayed = repository.repositoryDelayed();
  }

  @Override
  public DelayedLifeCycle lifeCycle() {
    return DelayedLifeCycle.CANCELED;
  }

  @Override
  public void applyState(final long key, final DelayedRecord recordValue) {
    repositoryDelayed.delete(key, recordValue);
  }
}
