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
package com.anyilanxin.kunpeng.engine.bpmn.commandapi.usertask.processor;

import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnUserTask;
import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl.AddSignBehavior;
import com.anyilanxin.kunpeng.engine.bpmn.commandapi.usertask.UserTaskApiAbstractProcessor;
import com.anyilanxin.kunpeng.protocol.business.impl.eventlog.BusinessLogRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.activityinstance.ActivityInstanceRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.usertask.UserTaskRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.usertask.addsign.AddSignBeforeRequestRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.usertask.addsign.AddSignBeforeResponseRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.activityinstance.AddSignState;
import com.anyilanxin.kunpeng.protocol.business.record.command.usertask.AddSignMode;
import com.anyilanxin.kunpeng.protocol.business.record.command.usertask.AddSignPhase;
import com.anyilanxin.kunpeng.protocol.business.record.command.usertask.AddSignRole;
import com.anyilanxin.kunpeng.protocol.business.record.command.usertask.UserTaskLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.usertask.CommandApiUserTaskValueLifeCycle;
import com.anyilanxin.kunpeng.repository.business.modules.activityinstance.ImmutableActivityInstanceRepository;
import com.anyilanxin.kunpeng.repository.business.modules.deployment.bpmnresource.record.ProcessDefinitionRuntime;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

/**
 * 前加签 API 处理器：门户校验（模型许可/已签收/非冻结/非终态/非加签任务/非自加签）后， 冻结活动、父任务计数 +1、按并行/串行创建加签任务。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class UserTaskAddSignBeforeProcessor
    extends UserTaskApiAbstractProcessor<AddSignBeforeRequestRecord> {
  private final LogEventWriter writer;
  private final ImmutableActivityInstanceRepository activityInstance;
  private final AddSignBehavior addSignBehavior;

  public UserTaskAddSignBeforeProcessor(final LogEventWriter writer) {
    super(writer, writer.getRepository().userTaskRepository());
    this.writer = writer;
    activityInstance = writer.getRepository().instanceRepository();
    addSignBehavior = writer.behavior().addSignBehavior();
  }

  @Override
  public void innerProcessRecord(
      final BusinessLogRecord<AddSignBeforeRequestRecord> record, final UserTaskRecord userTask) {
    final AddSignBeforeRequestRecord value = record.getValue();
    final long requestId = record.getRequestId();
    if (userTask.getLifeCycle() != UserTaskLifeCycle.CREATED) {
      writer.adErrorResponse(requestId, 0, "当前任务状态不允许加签:" + userTask.getState().name());
      return;
    }
    if (userTask.getAddSignRole() == AddSignRole.ADD_SIGN) {
      writer.adErrorResponse(requestId, 0, "加签任务不允许再加签（首期禁止级联加签）");
      return;
    }
    if (StringUtils.isBlank(userTask.getAssignee())) {
      writer.adErrorResponse(requestId, 0, "任务签收后才能加签");
      return;
    }
    final List<String> assignees = value.getAssignees();
    if (assignees.isEmpty()) {
      writer.adErrorResponse(requestId, 0, "加签名单不能为空");
      return;
    }
    if (assignees.contains(userTask.getAssignee())) {
      writer.adErrorResponse(requestId, 0, "不允许加签给原任务审批人本人");
      return;
    }
    final AddSignMode mode = value.getAddSignMode();
    if (mode != AddSignMode.PARALLEL && mode != AddSignMode.SEQUENTIAL) {
      writer.adErrorResponse(requestId, 0, "加签模式必须是并行或串行");
      return;
    }
    final ActivityInstanceRecord instanceRecord =
        activityInstance.getRecord(userTask.getActivityInstanceId());
    if (instanceRecord == null) {
      writer.adErrorResponse(requestId, 0, "当前用户活动实例不存在，无法加签");
      return;
    }
    if (instanceRecord.getAddSignState() != AddSignState.NONE) {
      writer.adErrorResponse(requestId, 0, "当前活动存在进行中的加签，不允许重复加签");
      return;
    }
    final ProcessDefinitionRuntime runtime =
        bpmnResource.getRuntime(userTask.getProcessDefinitionId());
    if (runtime == null) {
      writer.adErrorResponse(requestId, 0, "流程定义不存在，无法加签");
      return;
    }
    // 原任务定义必在运行时定义中（任务由其创建）；未声明 kunpeng:addSign 即拒绝
    final BpmnUserTask element =
        runtime
            .executableProcess()
            .getElementById(userTask.getTaskDefinitionKey(), BpmnUserTask.class);
    if (!element.isAddSignEnabled()) {
      writer.adErrorResponse(requestId, 0, "任务定义未允许加签（建模需声明 kunpeng:addSign）");
      return;
    }

    // 生效：冻结活动 → 父任务计数/模式落行 → 创建加签任务（并行全量/串行首位持名单）
    addSignBehavior.frozeActivity(instanceRecord, requestId);
    userTask.setAddSignMode(mode);
    userTask.setAddSignCount(mode == AddSignMode.PARALLEL ? assignees.size() : 1);
    writer.addEvent(userTask.getTaskId(), UserTaskLifeCycle.UPDATING, requestId, userTask);
    if (mode == AddSignMode.PARALLEL) {
      for (final String assignee : assignees) {
        addSignBehavior.createAddSignTask(
            userTask, instanceRecord, assignee, List.of(), AddSignPhase.BEFORE, mode, requestId);
      }
    } else {
      addSignBehavior.createAddSignTask(
          userTask,
          instanceRecord,
          assignees.getFirst(),
          assignees.subList(1, assignees.size()),
          AddSignPhase.BEFORE,
          mode,
          requestId);
    }
    writer.adResponse(
        CommandApiUserTaskValueLifeCycle.ADD_SIGN_BEFORE_RESPONSE,
        requestId,
        new AddSignBeforeResponseRecord());
  }

  @Override
  public CommandApiUserTaskValueLifeCycle valueLifeCycle() {
    return CommandApiUserTaskValueLifeCycle.ADD_SIGN_BEFORE_REQUEST;
  }
}
