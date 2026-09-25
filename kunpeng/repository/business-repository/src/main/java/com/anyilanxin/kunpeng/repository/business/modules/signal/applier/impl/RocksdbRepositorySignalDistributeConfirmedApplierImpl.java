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
package com.anyilanxin.kunpeng.repository.business.modules.signal.applier.impl;

import com.anyilanxin.kunpeng.protocol.business.impl.record.command.signal.SignalDistributeCorrelateRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.signal.SignalDistributeCorrelateLifeCycle;
import com.anyilanxin.kunpeng.repository.business.BusinessRepository;
import com.anyilanxin.kunpeng.repository.business.modules.signal.MutableSignalEventRepository;
import com.anyilanxin.kunpeng.repository.business.modules.signal.applier.RepositorySignalDistributeCorrelateApplier;

/**
 * 信号分发确认事件应用器。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class RocksdbRepositorySignalDistributeConfirmedApplierImpl
    implements RepositorySignalDistributeCorrelateApplier<SignalDistributeCorrelateRecord> {
  private final MutableSignalEventRepository signalSubscription;

  public RocksdbRepositorySignalDistributeConfirmedApplierImpl(
      final BusinessRepository repository) {
    signalSubscription = repository.signalEventRepository();
  }

  @Override
  public SignalDistributeCorrelateLifeCycle valueState() {
    return SignalDistributeCorrelateLifeCycle.CORRELATE_CONFIRMED;
  }

  @Override
  public void applyState(final long key, final SignalDistributeCorrelateRecord record) {
    signalSubscription.updateCorrelate(key, record);
  }
}
