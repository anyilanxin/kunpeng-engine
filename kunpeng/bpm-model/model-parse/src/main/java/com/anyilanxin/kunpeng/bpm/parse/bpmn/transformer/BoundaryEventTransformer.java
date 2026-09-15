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
package com.anyilanxin.kunpeng.bpm.parse.bpmn.transformer;

import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.BoundaryEvent;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnActivity;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnBoundaryEvent;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnEventType;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnProcess;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.transformation.BpmnTransformContext;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.transformation.ElementTransformer;

/** 边界事件转换器：推导事件语义并挂载到 attachedToRef 指向的宿主活动。 */
public final class BoundaryEventTransformer implements ElementTransformer<BoundaryEvent> {

  /** 返回本转换器处理的模型元素类型。 */
  @Override
  public Class<BoundaryEvent> getType() {
    return BoundaryEvent.class;
  }

  /**
   * 装配边界事件：事件语义推导、中断标记与宿主挂载。
   *
   * @param event 边界事件模型元素
   * @param context 转换上下文
   */
  @Override
  public void transform(final BoundaryEvent event, final BpmnTransformContext context) {
    final BpmnProcess process = context.getCurrentProcess();
    final BpmnBoundaryEvent element =
        process.getElementById(event.getId(), BpmnBoundaryEvent.class);

    if (element.isMessage()) {
      element.setEventType(BpmnEventType.MESSAGE);
    } else if (element.isTimer()) {
      element.setEventType(BpmnEventType.TIMER);
    } else if (element.isCompensation()) {
      element.setEventType(BpmnEventType.COMPENSATION);
    }
    element.setInterrupting(event.cancelActivity());

    final BpmnActivity attachedToElement =
        process.getElementById(event.getAttachedTo().getId(), BpmnActivity.class);
    attachedToElement.attach(element);
  }
}
