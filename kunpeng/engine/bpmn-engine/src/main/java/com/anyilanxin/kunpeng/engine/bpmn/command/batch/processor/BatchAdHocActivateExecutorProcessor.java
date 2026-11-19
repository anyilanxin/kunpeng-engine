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
package com.anyilanxin.kunpeng.engine.bpmn.command.batch.processor;

import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnActivity;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnAdHocSubProcess;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnContainer;
import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;
import com.anyilanxin.kunpeng.engine.bpmn.command.batch.AbstractBatchProcessor;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl.VariableBehavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.activities.ActivityContent;
import com.anyilanxin.kunpeng.protocol.business.EngineReservedVariables;
import com.anyilanxin.kunpeng.protocol.business.impl.eventlog.BusinessLogRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.activityinstance.ActivityInstanceRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.processinstance.ProcessInstanceBatchRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.activityinstance.ActivityInstanceLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.processinstance.ProcessInstanceBatchState;
import com.anyilanxin.kunpeng.repository.business.modules.deployment.bpmnresource.ImmutableBpmnResourceRepository;
import com.anyilanxin.kunpeng.repository.business.modules.deployment.bpmnresource.record.ProcessDefinitionRuntime;
import com.anyilanxin.kunpeng.repository.business.modules.processinstance.ImmutableProcessInstanceRepository;
import java.util.List;

/**
 * 临时子流程激活批执行器：每轮按保留变量名单快照（激活集合 elementId）创建一个子活动并自排（子活动 ACTIVATING 先、批命令后）， 被 maxBatch
 * 自然分段。子活动按自身元素类型走常规派发链（用户任务/服务任务等），本执行器只负责创建。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class BatchAdHocActivateExecutorProcessor extends AbstractBatchProcessor {
  private final VariableBehavior variableBehavior;
  private final ImmutableBpmnResourceRepository bpmnResource;
  private final ImmutableProcessInstanceRepository processInstanceState;

  public BatchAdHocActivateExecutorProcessor(final LogEventWriter writer) {
    super(writer);
    variableBehavior = writer.behavior().variableBehavior();
    bpmnResource = writer.getRepository().bpmnResourceRepository();
    processInstanceState = writer.getRepository().processInstanceRepository();
  }

  @Override
  public ProcessInstanceBatchState valueLifeCycle() {
    return ProcessInstanceBatchState.ACTIVATE_ADHOC_CREATE;
  }

  @Override
  public void processRecord(final BusinessLogRecord<ProcessInstanceBatchRecord> record) {
    final ProcessInstanceBatchRecord value = record.getValue();
    final ActivityInstanceRecord instance = activityInstance.getRecord(value.getBatchBusinessId());
    // 主体守卫：inner 容器已终止/完结（行删）或离开激活态即停，发批终态事件收尾
    if (instance == null || instance.getLifeCycle() != ActivityInstanceLifeCycle.ACTIVATED) {
      writer.addEvent(
          record.getKey(), ProcessInstanceBatchState.BATCH_COMPLETED, record.getRequestId(), value);
      return;
    }
    final ProcessDefinitionRuntime runtime =
        bpmnResource.getRuntime(instance.getProcessDefinitionId());
    final BpmnContainer innerContainer =
        runtime
            .executableProcess()
            .getElementById(instance.getActivityDefinitionKey(), BpmnContainer.class);
    final BpmnAdHocSubProcess adHocSubProcess = (BpmnAdHocSubProcess) innerContainer.getFlowScope();
    final List<String> activationSet =
        (List<String>)
            variableBehavior
                .getDirectVariable(new ActivityContent(instance, record.getRequestId()))
                .get(EngineReservedVariables.ADHOC_ACTIVATION_SET);
    final int cursor = (int) value.getCurrentExecutionId();
    final var target = adHocSubProcess.getAdHocActivities().get(activationSet.get(cursor));
    if (!(target instanceof final BpmnActivity activity)) {
      throw new IllegalStateException("临时子流程激活集合名单元素不存在: " + activationSet.get(cursor));
    }
    createChild(new ActivityContent(instance, record.getRequestId()), activity);
    if (cursor + 1 < activationSet.size()) {
      value.setCurrentExecutionId(cursor + 1);
      writer.addCommand(
          value.getBatchId(),
          ProcessInstanceBatchState.ACTIVATE_ADHOC_CREATE,
          record.getRequestId(),
          value);
      return;
    }
    writer.addEvent(
        record.getKey(), ProcessInstanceBatchState.BATCH_COMPLETED, record.getRequestId(), value);
  }

  /** 创建子活动实例并派发激活（原 AdHocSubProcessInnerInstanceElementProcessor 循环体的批化迁移） */
  private void createChild(final ActivityContent parentContent, final BpmnActivity target) {
    final ActivityInstanceRecord copy = parentContent.copy();
    copy.setActivityInstanceId(writer.nextCurrentSourceKey(parentContent.getProcessInstanceId()));
    copy.setParentActivityInstanceId(parentContent.getActivityInstanceId());
    copy.setStartActivityInstanceId(parentContent.getActivityInstanceId());
    copy.setStartActivityDefinitionKey(parentContent.getActivityDefinitionKey());
    final ActivityContent childContent = parentContent.updateValue(copy);
    final ActivityInstanceRecord child = childContent.getValue();
    child.setActivityDefinitionType(target.getElementType());
    child.setActivityDefinitionName(target.getName());
    child.setActivityDefinitionKey(target.getId());
    child.setSequenceCounter(processInstanceState.getSequenceCounter(child.getProcessInstanceId()));
    writer.addCommand(
        child.getActivityInstanceId(),
        ActivityInstanceLifeCycle.ACTIVATING,
        childContent.getRequestId(),
        child);
  }
}
