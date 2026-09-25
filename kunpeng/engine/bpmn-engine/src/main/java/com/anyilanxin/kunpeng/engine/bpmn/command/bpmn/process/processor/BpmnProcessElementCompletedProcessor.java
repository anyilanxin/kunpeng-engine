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
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl.HistoryCleanupBehavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl.VariableBehavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.process.BpmnProcessElementAbstractProcessor;
import com.anyilanxin.kunpeng.protocol.business.impl.eventlog.BusinessLogRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.activityinstance.ActivityInstanceRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.processinstance.ProcessInstanceRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.activityinstance.ActivityInstanceLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.processinstance.ProcessInstanceLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.processinstance.ProcessInstanceState;
import com.anyilanxin.kunpeng.repository.business.ImmutableBusinessRepository;
import com.anyilanxin.kunpeng.repository.business.modules.activityinstance.ImmutableActivityInstanceRepository;

/**
 * 流程元素已完命令处理器。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class BpmnProcessElementCompletedProcessor extends BpmnProcessElementAbstractProcessor {
  private final LogEventWriter writer;
  private final ImmutableActivityInstanceRepository activityInstance;
  private final HistoryCleanupBehavior historyCleanupBehavior;
  private final VariableBehavior variableBehavior;

  public BpmnProcessElementCompletedProcessor(final LogEventWriter writer) {
    super(writer);
    this.writer = writer;
    final ImmutableBusinessRepository repository = writer.getRepository();
    activityInstance = repository.instanceRepository();
    final Behavior behavior = writer.behavior();
    historyCleanupBehavior = behavior.historyCleanup();
    variableBehavior = behavior.variableBehavior();
  }

  @Override
  public ProcessInstanceLifeCycle processState() {
    return ProcessInstanceLifeCycle.COMPLETED;
  }

  @Override
  public void process(
      final BpmnProcess element, final BusinessLogRecord<ProcessInstanceRecord> logRecord) {
    final ProcessInstanceRecord value = logRecord.getValue();
    value.setEndTime(writer.millis());
    value.setState(ProcessInstanceState.COMPLETED);
    // 完成流程实例
    writer.addEvent(
        value.getProcessInstanceId(),
        ProcessInstanceLifeCycle.COMPLETED,
        logRecord.getRequestId(),
        value);

    if (value.getReferenceActivityInstanceId() != -1) {
      final ActivityInstanceRecord record =
          activityInstance.getRecord(value.getReferenceActivityInstanceId());
      writer.addCommand(
          record.getActivityInstanceId(),
          ActivityInstanceLifeCycle.COMPLETING,
          logRecord.getRequestId(),
          record);
    }
    variableBehavior.variableHistory(logRecord.getRequestId(), value);
    historyCleanupBehavior.addHistoryCleanup(value);
  }
}
