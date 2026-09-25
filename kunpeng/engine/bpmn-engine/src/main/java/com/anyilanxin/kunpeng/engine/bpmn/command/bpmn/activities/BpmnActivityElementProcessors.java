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
package com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.activities;

import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnFlowElement;
import com.anyilanxin.kunpeng.engine.bpmn.LogEventProcessor;
import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;
import com.anyilanxin.kunpeng.protocol.business.ValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.ValueType;
import com.anyilanxin.kunpeng.protocol.business.impl.eventlog.BusinessLogRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.activityinstance.ActivityInstanceRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.activityinstance.ActivityInstanceLifeCycle;

/**
 * 活动实例日志事件处理器：活动元素执行的总入口。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class BpmnActivityElementProcessors implements LogEventProcessor<ActivityInstanceRecord> {
  private final LogEventWriter writer;
  private final BpmnElementProcessors processors;

  public BpmnActivityElementProcessors(
      final LogEventWriter writer, final BpmnElementProcessors processors) {
    this.writer = writer;
    this.processors = processors;
  }

  @Override
  public ValueType valueType() {
    return ValueType.ACTIVITY;
  }

  @Override
  public ValueLifeCycle[] valueLifeCycles() {
    return new ValueLifeCycle[] {
      ActivityInstanceLifeCycle.ACTIVATING,
      ActivityInstanceLifeCycle.ACTIVATING_AFTER,
      ActivityInstanceLifeCycle.ACTIVATED,
      ActivityInstanceLifeCycle.TAKING,
      ActivityInstanceLifeCycle.TAKEN,
      ActivityInstanceLifeCycle.OCCURRED,
      ActivityInstanceLifeCycle.COMPLETING,
      ActivityInstanceLifeCycle.COMPLETING_AFTER,
      ActivityInstanceLifeCycle.COMPLETED,
      ActivityInstanceLifeCycle.TERMINATING,
      ActivityInstanceLifeCycle.TERMINATING_AFTER,
      ActivityInstanceLifeCycle.TERMINATED,
    };
  }

  @Override
  public void processRecord(final BusinessLogRecord<ActivityInstanceRecord> record) {
    final ActivityInstanceLifeCycle valueState = (ActivityInstanceLifeCycle) record.getValueState();
    final BpmnActivityElementProcessor<BpmnFlowElement> elementProcessor =
        processors.getElementProcess(record);
    if (elementProcessor == null) {
      return;
    }
    final BpmnFlowElement executable = processors.getElement(record, elementProcessor);
    final ActivityContent executionContent = processors.createContent(record);
    switch (valueState) {
      case ACTIVATING -> elementProcessor.onActivating(executable, executionContent);
      case ACTIVATING_AFTER -> elementProcessor.onActivatingAfter(executable, executionContent);
      case ACTIVATED -> elementProcessor.onActivated(executable, executionContent);
      case TAKING -> elementProcessor.onTaking(executable, executionContent);
      case TAKEN -> elementProcessor.onTaken(executable, executionContent);
      case COMPLETING -> elementProcessor.onCompleting(executable, executionContent);
      case COMPLETING_AFTER -> elementProcessor.onCompletingAfter(executable, executionContent);
      case COMPLETED -> elementProcessor.onCompleted(executable, executionContent);
      case TERMINATING -> elementProcessor.onTerminating(executable, executionContent);
      case TERMINATING_AFTER -> elementProcessor.onTerminatingAfter(executable, executionContent);
      case TERMINATED -> elementProcessor.onTerminated(executable, executionContent);
    }
  }
}
