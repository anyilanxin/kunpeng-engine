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
package com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.process.processor;

import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnProcess;
import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.Behavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl.BatchBehavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl.VariableBehavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.process.BpmnProcessElementAbstractProcessor;
import com.anyilanxin.kunpeng.protocol.business.impl.eventlog.BusinessLogRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.activityinstance.ActivityInstanceRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.processinstance.ProcessInstanceRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.activityinstance.ActivityInstanceLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.processinstance.ProcessInstanceLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.processinstance.ProcessInstanceState;
import com.anyilanxin.kunpeng.repository.business.modules.activityinstance.ImmutableActivityInstanceRepository;

/**
 * 流程元素已取消命令处理器。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class BpmnProcessElementCanceledProcessor extends BpmnProcessElementAbstractProcessor {
  private final LogEventWriter writer;
  private final ImmutableActivityInstanceRepository activityInstance;
  private final BatchBehavior batchBehavior;
  private final VariableBehavior variableBehavior;

  public BpmnProcessElementCanceledProcessor(final LogEventWriter writer) {
    super(writer);
    this.writer = writer;
    activityInstance = writer.getRepository().instanceRepository();
    batchBehavior = writer.behavior().batchBehavior();
    final Behavior behavior = writer.behavior();
    variableBehavior = behavior.variableBehavior();
  }

  @Override
  public ProcessInstanceLifeCycle processState() {
    return ProcessInstanceLifeCycle.CANCELED;
  }

  @Override
  public void process(
      final BpmnProcess element, final BusinessLogRecord<ProcessInstanceRecord> logRecord) {
    final ProcessInstanceRecord value = logRecord.getValue();
    value.setState(ProcessInstanceState.CANCELED);
    value.setEndTime(writer.millis());
    value.setLifeCycle(ProcessInstanceLifeCycle.CANCELED);
    writer.addEvent(
        value.getProcessInstanceId(),
        ProcessInstanceLifeCycle.CANCELED,
        logRecord.getRequestId(),
        value);

    if (logRecord.getBatchOperationReferenceKey() > 0) {
      batchBehavior.handleTerminatedBatchReference(logRecord.getBatchOperationReferenceKey());
      return;
    }
    if (value.getReferenceActivityInstanceId() > 0) {
      final ActivityInstanceRecord record =
          activityInstance.getRecord(value.getReferenceActivityInstanceId());
      writer.addCommand(
          record.getActivityInstanceId(),
          ActivityInstanceLifeCycle.TERMINATING,
          logRecord.getRequestId(),
          record);
    }
    variableBehavior.variableHistory(logRecord.getRequestId(), value);
  }
}
