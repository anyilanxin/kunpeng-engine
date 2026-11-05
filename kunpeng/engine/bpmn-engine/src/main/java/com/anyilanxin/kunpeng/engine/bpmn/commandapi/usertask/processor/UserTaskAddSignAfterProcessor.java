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
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.usertask.addsign.AddSignAfterRequestRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.usertask.addsign.AddSignAfterResponseRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.activityinstance.AddSignState;
import com.anyilanxin.kunpeng.protocol.business.record.command.usertask.AddSignMode;
import com.anyilanxin.kunpeng.protocol.business.record.command.usertask.AddSignRole;
import com.anyilanxin.kunpeng.protocol.business.record.command.usertask.UserTaskLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.usertask.CommandApiUserTaskValueLifeCycle;
import com.anyilanxin.kunpeng.repository.business.modules.activityinstance.ImmutableActivityInstanceRepository;
import com.anyilanxin.kunpeng.repository.business.modules.deployment.bpmnresource.record.ProcessDefinitionRuntime;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

/**
 * 后加签预登记 API 处理器：原任务完成前登记/追加后加签名单（同模式追加去重、跨模式拒绝）， 原任务完成时按登记创建加签任务。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class UserTaskAddSignAfterProcessor
    extends UserTaskApiAbstractProcessor<AddSignAfterRequestRecord> {
  private final LogEventWriter writer;
  private final ImmutableActivityInstanceRepository activityInstance;
  private final AddSignBehavior addSignBehavior;

  public UserTaskAddSignAfterProcessor(final LogEventWriter writer) {
    super(writer, writer.getRepository().userTaskRepository());
    this.writer = writer;
    activityInstance = writer.getRepository().instanceRepository();
    addSignBehavior = writer.behavior().addSignBehavior();
  }

  @Override
  public void innerProcessRecord(
      final BusinessLogRecord<AddSignAfterRequestRecord> record, final UserTaskRecord userTask) {
    final AddSignAfterRequestRecord value = record.getValue();
    final long requestId = record.getRequestId();
    if (userTask.getLifeCycle() != UserTaskLifeCycle.CREATED) {
      writer.adErrorResponse(requestId, 0, "当前任务状态不允许登记后加签:" + userTask.getState().name());
      return;
    }
    if (userTask.getAddSignRole() == AddSignRole.ADD_SIGN) {
      writer.adErrorResponse(requestId, 0, "加签任务不允许再加签（首期禁止级联加签）");
      return;
    }
    final List<String> assignees = value.getAssignees();
    if (assignees.isEmpty()) {
      writer.adErrorResponse(requestId, 0, "后加签名单不能为空");
      return;
    }
    if (StringUtils.isNotBlank(userTask.getAssignee())
        && assignees.contains(userTask.getAssignee())) {
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
      writer.adErrorResponse(requestId, 0, "当前用户活动实例不存在，无法登记后加签");
      return;
    }
    if (instanceRecord.getAddSignState() != AddSignState.NONE) {
      writer.adErrorResponse(requestId, 0, "当前活动存在进行中的加签，不允许登记后加签");
      return;
    }
    final ProcessDefinitionRuntime runtime =
        bpmnResource.getRuntime(userTask.getProcessDefinitionId());
    if (runtime == null) {
      writer.adErrorResponse(requestId, 0, "流程定义不存在，无法登记后加签");
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

    // 登记落行（同模式追加去重、跨模式拒绝），完成时消费创建
    if (!addSignBehavior.mergeAfterSignRegistration(userTask, assignees, mode, requestId)) {
      writer.adErrorResponse(requestId, 0, "已登记的后加签模式与本次不一致，不允许混用");
      return;
    }
    writer.adResponse(
        CommandApiUserTaskValueLifeCycle.ADD_SIGN_AFTER_RESPONSE,
        requestId,
        new AddSignAfterResponseRecord());
  }

  @Override
  public CommandApiUserTaskValueLifeCycle valueLifeCycle() {
    return CommandApiUserTaskValueLifeCycle.ADD_SIGN_AFTER_REQUEST;
  }
}
