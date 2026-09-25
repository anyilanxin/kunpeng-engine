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
package com.anyilanxin.kunpeng.repository.business.modules.activityinstance.applier.impl;

import com.anyilanxin.kunpeng.protocol.business.impl.record.command.activityinstance.ActivityInstanceRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.activityinstance.ActivityInstanceLifeCycle;
import com.anyilanxin.kunpeng.repository.business.BusinessRepository;
import com.anyilanxin.kunpeng.repository.business.modules.activityinstance.MutableActivityInstanceRepository;
import com.anyilanxin.kunpeng.repository.business.modules.activityinstance.applier.RepositoryActivityInstanceApplier;

/**
 * 活动实例监听器拒绝事件应用器。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class RocksdbRepositoryActivityInstanceListenerDenyApplierImpl
    implements RepositoryActivityInstanceApplier<ActivityInstanceRecord> {
  private final MutableActivityInstanceRepository activityInstance;

  public RocksdbRepositoryActivityInstanceListenerDenyApplierImpl(
      final BusinessRepository repository) {
    activityInstance = repository.instanceRepository();
  }

  @Override
  public void applyState(final long key, final ActivityInstanceRecord recordValue) {
    activityInstance.update(key, recordValue);
  }

  @Override
  public ActivityInstanceLifeCycle valueState() {
    return ActivityInstanceLifeCycle.LISTENER_DENY;
  }
}
