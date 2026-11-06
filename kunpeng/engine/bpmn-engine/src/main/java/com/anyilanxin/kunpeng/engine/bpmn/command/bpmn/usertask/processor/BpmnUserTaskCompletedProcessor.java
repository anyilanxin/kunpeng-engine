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
package com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.usertask.processor;

import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnUserTask;
import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl.AddSignBehavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.usertask.BpmnUserTaskElementAbstractProcessor;
import com.anyilanxin.kunpeng.protocol.business.impl.eventlog.BusinessLogRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.activityinstance.ActivityInstanceRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.usertask.UserTaskRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.activityinstance.ActivityInstanceLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.usertask.AddSignRole;
import com.anyilanxin.kunpeng.protocol.business.record.command.usertask.UserTaskLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.usertask.UserTaskState;

/**
 * 用户任务已完命令处理器。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class BpmnUserTaskCompletedProcessor extends BpmnUserTaskElementAbstractProcessor {

  public BpmnUserTaskCompletedProcessor(final LogEventWriter writer) {
    super(writer);
  }

  @Override
  public UserTaskLifeCycle processState() {
    return UserTaskLifeCycle.COMPLETED;
  }

  @Override
  public void process(
      final BpmnUserTask element, final BusinessLogRecord<UserTaskRecord> logRecord) {
    final UserTaskRecord value = logRecord.getValue();
    value.setState(UserTaskState.COMPLETED);
    value.setLifeCycle(UserTaskLifeCycle.COMPLETED);
    value.setEndTime(writer.millis());
    writer.addEvent(
        value.getTaskId(), UserTaskLifeCycle.COMPLETED, logRecord.getRequestId(), value);

    // 加签任务完成：只反馈组实例计数（结构保证永不触达活动）；带后加签登记则延迟传播——派生子组，锚任务完结计数延迟到子组完结
    if (value.getAddSignRole() == AddSignRole.ADD_SIGN) {
      final AddSignBehavior addSignBehavior = writer.behavior().addSignBehavior();
      if (!value.getAfterSignAssignees().isEmpty()) {
        final ActivityInstanceRecord group =
            activityInstance.getRecord(value.getActivityInstanceId());
        if (group != null) {
          addSignBehavior.onTaskCompletedWithAfterSigns(value, group, logRecord.getRequestId());
        }
        return;
      }
      addSignBehavior.onSignTaskEnded(value, logRecord.getRequestId(), false);
      return;
    }

    final ActivityInstanceRecord record = activityInstance.getRecord(value.getActivityInstanceId());

    // 后加签预登记在途：消费登记派生 AFTER 组（顶级时活动置 AWAITING 扣出边），组完结才放行 E0
    if (!value.getAfterSignAssignees().isEmpty()) {
      writer
          .behavior()
          .addSignBehavior()
          .onTaskCompletedWithAfterSigns(value, record, logRecord.getRequestId());
      return;
    }

    writer.addCommand(
        record.getActivityInstanceId(),
        ActivityInstanceLifeCycle.COMPLETING,
        logRecord.getRequestId(),
        record);
  }
}
