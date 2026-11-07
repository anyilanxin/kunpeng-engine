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
import com.anyilanxin.kunpeng.protocol.business.record.command.usertask.AddSignMode;
import com.anyilanxin.kunpeng.protocol.business.record.command.usertask.AddSignPhase;
import com.anyilanxin.kunpeng.protocol.business.record.command.usertask.UserTaskLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.usertask.CommandApiUserTaskValueLifeCycle;
import com.anyilanxin.kunpeng.repository.business.modules.activityinstance.ImmutableActivityInstanceRepository;
import com.anyilanxin.kunpeng.repository.business.modules.deployment.bpmnresource.record.ProcessDefinitionRuntime;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

/**
 * 前加签 API 处理器：门户校验（模型许可看原任务定义/已签收/非终态/非自加签）后派生 BEFORE 组实例（级联放行—— 任意层级任务可加签）， 同锚任务在途组时并入既有组不重复派生。
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
      writer.adErrorResponse(requestId, 0, "不允许加签给当前任务办理人本人");
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
      writer.adErrorResponse(requestId, 0, "当前任务所属活动实例不存在，无法加签");
      return;
    }
    final ProcessDefinitionRuntime runtime =
        bpmnResource.getRuntime(userTask.getProcessDefinitionId());
    if (runtime == null) {
      writer.adErrorResponse(requestId, 0, "流程定义不存在，无法加签");
      return;
    }
    // 许可看原任务定义（级联加签的锚任务是加签任务，定义 key 需剥后缀回原元素）；未声明 kunpeng:addSign 即拒绝
    final String originKey =
        userTask.getTaskDefinitionKey().endsWith(BpmnUserTask.ADD_SIGN_DEFINITION_SUFFIX)
            ? userTask
                .getTaskDefinitionKey()
                .substring(
                    0,
                    userTask.getTaskDefinitionKey().length()
                        - BpmnUserTask.ADD_SIGN_DEFINITION_SUFFIX.length())
            : userTask.getTaskDefinitionKey();
    final BpmnUserTask element =
        runtime.executableProcess().getElementById(originKey, BpmnUserTask.class);
    if (!element.isAddSignEnabled()) {
      writer.adErrorResponse(requestId, 0, "任务定义未允许加签（建模需声明 kunpeng:addSign）");
      return;
    }
    final String cascadeReject = addSignBehavior.cascadeRejectReason(userTask, element);
    if (cascadeReject != null) {
      writer.adErrorResponse(requestId, 0, cascadeReject);
      return;
    }

    // 同锚任务在途组：并入既有组（同相位同模式，名单去重追加、游标续跑），不重复派生组实例
    final ActivityInstanceRecord inFlightGroup = addSignBehavior.findInFlightGroup(userTask);
    if (inFlightGroup != null) {
      if (!addSignBehavior.mergeIntoGroup(inFlightGroup, assignees, mode, requestId)) {
        writer.adErrorResponse(requestId, 0, "进行中的加签模式与本次不一致，不允许混用");
        return;
      }
      writer.adResponse(
          CommandApiUserTaskValueLifeCycle.ADD_SIGN_BEFORE_RESPONSE,
          requestId,
          new AddSignBeforeResponseRecord());
      return;
    }

    // 生效：派生 BEFORE 组实例（顶级同步冻结展示态+锚行镜像，载荷/计数落组作用域），建任务归容器处理器
    addSignBehavior.spawnGroup(
        userTask, instanceRecord, assignees, AddSignPhase.BEFORE, mode, requestId);
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
