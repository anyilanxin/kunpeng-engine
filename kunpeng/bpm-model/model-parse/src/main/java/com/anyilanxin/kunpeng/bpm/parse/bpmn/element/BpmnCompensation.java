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

/** 补偿的运行时模型：补偿处理器为补偿边界事件的宿主；补偿抛出事件可经 activityRef 引用其他活动作为处理器。 */
public final class BpmnCompensation extends BpmnFlowElement {
  /** 补偿处理器（多实例活动存在时为其活动体） */
  private BpmnActivity compensationHandler;

  /** 补偿抛出事件经 activityRef 引用的活动，未引用为 null */
  private BpmnActivity referencedCompensationActivity;

  /**
   * 以补偿事件定义 id 构造补偿。
   *
   * @param id 补偿事件定义 id
   */
  public BpmnCompensation(final String id) {
    super(id);
  }

  /** 获取补偿处理器。 */
  public BpmnActivity getCompensationHandler() {
    return compensationHandler;
  }

  /**
   * 设置补偿处理器。
   *
   * @param compensationHandler 补偿处理器
   */
  public void setCompensationHandler(final BpmnActivity compensationHandler) {
    this.compensationHandler = compensationHandler;
  }

  /** 获取引用的补偿活动，未引用时返回 null。 */
  public BpmnActivity getReferencedCompensationActivity() {
    return referencedCompensationActivity;
  }

  /**
   * 设置引用的补偿活动。
   *
   * @param referencedCompensationActivity 引用的补偿活动
   */
  public void setReferencedCompensationActivity(final BpmnActivity referencedCompensationActivity) {
    this.referencedCompensationActivity = referencedCompensationActivity;
  }

  /** 是否引用了补偿活动。 */
  public boolean hasReferenceActivity() {
    return referencedCompensationActivity != null;
  }
}
