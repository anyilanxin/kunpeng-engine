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
import com.anyilanxin.kunpeng.engine.bpmn.commandapi.processinstance.ProcessInstanceApiAbstractProcessor;
import com.anyilanxin.kunpeng.protocol.business.impl.eventlog.BusinessLogRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.deployment.ProcessDefinitionRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.processinstance.create.ProcessInstanceCreateRequestRecord;
import com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.processinstance.CommandApiProcessInstanceValueLifeCycle;
import com.anyilanxin.kunpeng.repository.business.modules.deployment.bpmnresource.record.ProcessDefinitionRuntime;
import com.anyilanxin.kunpeng.repository.business.modules.key.ImmutableKeyGeneratorRepository;

/**
 * 流程实例创建并等待结果 API 处理器。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class ProcessInstanceActivateAndResultProcessor
    extends ProcessInstanceApiAbstractProcessor<ProcessInstanceCreateRequestRecord> {
  final LogEventWriter writer;
  private final BpmnTransformer bpmnTransformer;
  private final ImmutableKeyGeneratorRepository keyGenerator;
  private final int sourceId;

  public ProcessInstanceActivateAndResultProcessor(final LogEventWriter writer) {
    super(writer);
    this.writer = writer;
    keyGenerator = writer.getRepository().keyGeneratorRepository();
    bpmnTransformer = writer.getBpmnTransformer();
    sourceId = writer.getSourceId();
  }

  @Override
  public void processRecord(final BusinessLogRecord<ProcessInstanceCreateRequestRecord> record) {
    final ProcessInstanceCreateRequestRecord value = record.getValue();
    final long requestId = record.getRequestId();
    final ProcessExecutableDefinition executableProcess = getExecutableProcess(requestId, value);
    if (executableProcess != null) {
      startProcess(executableProcess, requestId);
    }
  }

  private void startProcess(
      final ProcessExecutableDefinition executableDefinition, final long requestId) {
    final BpmnProcess executableProcess = executableDefinition.executableProcess;
    final BpmnStartEvent noneStartEvent = executableProcess.getNoneStartEvent();
    //    if (noneStartEvent == null) {
    //      return;
    //    }
    //    final ProcessDefinitionRecord processDefinition =
    // executableDefinition.processDefinitionRecord;
    //    final ActivityInstanceRecord executionRecord = new ActivityInstanceRecord();
    //    executionRecord.setElementId(executableProcess.getId());
    //    executionRecord.setProcessDefinitionId(processDefinition.getId());
    //    executionRecord.setProcessDefinitionKey(processDefinition.getProcessDefinitionKey());
    //    executionRecord.setProcessName(executableProcess.getName());
    //    executionRecord.setProcessDefinitionVersion(processDefinition.getVersion());
    //    executionRecord.setElementName(executableProcess.getName());
    //    executionRecord.setElementType(BpmnElementType.PROCESS);
    //    executionRecord.setTargetElementId(noneStartEvent.getId());
    //    executionRecord.setExecutionState(ActivityInstanceLifeCycle.ACTIVATING);
    //    executionRecord.setTenantId(processDefinition.getTenantId());
    //    executionRecord.setNeedResponse(true);
    //    executionRecord.setResponseType(BpmnResponseType.COMPLETE);
    //    writer.addCommand(
    //        StoreValue.ACTIVITY, ActivityInstanceLifeCycle.ACTIVATING, requestId,
    // executionRecord);
    writer.adErrorResponse(requestId, -1, "业务暂未实现");
  }

  @Override
  public CommandApiProcessInstanceValueLifeCycle valueLifeCycle() {
    return CommandApiProcessInstanceValueLifeCycle.CREATE_AND_RESULT_REQUEST;
  }

  private ProcessExecutableDefinition getExecutableProcess(
      final long requestId, final ProcessInstanceCreateRequestRecord record) {
    final long processDefinitionId = record.getProcessDefinitionId();
    final ProcessDefinitionRecord processDefinitionRecord;
    if (processDefinitionId > 0) {
      processDefinitionRecord = bpmnResource.get(processDefinitionId);
      if (processDefinitionRecord == null) {
        writer.adErrorResponse(
            requestId, 0, "流程定义 id 不存在:processDefinitionId=" + processDefinitionId);
        return null;
      }
    } else {
      final int processDefinitionVersion = record.getProcessDefinitionVersion();
      final String tenantId = record.getTenantId();
      final String processDefinitionKey = record.getProcessDefinitionKey();
      if (processDefinitionVersion > 0) {
        processDefinitionRecord =
            bpmnResource.get(processDefinitionKey, tenantId, processDefinitionVersion);
        if (processDefinitionRecord == null) {
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
      } else {
        processDefinitionRecord = bpmnResource.get(processDefinitionKey, tenantId);
        if (processDefinitionRecord == null) {
          writer.adErrorResponse(
              requestId,
              0,
              "流程定义 key 不存在:processDefinitionKey="
                  + processDefinitionKey
                  + ",tenantId="
                  + tenantId);
          return null;
        }
      }
    }
    final ProcessDefinitionRuntime executableProcess =
        bpmnResource.getRuntime(processDefinitionRecord.getProcessDefinitionId());
    if (executableProcess == null) {
      writer.adErrorResponse(
          requestId,
          0,
          "流程定义 id 对应的模型信息 不存在:processDefinitionId="
              + processDefinitionRecord.getProcessDefinitionId());
      return null;
    }
    return new ProcessExecutableDefinition(
        executableProcess.executableProcess(), processDefinitionRecord);
  }

  private record ProcessExecutableDefinition(
      BpmnProcess executableProcess, ProcessDefinitionRecord processDefinitionRecord) {}
}
