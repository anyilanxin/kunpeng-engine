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

/** 事件网关的运行时模型：收集后继的捕获事件，任一事件先触发则取消其余分支。 */
public class BpmnEventBasedGateway extends BpmnFlowNode {
  /** 后继捕获事件列表，未装配为 null */
  private List<BpmnCatchEventElement> events;

  /**
   * 以元素 id 构造事件网关。
   *
   * @param id 元素唯一标识
   */
  public BpmnEventBasedGateway(final String id) {
    super(id);
  }

  /** 获取后继捕获事件列表，未装配时返回空列表。 */
  public List<BpmnCatchEventElement> getEvents() {
    return events == null ? Collections.emptyList() : events;
  }

  /** 获取后继捕获事件 id 集合（与事件列表同序）。 */
  public List<String> getEventIds() {
    if (events == null) {
      return Collections.emptyList();
    }
    final List<String> ids = new ArrayList<>(events.size());
    for (final BpmnCatchEventElement event : events) {
      ids.add(event.getId());
    }
    return ids;
  }

  /**
   * 设置后继捕获事件列表。
   *
   * @param events 后继捕获事件列表
   */
  public void setEvents(final List<BpmnCatchEventElement> events) {
    this.events = events;
  }
}
