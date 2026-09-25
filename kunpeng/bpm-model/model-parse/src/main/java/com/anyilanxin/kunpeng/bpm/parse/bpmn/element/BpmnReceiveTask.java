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
 * 接收任务的运行时模型：等待一个消息到达后继续（消息语义固定）。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class BpmnReceiveTask extends BpmnActivity {
  /** 等待的消息载荷，未装配为 null */
  private BpmnMessage message;

  /**
   * 以元素 id 构造接收任务。
   *
   * @param id 元素唯一标识
   */
  public BpmnReceiveTask(final String id) {
    super(id);
  }

  /** 是否为消息等待语义（恒为 true）。 */
  public boolean isMessage() {
    return true;
  }

  /** 获取等待的消息载荷，未装配时返回 null。 */
  public BpmnMessage getMessage() {
    return message;
  }

  /**
   * 设置等待的消息载荷。
   *
   * @param message 消息载荷
   */
  public void setMessage(final BpmnMessage message) {
    this.message = message;
  }
}
