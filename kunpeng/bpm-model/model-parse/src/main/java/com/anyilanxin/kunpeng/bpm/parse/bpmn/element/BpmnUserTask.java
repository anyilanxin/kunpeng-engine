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
package com.anyilanxin.kunpeng.bpm.parse.bpmn.element;

import com.anyilanxin.kunpeng.engine.script.ScriptExpression;

/**
 * 用户任务的运行时模型：受理人、候选组与候选人均以表达式承载（扁平字段而非嵌套属性对象，减少一层对象开销）。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class BpmnUserTask extends BpmnActivity {
  /** 受理人表达式，未声明为 null */
  private ScriptExpression assignee;

  /** 候选组表达式，未声明为 null */
  private ScriptExpression candidateGroups;

  /** 候选人表达式，未声明为 null */
  private ScriptExpression candidateUsers;

  /**
   * 以元素 id 构造用户任务。
   *
   * @param id 元素唯一标识
   */
  public BpmnUserTask(final String id) {
    super(id);
  }

  /** 获取受理人表达式，未声明时返回 null。 */
  public ScriptExpression getAssignee() {
    return assignee;
  }

  /**
   * 设置受理人表达式。
   *
   * @param assignee 受理人表达式
   */
  public void setAssignee(final ScriptExpression assignee) {
    this.assignee = assignee;
  }

  /** 获取候选组表达式，未声明时返回 null。 */
  public ScriptExpression getCandidateGroups() {
    return candidateGroups;
  }

  /**
   * 设置候选组表达式。
   *
   * @param candidateGroups 候选组表达式
   */
  public void setCandidateGroups(final ScriptExpression candidateGroups) {
    this.candidateGroups = candidateGroups;
  }

  /** 获取候选人表达式，未声明时返回 null。 */
  public ScriptExpression getCandidateUsers() {
    return candidateUsers;
  }

  /**
   * 设置候选人表达式。
   *
   * @param candidateUsers 候选人表达式
   */
  public void setCandidateUsers(final ScriptExpression candidateUsers) {
    this.candidateUsers = candidateUsers;
  }
}
