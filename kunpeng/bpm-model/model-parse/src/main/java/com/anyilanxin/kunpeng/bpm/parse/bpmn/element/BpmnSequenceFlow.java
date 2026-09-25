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
 * 顺序流的运行时模型：连接源节点与目标节点，可携带条件表达式，take 时机可挂执行监听器。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class BpmnSequenceFlow extends BpmnFlowElement {
  /** 源节点 */
  private BpmnFlowNode source;

  /** 目标节点 */
  private BpmnFlowNode target;

  /** 条件表达式，未声明时为 null */
  private ScriptExpression condition;

  /**
   * 以元素 id 构造顺序流。
   *
   * @param id 元素唯一标识
   */
  public BpmnSequenceFlow(final String id) {
    super(id);
  }

  /** 获取源节点。 */
  public BpmnFlowNode getSource() {
    return source;
  }

  /**
   * 设置源节点。
   *
   * @param source 源节点
   */
  public void setSource(final BpmnFlowNode source) {
    this.source = source;
  }

  /** 获取目标节点。 */
  public BpmnFlowNode getTarget() {
    return target;
  }

  /**
   * 设置目标节点。
   *
   * @param target 目标节点
   */
  public void setTarget(final BpmnFlowNode target) {
    this.target = target;
  }

  /** 获取条件表达式，未声明时返回 null。 */
  public ScriptExpression getCondition() {
    return condition;
  }

  /** 是否声明了条件表达式。 */
  public boolean isConditional() {
    return condition != null;
  }

  /**
   * 设置条件表达式。
   *
   * @param condition 条件表达式
   */
  public void setCondition(final ScriptExpression condition) {
    this.condition = condition;
  }
}
