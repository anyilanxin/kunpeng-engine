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
package com.anyilanxin.kunpeng.cluster.dispatch;

import com.anyilanxin.kunpeng.protocol.admin.AdminValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.admin.impl.eventlog.AdminLogRecord;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;

/**
 * 单状态日志事件处理器接口：处理单一生效状态的事件。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public interface LogEventProcessorSingleState<T extends UnifiedRecordValue>
    extends LogEventProcessor<T> {
  @Override
  default void processRecord(final AdminLogRecord<T> record) {}

  @Override
  default AdminValueLifeCycle[] valueLifeCycles() {
    return new AdminValueLifeCycle[] {valueLifeCycle()};
  }

  AdminValueLifeCycle valueLifeCycle();
}
