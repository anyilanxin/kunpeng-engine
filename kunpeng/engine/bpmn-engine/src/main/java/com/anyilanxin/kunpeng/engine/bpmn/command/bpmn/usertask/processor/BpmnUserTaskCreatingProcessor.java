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
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.Behavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl.VariableBehavior;
import com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.usertask.BpmnUserTaskElementAbstractProcessor;
import com.anyilanxin.kunpeng.engine.script.ScriptContext;
import com.anyilanxin.kunpeng.engine.script.ScriptExpression;
import com.anyilanxin.kunpeng.protocol.business.impl.eventlog.BusinessLogRecord;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.usertask.UserTaskRecord;
import com.anyilanxin.kunpeng.protocol.business.record.command.usertask.UserTaskLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.record.command.usertask.UserTaskListenerType;
import com.anyilanxin.kunpeng.protocol.business.record.command.usertask.UserTaskState;
import com.anyilanxin.kunpeng.utils.Either;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 用户任务创建中命令处理器。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class BpmnUserTaskCreatingProcessor extends BpmnUserTaskElementAbstractProcessor {
  private static final Logger LOG = LoggerFactory.getLogger(BpmnUserTaskCreatingProcessor.class);

  private final VariableBehavior variableBehavior;
  private final LogEventWriter writer;

  public BpmnUserTaskCreatingProcessor(final LogEventWriter writer) {
    super(writer);
    final Behavior behavior = writer.behavior();
    variableBehavior = behavior.variableBehavior();
    this.writer = writer;
  }

  @Override
  public UserTaskLifeCycle processState() {
    return UserTaskLifeCycle.CREATING;
  }

  @Override
  public void process(
      final BpmnUserTask element, final BusinessLogRecord<UserTaskRecord> logRecord) {
    final UserTaskRecord userTaskRecord = logRecord.getValue();
    final ScriptContext scriptContext = variableBehavior.scriptContext(userTaskRecord);
    final ScriptExpression assignee = element.getAssignee();
    if (assignee != null) {
      final Either<String, String> result = assignee.evaluateString(scriptContext);
      if (result.isLeft()) {
        LOG.error(
            "User task assignee evaluation failed for task {}: {}",
            userTaskRecord.getTaskId(),
            result.getLeft());
        return;
      } else {
        userTaskRecord.setAssignee(result.get());
      }
    }

    final ScriptExpression candidateGroups = element.getCandidateGroups();
    if (candidateGroups != null) {
      final Either<String, List<String>> result = candidateGroups.evaluateListString(scriptContext);
      if (result.isLeft()) {
        LOG.error(
            "User task candidateGroups evaluation failed for task {}: {}",
            userTaskRecord.getTaskId(),
            result.getLeft());
        return;
      } else {
        userTaskRecord.setCandidateGroups(result.get());
      }
    }

    final ScriptExpression candidateUsers = element.getCandidateUsers();
    if (candidateUsers != null) {
      final Either<String, List<String>> result = candidateUsers.evaluateListString(scriptContext);
      if (result.isLeft()) {
        LOG.error(
            "User task candidateUsers evaluation failed for task {}: {}",
            userTaskRecord.getTaskId(),
            result.getLeft());
        return;
      } else {
        userTaskRecord.setCandidateUsers(result.get());
      }
    }

    if (StringUtils.isNotBlank(userTaskRecord.getAssignee())) {
      userTaskRecord.setState(UserTaskState.ACTIVE);
    }
    userTaskRecord.setStartTime(writer.millis());
    writer.addEvent(
        userTaskRecord.getTaskId(),
        UserTaskLifeCycle.CREATING,
        logRecord.getRequestId(),
        userTaskRecord);

    // 触发监听器
    userTaskRecord.setListenerType(UserTaskListenerType.CREATE);
    writer.addCommand(
        userTaskRecord.getTaskId(),
        UserTaskLifeCycle.LISTENER_CREATE,
        logRecord.getRequestId(),
        userTaskRecord);
  }
}
