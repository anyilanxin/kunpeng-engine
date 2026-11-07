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
import com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.usertask.BpmnUserTaskElementAbstractProcessor;
import com.anyilanxin.kunpeng.protocol.business.impl.eventlog.BusinessLogRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.activityinstance.ActivityInstanceRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.usertask.UserTaskRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.activityinstance.ActivityInstanceLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.usertask.AddSignRole;
import com.anyilanxin.kunpeng.protocol.business.record.command.usertask.UserTaskLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.usertask.UserTaskState;

/**
 * 用户任务已终止命令处理器。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class BpmnUserTaskTerminatedProcessor extends BpmnUserTaskElementAbstractProcessor {

  public BpmnUserTaskTerminatedProcessor(final LogEventWriter writer) {
    super(writer);
  }

  @Override
  public UserTaskLifeCycle processState() {
    return UserTaskLifeCycle.TERMINATED;
  }

  @Override
  public void process(
      final BpmnUserTask element, final BusinessLogRecord<UserTaskRecord> logRecord) {
    final UserTaskRecord record = logRecord.getValue();
    record.setState(UserTaskState.TERMINATED);
    record.setLifeCycle(UserTaskLifeCycle.TERMINATED);
    record.setEndTime(writer.millis());
    writer.addEvent(
        record.getTaskId(), UserTaskLifeCycle.TERMINATED, logRecord.getRequestId(), record);
    // 加签任务终止：反馈组实例终止单位计数，不推进活动终止（组完结判定归容器处理器）
    if (record.getAddSignRole() == AddSignRole.ADD_SIGN) {
      writer.behavior().addSignBehavior().onSignTaskEnded(record, logRecord.getRequestId(), true);
      return;
    }
    if (record.getParentTaskId() == -1) {
      // 与 onTerminating 的终止分发互为镜像：无在途加签组时任务链推进活动终止（MI 子任务/常态终止），
      // 有在途加签组（或活动行已随终止批收尾删除）时由活动树终止批 batchAfter 统一推进，不重复发
      final ActivityInstanceRecord instanceRecord =
          activityInstance.getRecord(record.getActivityInstanceId());
      if (instanceRecord != null
          && !writer
              .behavior()
              .addSignBehavior()
              .hasInFlightChildGroup(record.getActivityInstanceId())) {
        writer.addCommand(
            instanceRecord.getActivityInstanceId(),
            ActivityInstanceLifeCycle.TERMINATED,
            logRecord.getRequestId(),
            logRecord.getOperationReferenceKey(),
            logRecord.getBatchOperationReferenceKey(),
            instanceRecord);
      }
    }
  }
}
