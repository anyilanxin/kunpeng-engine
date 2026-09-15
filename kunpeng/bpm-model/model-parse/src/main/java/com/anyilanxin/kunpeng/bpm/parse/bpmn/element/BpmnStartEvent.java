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

/** 开始事件的运行时模型：记录其所属事件子流程（若为事件子流程的开始事件）。 */
public class BpmnStartEvent extends BpmnCatchEventElement {
  /** 所属事件子流程的 id，非事件子流程开始事件为 null */
  private String eventSubProcessId;

  /**
   * 以元素 id 构造开始事件。
   *
   * @param id 元素唯一标识
   */
  public BpmnStartEvent(final String id) {
    super(id);
  }

  /** 获取所属事件子流程 id，非事件子流程开始事件返回 null。 */
  public String getEventSubProcessId() {
    return eventSubProcessId;
  }

  /**
   * 设置所属事件子流程 id。
   *
   * @param eventSubProcessId 事件子流程 id
   */
  public void setEventSubProcessId(final String eventSubProcessId) {
    this.eventSubProcessId = eventSubProcessId;
  }
}
