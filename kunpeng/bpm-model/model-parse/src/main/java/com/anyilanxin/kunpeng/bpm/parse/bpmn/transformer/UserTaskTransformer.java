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
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengAddSign;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengAssignmentDefinition;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnAddSignUserTask;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnUserTask;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.transformation.BpmnTransformContext;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.transformation.ElementTransformer;
import com.anyilanxin.kunpeng.bpm.parse.exception.BpmnParseException;

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
    final BpmnUserTask userTask =
        context.getCurrentProcess().getElementById(element.getId(), BpmnUserTask.class);
    // 加签许可：声明 kunpeng:addSign 即在解析期派生加签容器元素（id + "-addsign"，ADD_SIGN_USER_TASK 类型），
    // 运行时定义因此是部署产物的确定函数（重启/重放一致），引擎按 definitionKey 必能读到
    final KunpengAddSign addSign = element.getSingleExtensionElement(KunpengAddSign.class);
    if (addSign != null) {
      userTask.setAddSignEnabled(true);
      final Boolean cascade = addSign.getCascade();
      final boolean cascadeEnabled = cascade != null && cascade;
      userTask.setAddSignCascade(cascadeEnabled);
      final Integer maxDepth = addSign.getMaxDepth();
      if (maxDepth != null) {
        // 解析期校验：maxDepth=级联层数上限，必须 >=1；未开启级联时只能为 1（不允许级联即只能一级加签）
        if (maxDepth < 1) {
          throw new BpmnParseException(
              "kunpeng:addSign@maxDepth must be >= 1 (element " + element.getId() + ")");
        }
        if (!cascadeEnabled && maxDepth != 1) {
          throw new BpmnParseException(
              "kunpeng:addSign@maxDepth must be 1 when cascade is disabled (element "
                  + element.getId()
                  + "), no cascade means single-level add sign only");
        }
        userTask.setAddSignMaxDepth(maxDepth);
      }
      context
          .getCurrentProcess()
          .addFlowElement(
              new BpmnAddSignUserTask(element.getId() + BpmnUserTask.ADD_SIGN_DEFINITION_SUFFIX));
    }
    final KunpengAssignmentDefinition assignmentDefinition =
        element.getSingleExtensionElement(KunpengAssignmentDefinition.class);
    if (assignmentDefinition == null) {
      return;
    }
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
