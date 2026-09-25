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
package com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.activities;

import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnElementType;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnFlowElement;

/**
 * 活动元素处理器接口。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public interface BpmnActivityElementProcessor<T extends BpmnFlowElement> {

  BpmnElementType getElementType();

  Class<T> getType();

  default void onTaking(final T element, final ActivityContent activityContext) {}

  default void onTaken(final T element, final ActivityContent activityContext) {}

  /** 元素激活 1. 处理输入 2. 创建激活中事件 3. 触发监听器通知 4. 监听器完成后触发激活完成后续处理命令 */
  default void onActivating(final T element, final ActivityContent activityContext) {}

  default void onActivatingAfter(final T element, final ActivityContent activityContext) {}

  /** 元素激活完成---激活完成 1. 完成激活 2. 后续处理命令，比如是否完成，用户任务等是否创建，是否出发事件 */
  default void onActivated(final T element, final ActivityContent activityContext) {}

  /** 元素完成 1.完成激活事件 2.触发完成监听器 */
  default void onCompleting(final T element, final ActivityContent activityContext) {}

  default void onCompletingAfter(final T element, final ActivityContent activityContext) {}

  /** 元素完成成功---完成后续处理 1. 完成后续处理 */
  default void onCompleted(final T element, final ActivityContent activityContext) {}

  /** 元素终止 */
  default void onTerminating(final T element, final ActivityContent activityContext) {}

  default void onTerminatingAfter(final T element, final ActivityContent activityContext) {}

  /** 元素终止完成 */
  default void onTerminated(final T element, final ActivityContent activityContext) {}
}
