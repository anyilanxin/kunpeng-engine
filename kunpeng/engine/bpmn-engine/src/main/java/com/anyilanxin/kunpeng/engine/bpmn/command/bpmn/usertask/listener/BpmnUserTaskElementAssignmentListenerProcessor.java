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
package com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.usertask.listener;

import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnUserTask;
import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.Behavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl.JobBehavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.usertask.BpmnUserTaskElementListenerProcessor;
import com.anyilanxin.kunpeng.protocol.business.impl.eventlog.BusinessLogRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.usertask.UserTaskRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.usertask.UserTaskListenerType;

/**
 * 用户任务改派监听器处理器。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class BpmnUserTaskElementAssignmentListenerProcessor
    implements BpmnUserTaskElementListenerProcessor {
  private final LogEventWriter writer;
  private final JobBehavior jobBehavior;

  public BpmnUserTaskElementAssignmentListenerProcessor(final LogEventWriter writer) {
    this.writer = writer;
    final Behavior behavior = writer.behavior();
    jobBehavior = behavior.jobBehavior();
  }

  @Override
  public UserTaskListenerType getType() {
    return UserTaskListenerType.ASSIGNMENT;
  }

  @Override
  public void onCreate(
      final BpmnUserTask element, final BusinessLogRecord<UserTaskRecord> logRecord) {}

  @Override
  public void onComplete(
      final BpmnUserTask element, final BusinessLogRecord<UserTaskRecord> logRecord) {}

  @Override
  public void onTerminated(
      final BpmnUserTask element, final BusinessLogRecord<UserTaskRecord> logRecord) {}
}
