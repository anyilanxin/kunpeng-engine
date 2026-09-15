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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 排他网关与包容网关的共同运行时模型：维护默认流并索引带条件的出边，供运行期选择分支。
 *
 * <p>两类网关仅在分支选择策略上不同、数据结构一致，共用本类以减少重复定义；条件出边索引懒分配。
 */
public class BpmnBranchingGateway extends BpmnFlowNode {
  /** 默认流（无条件命中时走），未声明为 null */
  private BpmnSequenceFlow defaultFlow;

  /** 带条件的出边，懒分配 */
  private List<BpmnSequenceFlow> outgoingWithCondition;

  /**
   * 以元素 id 构造分支网关。
   *
   * @param id 元素唯一标识
   */
  public BpmnBranchingGateway(final String id) {
    super(id);
  }

  /** 获取默认流，未声明时返回 null。 */
  public BpmnSequenceFlow getDefaultFlow() {
    return defaultFlow;
  }

  /**
   * 设置默认流。
   *
   * @param defaultFlow 默认流
   */
  public void setDefaultFlow(final BpmnSequenceFlow defaultFlow) {
    this.defaultFlow = defaultFlow;
  }

  /** 获取带条件的出边列表，不存在时返回空列表。 */
  public List<BpmnSequenceFlow> getOutgoingWithCondition() {
    return outgoingWithCondition == null ? Collections.emptyList() : outgoingWithCondition;
  }

  /**
   * 注册出边：条件流同时进入条件出边索引。
   *
   * @param flow 出边
   */
  @Override
  public void addOutgoing(final BpmnSequenceFlow flow) {
    super.addOutgoing(flow);
    if (flow.getCondition() != null) {
      if (outgoingWithCondition == null) {
        outgoingWithCondition = new ArrayList<>(2);
      }
      outgoingWithCondition.add(flow);
    }
  }
}
