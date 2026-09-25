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
package com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl;

import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnUserTask;
import com.anyilanxin.kunpeng.engine.bpmn.LogEventWriter;
import com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.activities.ActivityContent;
import com.anyilanxin.kunpeng.engine.script.EvaluationResult;
import com.anyilanxin.kunpeng.engine.script.ScriptContext;
import com.anyilanxin.kunpeng.engine.script.ScriptExpression;
import com.anyilanxin.kunpeng.protocol.business.impl.record.command.usertask.UserTaskRecord;
import com.anyilanxin.kunpeng.utils.Either;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 用户任务行为：用户任务的创建、认领与完成语义。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class UserTaskBehavior {
  private static final Logger LOG = LoggerFactory.getLogger(UserTaskBehavior.class);
  private final LogEventWriter writer;
  private final VariableBehavior variableBehavior;

  public UserTaskBehavior(final LogEventWriter writer, final VariableBehavior variableBehavior) {
    this.writer = writer;
    this.variableBehavior = variableBehavior;
  }

  public void createJobIncident() {}

  public void createExecutionIncident() {}

  public UserTaskRecord createNewUserTask(
      final ActivityContent activityContext, final BpmnUserTask element) {
    final UserTaskRecord userTaskRecord = createUserTask(activityContext);
    final ScriptContext scriptContext = variableBehavior.scriptContext(activityContext);

    final ScriptExpression assignee = element.getAssignee();
    if (assignee != null) {
      final Either<String, String> result = assignee.evaluateString(scriptContext);
      if (result.isLeft()) {
        LOG.error(
            "User task assignee evaluation failed for activity {}: {}",
            activityContext.getActivityInstanceId(),
            result.getLeft());
        return null;
      } else {
        userTaskRecord.setAssignee(result.get());
      }
    }

    final ScriptExpression candidateGroups = element.getCandidateGroups();
    if (candidateGroups != null) {
      final Either<String, List<String>> result = candidateGroups.evaluateListString(scriptContext);
      if (result.isLeft()) {
        LOG.error(
            "User task candidateGroups evaluation failed for activity {}: {}",
            activityContext.getActivityInstanceId(),
            result.getLeft());
        return null;
      } else {
        userTaskRecord.setCandidateGroups(result.get());
      }
    }

    final ScriptExpression candidateUsers = element.getCandidateUsers();
    if (candidateUsers != null) {
      final Either<String, List<String>> result = candidateUsers.evaluateListString(scriptContext);
      if (result.isLeft()) {
        LOG.error(
            "User task candidateUsers evaluation failed for activity {}: {}",
            activityContext.getActivityInstanceId(),
            result.getLeft());
        return null;
      } else {
        userTaskRecord.setCandidateUsers(result.get());
      }
    }
    return userTaskRecord;
  }

  private void handleExpression(final EvaluationResult result) {}

  public UserTaskRecord createUserTask(final ActivityContent activityContext) {
    return new UserTaskRecord()
        .setTaskId(writer.nextCurrentSourceKey(activityContext.getProcessInstanceId()))
        .setActivityInstanceId(activityContext.getActivityInstanceId())
        .setProcessInstanceId(activityContext.getProcessInstanceId())
        .setProcessDefinitionId(activityContext.getProcessDefinitionId())
        .setProcessDefinitionKey(activityContext.getProcessDefinitionKeyBuffer())
        .setTaskDefinitionKey(activityContext.getActivityDefinitionKeyBuffer())
        .setTaskDefinitionName(activityContext.getActivityDefinitionNameBuffer());
  }
}
