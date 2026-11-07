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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.engine.bpmn.command.batch.processor;

import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnMultiInstanceBody;
import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;
import com.anyilanxin.kunpeng.engine.bpmn.command.batch.AbstractBatchProcessor;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl.MultiInstanceBehavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.activities.ActivityContent;
import com.anyilanxin.kunpeng.protocol.business.impl.eventlog.BusinessLogRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.activityinstance.ActivityInstanceRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.processinstance.ProcessInstanceBatchRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.activityinstance.ActivityInstanceLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.processinstance.ProcessInstanceBatchState;
import com.anyilanxin.kunpeng.repository.business.modules.deployment.bpmnresource.ImmutableBpmnResourceRepository;
import com.anyilanxin.kunpeng.repository.business.modules.deployment.bpmnresource.record.ProcessDefinitionRuntime;

/**
 * 并行多实例创建批执行器：每轮创建一个子实例并自排（子实例 ACTIVATING 先、批命令后）， 被 maxBatch 自然分段。
 *
 * <p>完成判定与创建解耦：body 完成走计数代数（completed+terminated==nrOfInstances，见
 * MultiInstanceElementProcessor#calculateComplete），立即完成型子实例在创建窗口内不会提前触发 body 完结。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class BatchMiCreateExecutorProcessor extends AbstractBatchProcessor {
  private final MultiInstanceBehavior multiInstanceBehavior;
  private final ImmutableBpmnResourceRepository bpmnResource;

  public BatchMiCreateExecutorProcessor(final LogEventWriter writer) {
    super(writer);
    multiInstanceBehavior = writer.behavior().multiInstanceBehavior();
    bpmnResource = writer.getRepository().bpmnResourceRepository();
  }

  @Override
  public ProcessInstanceBatchState valueLifeCycle() {
    return ProcessInstanceBatchState.ACTIVATE_MI_CREATE;
  }

  @Override
  public void processRecord(final BusinessLogRecord<ProcessInstanceBatchRecord> record) {
    final ProcessInstanceBatchRecord value = record.getValue();
    final ActivityInstanceRecord instance = activityInstance.getRecord(value.getBatchBusinessId());
    // 主体守卫：body 已终止/完结（行删）或离开激活态（终止中/完成中）即停，发批终态事件收尾
    if (instance == null || instance.getLifeCycle() != ActivityInstanceLifeCycle.ACTIVATED) {
      writer.addEvent(
          record.getKey(), ProcessInstanceBatchState.BATCH_COMPLETED, record.getRequestId(), value);
      return;
    }
    final ProcessDefinitionRuntime runtime =
        bpmnResource.getRuntime(instance.getProcessDefinitionId());
    final BpmnMultiInstanceBody element =
        runtime
            .executableProcess()
            .getElementById(instance.getActivityDefinitionKey(), BpmnMultiInstanceBody.class);
    final boolean hasMore =
        multiInstanceBehavior.createNextParallelChild(
            element, new ActivityContent(instance, record.getRequestId()));
    if (hasMore) {
      value.setCurrentExecutionId(value.getCurrentExecutionId() + 1);
      writer.addCommand(
          value.getBatchId(),
          ProcessInstanceBatchState.ACTIVATE_MI_CREATE,
          record.getRequestId(),
          value);
      return;
    }
    writer.addEvent(
        record.getKey(), ProcessInstanceBatchState.BATCH_COMPLETED, record.getRequestId(), value);
  }
}
