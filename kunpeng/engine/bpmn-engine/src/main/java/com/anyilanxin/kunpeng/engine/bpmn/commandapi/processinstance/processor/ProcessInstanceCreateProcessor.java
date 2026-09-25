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
package com.anyilanxin.kunpeng.engine.bpmn.commandapi.processinstance.processor;

import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnProcess;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnStartEvent;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.transformation.BpmnTransformer;
import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl.VariableBehavior;
import com.anyilanxin.kunpeng.engine.bpmn.commandapi.processinstance.ProcessInstanceApiAbstractProcessor;
import com.anyilanxin.kunpeng.protocol.business.ValueType;
import com.anyilanxin.kunpeng.protocol.business.impl.eventlog.BusinessLogRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.async.AsyncRequestRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.deployment.ProcessDefinitionRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.processinstance.ProcessInstanceRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.processinstance.create.ProcessInstanceCreateRequestRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.async.AsyncRequestLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.processinstance.ProcessInstanceLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.processinstance.CommandApiProcessInstanceValueLifeCycle;
import com.anyilanxin.kunpeng.repository.business.modules.async.ImmutableAsyncRepository;
import com.anyilanxin.kunpeng.repository.business.modules.deployment.bpmnresource.record.ProcessDefinitionRuntime;
import com.anyilanxin.kunpeng.repository.business.modules.key.ImmutableKeyGeneratorRepository;

/**
 * 流程实例创建 API 处理器。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class ProcessInstanceCreateProcessor
    extends ProcessInstanceApiAbstractProcessor<ProcessInstanceCreateRequestRecord> {
  final LogEventWriter writer;
  private final BpmnTransformer bpmnTransformer;
  private final ImmutableKeyGeneratorRepository keyGenerator;
  private final int sourceId;
  private final VariableBehavior variableBehavior;
  private final ImmutableAsyncRepository async;

  public ProcessInstanceCreateProcessor(final LogEventWriter writer) {
    super(writer);
    this.writer = writer;
    keyGenerator = writer.getRepository().keyGeneratorRepository();
    bpmnTransformer = writer.getBpmnTransformer();
    sourceId = writer.getSourceId();
    variableBehavior = writer.behavior().variableBehavior();
    async = writer.getRepository().asyncRepository();
  }

  @Override
  public void processRecord(final BusinessLogRecord<ProcessInstanceCreateRequestRecord> record) {
    final ProcessInstanceCreateRequestRecord value = record.getValue();
    final long requestId = record.getRequestId();
    final ProcessDefinitionRuntime executableProcess = getExecutableProcess(requestId, value);
    if (executableProcess != null) {
      startProcess(value, executableProcess, requestId);
    }
  }

  private void startProcess(
      final ProcessInstanceCreateRequestRecord request,
      final ProcessDefinitionRuntime executableDefinition,
      final long requestId) {
    final BpmnProcess executableProcess = executableDefinition.executableProcess();
    final ProcessDefinitionRecord processDefinition = executableDefinition.processDefinition();
    final BpmnStartEvent noneStartEvent = executableProcess.getNoneStartEvent();
    if (noneStartEvent == null) {
      writer.adErrorResponse(requestId, -1, "无有效的启动节点");
      return;
    }
    final long processInstanceId = writer.nextCurrentSourceKey();
    final ProcessInstanceRecord record =
        new ProcessInstanceRecord()
            .setProcessInstanceId(processInstanceId)
            .setRootProcessInstanceId(processInstanceId)
            .setLifeCycle(ProcessInstanceLifeCycle.ACTIVATING)
            .setBusinessKey(request.getBusinessKey())
            .setStartUserId("")
            .setVariables(request.getVariablesBuffer())
            .setStartActivityDefinitionKey(noneStartEvent.getId())
            .setProcessDefinitionId(processDefinition.getProcessDefinitionId())
            .setProcessDefinitionKey(processDefinition.getProcessDefinitionKey());

    // 添加异步请求
    final AsyncRequestRecord requestRecord =
        new AsyncRequestRecord()
            .setKey(record.getProcessInstanceId())
            .setValueType(ValueType.PROCESS_INSTANCE)
            .setValueLifeCycle(ProcessInstanceLifeCycle.ACTIVATING)
            .setRequestId(requestId);
    writer.addEvent(
        requestRecord.getKey(), AsyncRequestLifeCycle.CREATED, requestId, requestRecord);
    // 创建流程实例命令
    writer.addCommand(
        record.getProcessInstanceId(), ProcessInstanceLifeCycle.ACTIVATING, requestId, record);
  }

  @Override
  public CommandApiProcessInstanceValueLifeCycle valueLifeCycle() {
    return CommandApiProcessInstanceValueLifeCycle.CREATE_REQUEST;
  }

  private ProcessDefinitionRuntime getExecutableProcess(
      final long requestId, final ProcessInstanceCreateRequestRecord record) {
    final long processDefinitionId = record.getProcessDefinitionId();
    if (processDefinitionId > 0) {
      final ProcessDefinitionRuntime definitionCache = bpmnResource.getRuntime(processDefinitionId);
      if (definitionCache == null) {
        writer.adErrorResponse(
            requestId, 0, "流程定义 id 不存在:processDefinitionId=" + processDefinitionId);
        return null;
      }
      return definitionCache;
    } else {
      final int processDefinitionVersion = record.getProcessDefinitionVersion();
      final String tenantId = record.getTenantId();
      final String processDefinitionKey = record.getProcessDefinitionKey();
      final ProcessDefinitionRuntime definitionCache =
          bpmnResource.getRuntime(processDefinitionKey, tenantId, processDefinitionVersion);
      if (definitionCache == null) {
        writer.adErrorResponse(
            requestId,
            0,
            "流程定义 key 不存在:processDefinitionKey="
                + processDefinitionKey
                + ",tenantId="
                + tenantId
                + ",version="
                + processDefinitionVersion);
        return null;
      }
      return definitionCache;
    }
  }
}
