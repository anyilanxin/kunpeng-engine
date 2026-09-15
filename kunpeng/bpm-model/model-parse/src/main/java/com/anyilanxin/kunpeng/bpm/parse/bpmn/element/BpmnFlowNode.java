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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 流程节点（活动、事件、网关）的运行时基类。
 *
 * <p>维护入边/出边连接、输入输出变量映射；两侧集合均懒分配，多数节点只有少量连线，未分配时可节省每个节点两个 ArrayList 的开销。
 */
public class BpmnFlowNode extends BpmnFlowElement {
  /** 入边（指向本节点的顺序流），懒分配 */
  private List<BpmnSequenceFlow> incoming;

  /** 出边（从本节点引出的顺序流），懒分配 */
  private List<BpmnSequenceFlow> outgoing;

  /** 输入变量映射表达式（kunpeng:ioMapping/inputs），未声明时为 null */
  private ScriptExpression inputMappings;

  /** 输出变量映射表达式（kunpeng:ioMapping/outputs），未声明时为 null */
  private ScriptExpression outputMappings;

  /**
   * 以元素 id 构造流程节点。
   *
   * @param id 元素唯一标识
   */
  public BpmnFlowNode(final String id) {
    super(id);
  }

  /** 获取入边列表，未连接时返回空列表（只读约定，调用方不得修改）。 */
  public List<BpmnSequenceFlow> getIncoming() {
    return incoming == null ? Collections.emptyList() : incoming;
  }

  /** 获取出边列表，未连接时返回空列表（只读约定，调用方不得修改）。 */
  public List<BpmnSequenceFlow> getOutgoing() {
    return outgoing == null ? Collections.emptyList() : outgoing;
  }

  /** 是否声明了输入变量映射。 */
  public boolean hasInputMappings() {
    return inputMappings != null;
  }

  /** 是否声明了输出变量映射。 */
  public boolean hasOutputMappings() {
    return outputMappings != null;
  }

  /**
   * 注册入边。
   *
   * @param flow 指向本节点的顺序流
   */
  public void addIncoming(final BpmnSequenceFlow flow) {
    if (incoming == null) {
      incoming = new ArrayList<>(2);
    }
    incoming.add(flow);
  }

  /**
   * 注册出边。
   *
   * @param flow 从本节点引出的顺序流
   */
  public void addOutgoing(final BpmnSequenceFlow flow) {
    if (outgoing == null) {
      outgoing = new ArrayList<>(2);
    }
    outgoing.add(flow);
  }

  /** 获取输入变量映射表达式，未声明时返回 null。 */
  public ScriptExpression getInputMappings() {
    return inputMappings;
  }

  /**
   * 设置输入变量映射表达式。
   *
   * @param inputMappings 输入变量映射表达式
   */
  public void setInputMappings(final ScriptExpression inputMappings) {
    this.inputMappings = inputMappings;
  }

  /** 获取输出变量映射表达式，未声明时返回 null。 */
  public ScriptExpression getOutputMappings() {
    return outputMappings;
  }

  /** 清空入边与出边（多实例重组时连线整体迁移到活动体后使用）。 */
  public void clearFlows() {
    incoming = null;
    outgoing = null;
  }

  /**
   * 设置输出变量映射表达式。
   *
   * @param outputMappings 输出变量映射表达式
   */
  public void setOutputMappings(final ScriptExpression outputMappings) {
    this.outputMappings = outputMappings;
  }
}
