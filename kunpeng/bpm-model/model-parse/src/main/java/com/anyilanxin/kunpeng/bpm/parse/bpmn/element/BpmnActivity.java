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
 * 活动（任务类元素）的运行时基类。
 *
 * <p>可挂载边界事件与事件子流程；{@link #getEvents()} 的顺序定义事件触发优先级：事件子流程的开始事件在前、边界事件在后。三个集合均为懒分配。
 */
public class BpmnActivity extends BpmnFlowNode {
  /** 挂载的边界事件，懒分配 */
  private List<BpmnBoundaryEvent> boundaryEvents;

  /** 挂载的事件子流程，懒分配 */
  private List<BpmnContainer> eventSubprocesses;

  /** 事件触发源（含事件子流程开始事件与边界事件），懒分配，顺序即优先级 */
  private List<BpmnCatchEventElement> catchEvents;

  /** 会中断活动的事件元素 id，懒分配 */
  private List<String> interruptingIds;

  /**
   * 以元素 id 构造活动。
   *
   * @param id 元素唯一标识
   */
  public BpmnActivity(final String id) {
    super(id);
  }

  /**
   * 挂载边界事件：加入事件触发源列表，中断型事件同时登记到中断列表。
   *
   * @param boundaryEvent 边界事件
   */
  public void attach(final BpmnBoundaryEvent boundaryEvent) {
    if (boundaryEvents == null) {
      boundaryEvents = new ArrayList<>(2);
      catchEvents = new ArrayList<>(2);
    }
    boundaryEvents.add(boundaryEvent);
    catchEvents.add(boundaryEvent);
    if (boundaryEvent.isInterrupting()) {
      addInterruptingId(boundaryEvent.getId());
    }
  }

  /**
   * 挂载事件子流程：其开始事件置于触发源列表首位（优先于边界事件）。
   *
   * @param eventSubprocess 事件子流程容器
   */
  public void attach(final BpmnContainer eventSubprocess) {
    final BpmnStartEvent startEvent = eventSubprocess.getStartEvents().get(0);
    if (catchEvents == null) {
      catchEvents = new ArrayList<>(2);
    }
    // 事件子流程开始事件置于列表首位，优先于边界事件触发
    catchEvents.add(0, startEvent);
    if (eventSubprocesses == null) {
      eventSubprocesses = new ArrayList<>(2);
    }
    eventSubprocesses.add(eventSubprocess);
    if (startEvent.isInterrupting()) {
      addInterruptingId(startEvent.getId());
    }
  }

  /** 添加中断型事件元素 id（懒分配）。 */
  private void addInterruptingId(final String id) {
    if (interruptingIds == null) {
      interruptingIds = new ArrayList<>(2);
    }
    interruptingIds.add(id);
  }

  /** 获取事件触发源列表（顺序即优先级），未挂载时返回空列表。 */
  public List<BpmnCatchEventElement> getEvents() {
    return catchEvents == null ? Collections.emptyList() : catchEvents;
  }

  /** 获取挂载的边界事件列表，未挂载时返回空列表。 */
  public List<BpmnBoundaryEvent> getBoundaryEvents() {
    return boundaryEvents == null ? Collections.emptyList() : boundaryEvents;
  }

  /** 获取挂载的事件子流程列表，未挂载时返回空列表。 */
  public List<BpmnContainer> getEventSubprocesses() {
    return eventSubprocesses == null ? Collections.emptyList() : eventSubprocesses;
  }

  /** 获取会中断活动的事件元素 id 集合，不存在时返回空集合。 */
  public List<String> getInterruptingElementIds() {
    return interruptingIds == null ? Collections.emptyList() : interruptingIds;
  }

  /** 是否存在挂载的事件（边界事件或事件子流程）。 */
  public boolean hasAttachedEvents() {
    return catchEvents != null;
  }

  /**
   * 清空挂载的事件索引（触发源、边界事件、中断 id 列表）；事件子流程列表保留原引用。
   *
   * <p>多实例重组时使用：边界事件改挂到多实例活动体上，内部活动的事件索引随之清空。
   */
  public void clearAttachedEvents() {
    catchEvents = null;
    boundaryEvents = null;
    interruptingIds = null;
  }
}
