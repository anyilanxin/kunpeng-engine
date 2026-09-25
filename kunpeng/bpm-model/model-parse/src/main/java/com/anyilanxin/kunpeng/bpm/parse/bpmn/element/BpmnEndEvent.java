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

/**
 * 结束事件的运行时模型：可为普通结束、终止结束（terminateEndEvent）或补偿结束；声明任务定义时也可作为任务型元素抛出消息。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class BpmnEndEvent extends BpmnFlowNode {
  /** 终止结束事件标记 */
  private boolean terminateEndEvent;

  /** 任务属性（作为任务型结束事件时），未声明为 null */
  private BpmnJobProperties jobProperties;

  /**
   * 以元素 id 构造结束事件。
   *
   * @param id 元素唯一标识
   */
  public BpmnEndEvent(final String id) {
    super(id);
  }

  /** 是否为普通结束事件（非终止、非补偿）。 */
  public boolean isNoneEndEvent() {
    return !isTerminateEndEvent() && !isCompensationEvent();
  }

  /** 是否为终止结束事件。 */
  public boolean isTerminateEndEvent() {
    return terminateEndEvent;
  }

  /** 是否为补偿结束事件。 */
  public boolean isCompensationEvent() {
    return getEventType() == BpmnEventType.COMPENSATION;
  }

  /**
   * 标记为终止结束事件。
   *
   * @param terminateEndEvent 是否终止结束
   */
  public void setTerminateEndEvent(final boolean terminateEndEvent) {
    this.terminateEndEvent = terminateEndEvent;
  }

  /** 获取任务属性，未声明时返回 null。 */
  public BpmnJobProperties getJobProperties() {
    return jobProperties;
  }

  /**
   * 设置任务属性。
   *
   * @param jobProperties 任务属性
   */
  public void setJobProperties(final BpmnJobProperties jobProperties) {
    this.jobProperties = jobProperties;
  }
}
