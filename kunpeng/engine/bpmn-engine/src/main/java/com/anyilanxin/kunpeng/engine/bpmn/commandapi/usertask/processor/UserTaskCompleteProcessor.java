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
package com.anyilanxin.kunpeng.engine.bpmn.commandapi.usertask.processor;

import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnUserTask;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.transformation.BpmnTransformer;
import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl.AddSignBehavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl.VariableBehavior;
import com.anyilanxin.kunpeng.engine.bpmn.commandapi.usertask.UserTaskApiAbstractProcessor;
import com.anyilanxin.kunpeng.protocol.business.impl.eventlog.BusinessLogRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.activityinstance.ActivityInstanceRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.usertask.UserTaskRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.usertask.complete.UserTaskCompleteRequestRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.activityinstance.AddSignState;
import com.anyilanxin.kunpeng.protocol.business.record.command.usertask.AddSignMode;
import com.anyilanxin.kunpeng.protocol.business.record.command.usertask.UserTaskLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.usertask.CommandApiUserTaskValueLifeCycle;
import com.anyilanxin.kunpeng.repository.business.modules.activityinstance.ImmutableActivityInstanceRepository;
import com.anyilanxin.kunpeng.repository.business.modules.deployment.bpmnresource.record.ProcessDefinitionRuntime;
import com.anyilanxin.kunpeng.repository.business.modules.key.ImmutableKeyGeneratorRepository;
import com.anyilanxin.kunpeng.repository.business.modules.usertask.ImmutableUserTaskRepository;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;

/**
 * 用户任务完成 API 处理器。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class UserTaskCompleteProcessor
    extends UserTaskApiAbstractProcessor<UserTaskCompleteRequestRecord> {
  private final LogEventWriter writer;
  private final BpmnTransformer bpmnTransformer;
  private final ImmutableKeyGeneratorRepository keyGenerator;
  private final int sourceId;
  private final VariableBehavior variableBehavior;
  private final ImmutableActivityInstanceRepository activityInstance;
  private final ImmutableUserTaskRepository userTask;
  private final AddSignBehavior addSignBehavior;

  public UserTaskCompleteProcessor(final LogEventWriter writer) {
    super(writer, writer.getRepository().userTaskRepository());
    this.writer = writer;
    activityInstance = writer.getRepository().instanceRepository();
    keyGenerator = writer.getRepository().keyGeneratorRepository();
    userTask = writer.getRepository().userTaskRepository();
    bpmnTransformer = writer.getBpmnTransformer();
    sourceId = writer.getSourceId();
    variableBehavior = writer.behavior().variableBehavior();
    addSignBehavior = writer.behavior().addSignBehavior();
  }

  /**
   * 完成请求携带的后加签并入预登记：名单/模式/模型许可校验后合并落行，原任务完成时消费创建。
   *
   * @return 校验与合并成功返回真；否则已回错误响应返回假
   */
  private boolean mergeCarriedAfterSign(
      final BusinessLogRecord<UserTaskCompleteRequestRecord> record,
      final UserTaskRecord userTask,
      final ActivityInstanceRecord instanceRecord) {
    final UserTaskCompleteRequestRecord value = record.getValue();
    final List<String> assignees = value.getAddSignAssignees();
    final AddSignMode mode = value.getAddSignMode();
    if (mode != AddSignMode.PARALLEL && mode != AddSignMode.SEQUENTIAL) {
      writer.adErrorResponse(record.getRequestId(), 0, "携带后加签时必须指定并行或串行模式");
      return false;
    }
    if (StringUtils.isNotBlank(userTask.getAssignee())
        && assignees.contains(userTask.getAssignee())) {
      writer.adErrorResponse(record.getRequestId(), 0, "不允许加签给原任务审批人本人");
      return false;
    }
    final ProcessDefinitionRuntime runtime =
        bpmnResource.getRuntime(userTask.getProcessDefinitionId());
    if (runtime == null) {
      writer.adErrorResponse(record.getRequestId(), 0, "流程定义不存在，无法携带后加签");
      return false;
    }
    final BpmnUserTask element =
        runtime
            .executableProcess()
            .getElementById(userTask.getTaskDefinitionKey(), BpmnUserTask.class);
    if (!element.isAddSignEnabled()) {
      writer.adErrorResponse(record.getRequestId(), 0, "任务定义未允许加签（建模需声明 kunpeng:addSign）");
      return false;
    }
    if (!addSignBehavior.mergeAfterSignRegistration(
        userTask, assignees, mode, record.getRequestId())) {
      writer.adErrorResponse(record.getRequestId(), 0, "已登记的后加签模式与本次不一致，不允许混用");
      return false;
    }
    return true;
  }

  @Override
  public void innerProcessRecord(
      final BusinessLogRecord<UserTaskCompleteRequestRecord> record,
      final UserTaskRecord userTask) {
    final UserTaskCompleteRequestRecord value = record.getValue();
    if (userTask.getLifeCycle() != UserTaskLifeCycle.CREATED) {
      writer.adErrorResponse(
          record.getRequestId(), 0, "当前任务状态不允许此操作:" + userTask.getState().name());
      return;
    }

    final ActivityInstanceRecord instanceRecord =
        activityInstance.getRecord(userTask.getActivityInstanceId());
    if (instanceRecord != null) {
      if (instanceRecord.getAddSignState() == AddSignState.FROZEN) {
        writer.adErrorResponse(record.getRequestId(), 0, "当前活动前加签进行中，任务已冻结，不允许完成");
        return;
      }
      // 完成携带后加签：先校验并合并进预登记（完成与加签创建同命令级联原子）
      final List<String> carriedAssignees = value.getAddSignAssignees();
      if (!carriedAssignees.isEmpty() && !mergeCarriedAfterSign(record, userTask, instanceRecord)) {
        return;
      }
      final Map<String, Object> variables = value.getVariables();
      final Map<String, Object> localVariables = value.getLocalVariables();
      if (!localVariables.isEmpty()) {
        variables.putAll(localVariables);
      }
      userTask.setVariables(variables);
      writer.addCommand(
          userTask.getTaskId(), UserTaskLifeCycle.COMPLETING, record.getRequestId(), userTask);
    } else {
      writer.adErrorResponse(record.getRequestId(), 0, "当前用户活动实力不存在，无法操作完成");
      return;
    }

    final UserTaskCompleteRequestRecord response = new UserTaskCompleteRequestRecord();
    response.setTaskId(userTask.getTaskId());
    writer.adResponse(
        CommandApiUserTaskValueLifeCycle.COMPLETE_RESPONSE, record.getRequestId(), response);
  }

  @Override
  public CommandApiUserTaskValueLifeCycle valueLifeCycle() {
    return CommandApiUserTaskValueLifeCycle.COMPLETE_REQUEST;
  }
}
