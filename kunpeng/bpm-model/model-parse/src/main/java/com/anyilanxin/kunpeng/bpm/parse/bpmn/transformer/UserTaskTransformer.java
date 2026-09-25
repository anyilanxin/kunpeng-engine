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
package com.anyilanxin.kunpeng.bpm.parse.bpmn.transformer;

import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.UserTask;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengAssignmentDefinition;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnUserTask;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.transformation.BpmnTransformContext;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.transformation.ElementTransformer;

/**
 * 用户任务转换器：装配分配定义扩展（受理人、候选组、候选人，均为表达式）。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class UserTaskTransformer implements ElementTransformer<UserTask> {

  /** 返回本转换器处理的模型元素类型。 */
  @Override
  public Class<UserTask> getType() {
    return UserTask.class;
  }

  /**
   * 装配分配定义扩展（可选）。
   *
   * @param element 用户任务模型元素
   * @param context 转换上下文
   */
  @Override
  public void transform(final UserTask element, final BpmnTransformContext context) {
    final KunpengAssignmentDefinition assignmentDefinition =
        element.getSingleExtensionElement(KunpengAssignmentDefinition.class);
    if (assignmentDefinition == null) {
      return;
    }
    final BpmnUserTask userTask =
        context.getCurrentProcess().getElementById(element.getId(), BpmnUserTask.class);
    final String assignee = assignmentDefinition.getAssignee();
    if (assignee != null && !assignee.isBlank()) {
      userTask.setAssignee(context.parseExpression(assignee));
    }
    final String candidateGroups = assignmentDefinition.getCandidateGroups();
    if (candidateGroups != null && !candidateGroups.isBlank()) {
      userTask.setCandidateGroups(context.parseExpression(candidateGroups));
    }
    final String candidateUsers = assignmentDefinition.getCandidateUsers();
    if (candidateUsers != null && !candidateUsers.isBlank()) {
      userTask.setCandidateUsers(context.parseExpression(candidateUsers));
    }
  }
}
