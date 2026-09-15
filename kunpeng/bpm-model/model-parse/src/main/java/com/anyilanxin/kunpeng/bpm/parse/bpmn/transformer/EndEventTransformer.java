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

import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.EndEvent;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.EventDefinition;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.MessageEventDefinition;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.TerminateEventDefinition;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnEndEvent;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnEventType;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.transformation.BpmnTransformContext;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.transformation.ElementTransformer;
import java.util.Collection;

/** 结束事件转换器：推导事件语义（普通/消息/补偿/终止）。 */
public final class EndEventTransformer implements ElementTransformer<EndEvent> {

  /** 返回本转换器处理的模型元素类型。 */
  @Override
  public Class<EndEvent> getType() {
    return EndEvent.class;
  }

  /**
   * 装配结束事件：默认普通语义，存在事件定义时按定义覆写。
   *
   * @param element 结束事件模型元素
   * @param context 转换上下文
   */
  @Override
  public void transform(final EndEvent element, final BpmnTransformContext context) {
    final BpmnEndEvent endEvent =
        context.getCurrentProcess().getElementById(element.getId(), BpmnEndEvent.class);

    endEvent.setEventType(BpmnEventType.NONE);
    final Collection<EventDefinition> eventDefinitions = element.getEventDefinitions();
    if (eventDefinitions == null || eventDefinitions.isEmpty()) {
      return;
    }
    for (final EventDefinition eventDefinition : eventDefinitions) {
      if (eventDefinition instanceof TerminateEventDefinition) {
        endEvent.setTerminateEndEvent(true);
      } else if (eventDefinition instanceof MessageEventDefinition) {
        endEvent.setEventType(BpmnEventType.MESSAGE);
      }
    }
  }
}
