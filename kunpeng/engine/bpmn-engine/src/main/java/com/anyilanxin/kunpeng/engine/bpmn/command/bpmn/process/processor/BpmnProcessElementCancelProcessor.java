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
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl.BatchBehavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.process.BpmnProcessElementAbstractProcessor;
import com.anyilanxin.kunpeng.protocol.business.impl.eventlog.BusinessLogRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.processinstance.ProcessInstanceRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.processinstance.ProcessInstanceLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.processinstance.ProcessInstanceState;
import com.anyilanxin.kunpeng.repository.business.modules.activityinstance.ImmutableActivityInstanceRepository;

/**
 * 流程元素取消命令处理器。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class BpmnProcessElementCancelProcessor extends BpmnProcessElementAbstractProcessor {
  private final LogEventWriter writer;
  private final ImmutableActivityInstanceRepository activityInstance;
  private final BatchBehavior batchBehavior;

  public BpmnProcessElementCancelProcessor(final LogEventWriter writer) {
    super(writer);
    this.writer = writer;
    activityInstance = writer.getRepository().instanceRepository();
    batchBehavior = writer.behavior().batchBehavior();
  }

  @Override
  public ProcessInstanceLifeCycle processState() {
    return ProcessInstanceLifeCycle.CANCEL;
  }

  @Override
  public void process(
      final BpmnProcess element, final BusinessLogRecord<ProcessInstanceRecord> logRecord) {
    final ProcessInstanceRecord value = logRecord.getValue();
    value.setState(ProcessInstanceState.CANCEL);
    value.setEndTime(writer.millis());
    writer.addEvent(
        value.getProcessInstanceId(),
        ProcessInstanceLifeCycle.CANCEL,
        logRecord.getRequestId(),
        value);
    // 查询是否有直接下级
    final boolean haveChild =
        activityInstance.getProcessInstanceChildCount(value.getProcessInstanceId());
    // 如果有则触发批处理，否则直接调整到完成
    if (haveChild) {
      batchBehavior.createProcessInstanceTerminatedBatch(
          ProcessInstanceLifeCycle.CANCELED,
          value,
          logRecord.getRequestId(),
          logRecord.getOperationReferenceKey(),
          logRecord.getBatchOperationReferenceKey());
    } else {
      writer.addCommand(
          value.getProcessInstanceId(),
          ProcessInstanceLifeCycle.CANCELED,
          logRecord.getRequestId(),
          value);
    }
  }
}
