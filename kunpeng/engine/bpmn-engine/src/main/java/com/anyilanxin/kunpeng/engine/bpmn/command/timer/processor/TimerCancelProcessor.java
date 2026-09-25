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
package com.anyilanxin.kunpeng.engine.bpmn.command.timer.processor;

import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;
import com.anyilanxin.kunpeng.engine.bpmn.command.timer.TimerAbstractProcessor;
import com.anyilanxin.kunpeng.protocol.business.impl.eventlog.BusinessLogRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.timer.TimerEventRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.timer.TimerLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.timer.TimerState;

/**
 * 定时器取消处理器。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class TimerCancelProcessor extends TimerAbstractProcessor {

  public TimerCancelProcessor(final LogEventWriter writer) {
    super(writer);
  }

  @Override
  public void processRecord(final BusinessLogRecord<TimerEventRecord> record) {
    final TimerEventRecord value = record.getValue();
    value.setEndTime(writer.millis());
    value.setState(TimerState.CANCELED);
    writer.addEvent(value.getTimerId(), TimerLifeCycle.CANCELED, record.getRequestId(), value);
  }

  @Override
  public TimerLifeCycle valueLifeCycle() {
    return TimerLifeCycle.CANCEL;
  }
}
