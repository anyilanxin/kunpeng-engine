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
import com.anyilanxin.kunpeng.engine.bpmn.command.behavior.impl.IncidentBehavior;
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
import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 用户任务创建中命令处理器。
 *
 * <p>受理人、候选组、候选人由表达式求值得出；求值失败时任务行按创建中落库并挂用户任务 incident，见 {@link #failCreating}。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class BpmnUserTaskCreatingProcessor extends BpmnUserTaskElementAbstractProcessor {
  private static final Logger LOG = LoggerFactory.getLogger(BpmnUserTaskCreatingProcessor.class);

  private final VariableBehavior variableBehavior;
  private final IncidentBehavior incidentBehavior;
  private final LogEventWriter writer;

  public BpmnUserTaskCreatingProcessor(final LogEventWriter writer) {
    super(writer);
    final Behavior behavior = writer.behavior();
    variableBehavior = behavior.variableBehavior();
    incidentBehavior = behavior.incidentBehavior();
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
        failCreating(userTaskRecord, logRecord.getRequestId(), "assignee", result.getLeft());
        return;
      } else {
        userTaskRecord.setAssignee(result.get());
      }
    }

    final ScriptExpression candidateGroups = element.getCandidateGroups();
    if (candidateGroups != null) {
      if (candidateGroups.isStatic()) {
        // 静态表达式的求值结果是整段原文而不是列表，按逗号拆分
        final String[] split = candidateGroups.getSourceText().split(",");
        userTaskRecord.setCandidateGroups(Arrays.asList(split));
      } else {
        final Either<String, List<String>> result =
            candidateGroups.evaluateListString(scriptContext);
        if (result.isLeft()) {
          failCreating(
              userTaskRecord, logRecord.getRequestId(), "candidateGroups", result.getLeft());
          return;
        } else {
          userTaskRecord.setCandidateGroups(result.get());
        }
      }
    }

    final ScriptExpression candidateUsers = element.getCandidateUsers();
    if (candidateUsers != null) {
      if (candidateUsers.isStatic()) {
        final String[] split = candidateUsers.getSourceText().split(",");
        userTaskRecord.setCandidateUsers(Arrays.asList(split));
      } else {
        final Either<String, List<String>> result =
            candidateUsers.evaluateListString(scriptContext);
        if (result.isLeft()) {
          failCreating(
              userTaskRecord, logRecord.getRequestId(), "candidateUsers", result.getLeft());
          return;
        } else {
          userTaskRecord.setCandidateUsers(result.get());
        }
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

  /**
   * 表达式求值失败：任务行按 CREATING 落库并挂用户任务 incident。 停在创建中的任务不可操作（认领、完成等入口只接受 CREATED）；解决 incident 会按
   * CREATING 重新下发本命令，重新求值后继续创建。
   */
  private void failCreating(
      final UserTaskRecord userTaskRecord,
      final long requestId,
      final String property,
      final String failureMessage) {
    LOG.error(
        "User task {} evaluation failed for task {}: {}",
        property,
        userTaskRecord.getTaskId(),
        failureMessage);
    writer.addEvent(
        userTaskRecord.getTaskId(), UserTaskLifeCycle.CREATING, requestId, userTaskRecord);
    incidentBehavior.createUserTaskIncident(
        userTaskRecord,
        UserTaskLifeCycle.CREATING,
        "User task " + property + " expression failed: " + failureMessage,
        requestId);
  }
}
