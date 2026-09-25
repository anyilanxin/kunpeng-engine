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
 * 链接事件的运行时模型：以链接名配对链接抛出与链接捕获事件。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class BpmnLink extends BpmnFlowElement {
  /** 配对的链接捕获事件，未装配为 null */
  private BpmnCatchEventElement catchEvent;

  /**
   * 以链接事件定义 id 构造链接。
   *
   * @param id 链接事件定义 id
   */
  public BpmnLink(final String id) {
    super(id);
  }

  /** 获取配对的链接捕获事件，未装配时返回 null。 */
  public BpmnCatchEventElement getCatchEvent() {
    return catchEvent;
  }

  /**
   * 设置配对的链接捕获事件。
   *
   * @param catchEvent 链接捕获事件
   */
  public void setCatchEvent(final BpmnCatchEventElement catchEvent) {
    this.catchEvent = catchEvent;
  }
}
