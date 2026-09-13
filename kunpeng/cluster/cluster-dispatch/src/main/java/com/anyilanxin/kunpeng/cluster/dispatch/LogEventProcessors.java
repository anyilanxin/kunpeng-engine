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

import com.anyilanxin.kunpeng.cluster.dispatch.eventlog.LogRecord;
import com.anyilanxin.kunpeng.protocol.admin.AdminRecordProcessIndex;
import com.anyilanxin.kunpeng.protocol.admin.AdminValueLifeCycle;

/**
 * @author zxuanhong
 * @since
 */
@SuppressWarnings({"rawtypes"})
public final class LogEventProcessors {
  private final LogEventProcessor[] processors;

  public LogEventProcessors() {
    processors = new LogEventProcessor[AdminRecordProcessIndex.size()];
  }

  public LogEventProcessors onCommand(final LogEventProcessor<?> processor) {
    for (final AdminValueLifeCycle lifeCycle : processor.valueLifeCycles()) {
      if (lifeCycle.isState()) {
        throw new IllegalStateException(
            "Expected to register command life cycles only, but got state: " + lifeCycle);
      }
      if (processors[lifeCycle.processIndex()] != null) {
        throw new IllegalStateException(
            "Log event processor has already been registered:"
                + processors[lifeCycle.processIndex()].getClass().getName());
      }
      processors[lifeCycle.processIndex()] = processor;
    }
    return this;
  }

  public LogEventProcessor<?> getProcessor(final LogRecord record) {
    final LogEventProcessor processor = processors[record.getValueState().processIndex()];
    if (processor == null) {
      throw new IllegalStateException(
          "Not Have LogEventProcessor registered for record:" + record.getValueState());
    }
    return processor;
  }
}
