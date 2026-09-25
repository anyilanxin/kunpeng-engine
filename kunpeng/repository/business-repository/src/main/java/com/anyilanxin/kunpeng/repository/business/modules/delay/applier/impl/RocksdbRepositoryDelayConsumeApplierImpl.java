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
package com.anyilanxin.kunpeng.repository.business.modules.delay.applier.impl;

import com.anyilanxin.kunpeng.protocol.business.impl.record.command.delay.DelayEventCommandRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.delay.DelayLifeCycle;
import com.anyilanxin.kunpeng.repository.business.BusinessRepository;
import com.anyilanxin.kunpeng.repository.business.modules.delay.MutableDelayRepository;
import com.anyilanxin.kunpeng.repository.business.modules.delay.applier.RepositoryDelayApplier;

/**
 * 延迟事件消费事件应用器。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class RocksdbRepositoryDelayConsumeApplierImpl
    implements RepositoryDelayApplier<DelayEventCommandRecord> {

  private final MutableDelayRepository mutable;

  public RocksdbRepositoryDelayConsumeApplierImpl(final BusinessRepository repository) {
    mutable = repository.delayRepository();
  }

  @Override
  public DelayLifeCycle valueState() {
    return DelayLifeCycle.CONSUME;
  }

  @Override
  public void applyState(final long key, final DelayEventCommandRecord recordValue) {
    mutable.delete(key);
  }
}
