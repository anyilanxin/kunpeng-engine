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
  /** 加签虚拟活动定义 key 后缀：原定义 key + 后缀（解析期合成、引擎建加签任务时对齐） */
  public static final String ADD_SIGN_DEFINITION_SUFFIX = "-addsign";

  /** 受理人表达式，未声明为 null */
  private ScriptExpression assignee;

  /** 候选组表达式，未声明为 null */
  private ScriptExpression candidateGroups;

  /** 候选人表达式，未声明为 null */
  private ScriptExpression candidateUsers;

  /** 是否允许加签（模型期 kunpeng:addSign 声明；仅原任务有意义），缺省 false */
  private boolean addSignEnabled;

  /** 是否为加签虚拟节点（解析期由原任务派生的合成元素），缺省 false */
  private boolean addSignVirtual;

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

  /** 是否允许加签（模型期 kunpeng:addSign 声明；仅原任务有意义）。 */
  public boolean isAddSignEnabled() {
    return addSignEnabled;
  }

  public void setAddSignEnabled(final boolean addSignEnabled) {
    this.addSignEnabled = addSignEnabled;
  }

  /** 是否为加签虚拟节点（解析期由原任务派生的 `id + "-addsign"` 合成元素）。 */
  public boolean isAddSignVirtual() {
    return addSignVirtual;
  }

  public void setAddSignVirtual(final boolean addSignVirtual) {
    this.addSignVirtual = addSignVirtual;
  }
}
