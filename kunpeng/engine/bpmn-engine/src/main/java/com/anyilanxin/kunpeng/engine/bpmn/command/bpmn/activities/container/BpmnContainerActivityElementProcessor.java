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
package com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.activities.container;

import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnActivity;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnFlowElement;
import com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.activities.ActivityContent;
import com.anyilanxin.kunpeng.engine.bpmn.command.bpmn.activities.BpmnActivityElementProcessor;

/**
 * BPMN 容器型活动元素处理器接口：子流程类元素的统一抽象。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public interface BpmnContainerActivityElementProcessor<T extends BpmnFlowElement>
    extends BpmnActivityElementProcessor<T> {

  default void onChildActivating(
      final BpmnActivity element,
      final ActivityContent parentContent,
      final ActivityContent childContent) {}

  default void onChildTerminating(
      final BpmnActivity element,
      final ActivityContent parentContent,
      final ActivityContent childContent) {}
}
