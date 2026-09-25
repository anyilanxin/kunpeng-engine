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
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.usertask.cancel.UserTaskCancelRequestRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.commandapi.usertask.cancel.UserTaskCancelResponseRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.activityinstance.ActivityInstanceLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.usertask.UserTaskLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.usertask.CommandApiUserTaskValueLifeCycle;
import com.anyilanxin.kunpeng.repository.business.modules.activityinstance.ImmutableActivityInstanceRepository;
import com.anyilanxin.kunpeng.repository.business.modules.key.ImmutableKeyGeneratorRepository;

/**
 * 用户任务取消 API 处理器。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class UserTaskCancelProcessor
    extends UserTaskApiAbstractProcessor<UserTaskCancelRequestRecord> {
  final LogEventWriter writer;
  private final BpmnTransformer bpmnTransformer;
  private final ImmutableKeyGeneratorRepository keyGenerator;
  private final ImmutableActivityInstanceRepository activityInstance;
  private final int sourceId;
  private final VariableBehavior variableBehavior;
  private final UserTaskCancelResponseRecord response = new UserTaskCancelResponseRecord();

  public UserTaskCancelProcessor(final LogEventWriter writer) {
    super(writer, writer.getRepository().userTaskRepository());
    this.writer = writer;
    activityInstance = writer.getRepository().instanceRepository();
    keyGenerator = writer.getRepository().keyGeneratorRepository();
    bpmnTransformer = writer.getBpmnTransformer();
    sourceId = writer.getSourceId();
    variableBehavior = writer.behavior().variableBehavior();
  }

  @Override
  public void innerProcessRecord(
      final BusinessLogRecord<UserTaskCancelRequestRecord> record, final UserTaskRecord userTask) {
    if (userTask.getLifeCycle() != UserTaskLifeCycle.CREATED) {
      writer.adErrorResponse(
          record.getRequestId(), 0, "当前任务状态不允许此操作:" + userTask.getState().name());
      return;
    }

    final ActivityInstanceRecord instanceRecord =
        activityInstance.getRecord(userTask.getActivityInstanceId());
    if (instanceRecord != null) {
      writer.addCommand(
          ActivityInstanceLifeCycle.TERMINATING, record.getRequestId(), instanceRecord);
    } else {
      writer.adErrorResponse(record.getRequestId(), 0, "当前用户活动实力不存在，无法操作取消");
      return;
    }

    response.reset();
    response.setTaskId(userTask.getTaskId());
    writer.adResponse(
        CommandApiUserTaskValueLifeCycle.CANCEL_RESPONSE, record.getRequestId(), response);
  }

  @Override
  public CommandApiUserTaskValueLifeCycle valueLifeCycle() {
    return CommandApiUserTaskValueLifeCycle.CANCEL_REQUEST;
  }
}
