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
package com.anyilanxin.kunpeng.repository.business.modules.async.applier.impl;

import com.anyilanxin.kunpeng.protocol.business.impl.record.command.async.AsyncRequestRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.async.AsyncRequestLifeCycle;
import com.anyilanxin.kunpeng.repository.business.BusinessRepository;
import com.anyilanxin.kunpeng.repository.business.modules.async.MutableAsyncRepository;
import com.anyilanxin.kunpeng.repository.business.modules.async.applier.RepositoryAsyncApplier;

/**
 * 异步请求登记事件应用器。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class RocksdbRepositoryAsyncAddApplierImpl
    implements RepositoryAsyncApplier<AsyncRequestRecord> {
  private final MutableAsyncRepository mutable;

  public RocksdbRepositoryAsyncAddApplierImpl(final BusinessRepository repository) {
    mutable = repository.asyncRepository();
  }

  @Override
  public AsyncRequestLifeCycle valueState() {
    return AsyncRequestLifeCycle.CREATED;
  }

  @Override
  public void applyState(final long key, final AsyncRequestRecord recordValue) {
    mutable.add(key, recordValue);
  }
}
