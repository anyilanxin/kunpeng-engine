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

import com.anyilanxin.kunpeng.bpm.parse.bpmn.transformation.BpmnTransformer;
import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl.VariableBehavior;
import com.anyilanxin.kunpeng.engine.bpmn.commandapi.usertask.UserTaskApiAbstractProcessor;
import com.anyilanxin.kunpeng.protocol.business.impl.eventlog.BusinessLogRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.activityinstance.ActivityInstanceRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.usertask.UserTaskRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.usertask.complete.UserTaskCompleteRequestRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.usertask.UserTaskLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.usertask.CommandApiUserTaskValueLifeCycle;
import com.anyilanxin.kunpeng.repository.business.modules.activityinstance.ImmutableActivityInstanceRepository;
import com.anyilanxin.kunpeng.repository.business.modules.key.ImmutableKeyGeneratorRepository;
import com.anyilanxin.kunpeng.repository.business.modules.usertask.ImmutableUserTaskRepository;
import java.util.Map;

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

  public UserTaskCompleteProcessor(final LogEventWriter writer) {
    super(writer, writer.getRepository().userTaskRepository());
    this.writer = writer;
    activityInstance = writer.getRepository().instanceRepository();
    keyGenerator = writer.getRepository().keyGeneratorRepository();
    userTask = writer.getRepository().userTaskRepository();
    bpmnTransformer = writer.getBpmnTransformer();
    sourceId = writer.getSourceId();
    variableBehavior = writer.behavior().variableBehavior();
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
