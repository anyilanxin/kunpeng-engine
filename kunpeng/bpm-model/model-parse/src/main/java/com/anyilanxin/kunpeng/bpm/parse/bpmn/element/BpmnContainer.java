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
 * 流程元素容器（子流程与流程）的运行时基类：收集容器内的开始事件并提供判定方法。
 *
 * <p>容器自身的全部子元素统一登记在流程级注册表中（见 {@link BpmnProcess#getElementById(String)}），容器内不再维护重复索引。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class BpmnContainer extends BpmnActivity {
  /** 容器内开始事件列表，懒分配 */
  private List<BpmnStartEvent> startEvents;

  /**
   * 以元素 id 构造容器。
   *
   * @param id 元素唯一标识
   */
  public BpmnContainer(final String id) {
    super(id);
  }

  /**
   * 登记容器内开始事件。
   *
   * @param startEvent 开始事件
   */
  public void addStartEvent(final BpmnStartEvent startEvent) {
    if (startEvents == null) {
      startEvents = new ArrayList<>(2);
    }
    startEvents.add(startEvent);
  }

  /** 获取容器内开始事件列表，不存在时返回空列表。 */
  public List<BpmnStartEvent> getStartEvents() {
    return startEvents == null ? Collections.emptyList() : startEvents;
  }

  /** 是否存在无事件定义的普通开始事件。 */
  public boolean hasNoneStartEvent() {
    return findNoneStartEvent() != null;
  }

  /** 是否存在消息开始事件。 */
  public boolean hasMessageStartEvent() {
    return hasStartEventOf(BpmnEventType.MESSAGE);
  }

  /** 是否存在定时开始事件。 */
  public boolean hasTimerStartEvent() {
    return hasStartEventOf(BpmnEventType.TIMER);
  }

  /** 获取第一个无事件定义的普通开始事件，不存在时返回 null。 */
  public BpmnStartEvent getNoneStartEvent() {
    return findNoneStartEvent();
  }

  /** 查找普通开始事件。 */
  private BpmnStartEvent findNoneStartEvent() {
    if (startEvents == null) {
      return null;
    }
    for (final BpmnStartEvent startEvent : startEvents) {
      if (startEvent.isNone()) {
        return startEvent;
      }
    }
    return null;
  }

  /** 判定是否存在指定事件语义的开始事件。 */
  private boolean hasStartEventOf(final BpmnEventType eventType) {
    if (startEvents == null) {
      return false;
    }
    for (final BpmnStartEvent startEvent : startEvents) {
      if (startEvent.getEventType() == eventType) {
        return true;
      }
    }
    return false;
  }
}
