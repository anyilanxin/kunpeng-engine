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
 * 边界事件的运行时模型：挂载到某个活动上，依据 {@link #isInterrupting()} 决定是否中断宿主活动。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class BpmnBoundaryEvent extends BpmnCatchEventElement {

  /**
   * 以元素 id 构造边界事件。
   *
   * @param id 元素唯一标识
   */
  public BpmnBoundaryEvent(final String id) {
    super(id);
  }
}
