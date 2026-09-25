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
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl.JobBehavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl.VariableBehavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.process.BpmnProcessElementAbstractProcessor;
import com.anyilanxin.kunpeng.protocol.business.ValueType;
import com.anyilanxin.kunpeng.protocol.business.impl.eventlog.BusinessLogRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.async.AsyncRequestRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.processinstance.ProcessInstanceRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.processinstance.create.ProcessInstanceCreateResponseRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.async.AsyncRequestLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.processinstance.ProcessInstanceLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.processinstance.ProcessInstanceListenerType;
import com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.processinstance.CommandApiProcessInstanceValueLifeCycle;
import com.anyilanxin.kunpeng.repository.business.ImmutableBusinessRepository;
import com.anyilanxin.kunpeng.repository.business.modules.async.ImmutableAsyncRepository;
import java.util.Optional;

/**
 * 流程元素激活中命令处理器。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class BpmnProcessElementActivatingProcessor extends BpmnProcessElementAbstractProcessor {
  private final LogEventWriter writer;
  private final ImmutableAsyncRepository async;
  private final JobBehavior jobBehavior;
  private final ProcessInstanceCreateResponseRecord response =
      new ProcessInstanceCreateResponseRecord();
  private final VariableBehavior variableBehavior;

  public BpmnProcessElementActivatingProcessor(final LogEventWriter writer) {
    super(writer);
    this.writer = writer;
    final Behavior behavior = writer.behavior();
    final ImmutableBusinessRepository repository = writer.getRepository();
    async = repository.asyncRepository();
    jobBehavior = behavior.jobBehavior();
    variableBehavior = behavior.variableBehavior();
  }

  @Override
  public ProcessInstanceLifeCycle processState() {
    return ProcessInstanceLifeCycle.ACTIVATING;
  }

  @Override
  public void process(
      final BpmnProcess element, final BusinessLogRecord<ProcessInstanceRecord> logRecord) {
    final ProcessInstanceRecord record = logRecord.getValue();
    record.setStartTime(writer.millis());
    final long requestId = logRecord.getRequestId();
    if (record.getStartActivityDefinitionKeys().isEmpty()) {
      writer.adErrorResponse(requestId, -1, "未知的激活节点");
      return;
    }
    // 如果需要响应，则处理响应
    final Optional<AsyncRequestRecord> query =
        async.query(
            record.getProcessInstanceId(),
            ValueType.PROCESS_INSTANCE,
            ProcessInstanceLifeCycle.ACTIVATING);
    if (query.isPresent()) {
      final AsyncRequestRecord requestRecord = query.get();
      response.reset();
      response.setProcessDefinitionId(record.getProcessDefinitionId());
      response.setProcessDefinitionKey(record.getProcessDefinitionKey());
      response.setProcessInstanceId(record.getProcessInstanceId());
      writer.adResponse(
          CommandApiProcessInstanceValueLifeCycle.CREATE_RESPONSE,
          requestRecord.getRequestId(),
          response);
      // 标记异步任务完成
      writer.addEvent(
          requestRecord.getKey(), AsyncRequestLifeCycle.COMPLETED, requestId, requestRecord);
    }
    // 处理变量
    variableBehavior.variableCreate(requestId, record, record.getVariablesBuffer());
    // 激活流程实例
    writer.addEvent(
        record.getProcessInstanceId(), ProcessInstanceLifeCycle.ACTIVATING, requestId, record);

    record.setListenerType(ProcessInstanceListenerType.START);
    record.setListenerIndex(0);
    writer.addCommand(
        record.getProcessInstanceId(),
        ProcessInstanceLifeCycle.LISTENER_CREATE,
        logRecord.getRequestId(),
        record);
  }
}
