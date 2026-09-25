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
package com.anyilanxin.kunpeng.engine.bpmn;

import com.anyilanxin.kunpeng.protocol.business.RecordProcessIndex;
import com.anyilanxin.kunpeng.protocol.business.ValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.impl.eventlog.BusinessLogRecord;

/**
 * 日志事件处理器容器：按事件类型索引全部处理器。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@SuppressWarnings({"rawtypes"})
public final class LogEventProcessors {
  private final LogEventProcessor[] processors;

  public LogEventProcessors() {
    processors = new LogEventProcessor[RecordProcessIndex.size()];
  }

  public LogEventProcessors onCommand(final LogEventProcessor<?> processor) {
    for (final ValueLifeCycle lifeCycle : processor.valueLifeCycles()) {
      if (processors[lifeCycle.processIndex()] != null) {
        throw new IllegalStateException(
            "Log event processor has already been registered:"
                + processors[lifeCycle.processIndex()].getClass().getName());
      }
      processors[lifeCycle.processIndex()] = processor;
    }
    return this;
  }

  public LogEventProcessor<?> getProcessor(final BusinessLogRecord record) {
    final LogEventProcessor processor = processors[record.getValueState().processIndex()];
    if (processor == null) {
      throw new IllegalStateException(
          "Not Have LogEventProcessor registered for record:" + record.getValueState());
    }
    return processor;
  }
}
