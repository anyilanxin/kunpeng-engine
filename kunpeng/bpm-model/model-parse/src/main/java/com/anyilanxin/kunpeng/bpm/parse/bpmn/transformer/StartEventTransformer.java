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

import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.FlowNode;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.Process;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.StartEvent;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnContainer;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnEventType;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnMessage;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnProcess;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnSignal;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnStartEvent;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.transformation.BpmnTransformContext;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.transformation.ElementTransformer;
import com.anyilanxin.kunpeng.bpm.parse.exception.BpmnParseException;
import com.anyilanxin.kunpeng.utils.Either;

/** 开始事件转换器：推导事件语义、登记到所属容器（流程或子流程），流程级消息/信号开始事件提前求值名称。 */
public final class StartEventTransformer implements ElementTransformer<StartEvent> {

  /** 返回本转换器处理的模型元素类型。 */
  @Override
  public Class<StartEvent> getType() {
    return StartEvent.class;
  }

  /**
   * 装配开始事件：事件语义推导、容器登记与流程级名称求值。
   *
   * @param element 开始事件模型元素
   * @param context 转换上下文
   */
  @Override
  public void transform(final StartEvent element, final BpmnTransformContext context) {
    final BpmnProcess process = context.getCurrentProcess();
    final BpmnStartEvent startEvent = process.getElementById(element.getId(), BpmnStartEvent.class);

    startEvent.setInterrupting(element.isInterrupting());
    startEvent.setEventType(BpmnEventType.NONE);
    if (startEvent.isMessage()) {
      startEvent.setEventType(BpmnEventType.MESSAGE);
    } else if (startEvent.isTimer()) {
      startEvent.setEventType(BpmnEventType.TIMER);
    } else if (startEvent.isCompensation()) {
      startEvent.setEventType(BpmnEventType.COMPENSATION);
    }

    if (element.getScope() instanceof FlowNode) {
      final BpmnContainer container =
          process.getElementById(((FlowNode) element.getScope()).getId(), BpmnContainer.class);
      container.addStartEvent(startEvent);
    } else {
      // 流程级开始事件
      process.addStartEvent(startEvent);
    }

    if (startEvent.isMessage() && element.getScope() instanceof Process) {
      evaluateMessageName(startEvent);
    }
    if (startEvent.isSignal() && element.getScope() instanceof Process) {
      evaluateSignalName(startEvent);
    }
  }

  /** 流程级消息开始事件要求消息名在部署期可静态解析。 */
  private void evaluateMessageName(final BpmnStartEvent startEvent) {
    final BpmnMessage message = startEvent.getMessage();
    if (message.getMessageName() == null) {
      final Either<String, String> result = message.getMessageNameExpression().evaluateString();
      if (result.isLeft()) {
        throw new BpmnParseException(
            "Failed to evaluate message name expression of message start event: '"
                + message.getMessageNameExpression()
                + "'");
      }
      message.setMessageName(result.get());
    }
  }

  /** 流程级信号开始事件要求信号名在部署期可静态解析。 */
  private void evaluateSignalName(final BpmnStartEvent startEvent) {
    final BpmnSignal signal = startEvent.getSignal();
    if (signal.getSignalName() == null) {
      final Either<String, String> result = signal.getSignalNameExpression().evaluateString();
      if (result.isLeft()) {
        throw new BpmnParseException(
            "Failed to evaluate signal name expression of signal start event: '"
                + signal.getSignalNameExpression()
                + "'");
      }
      signal.setSignalName(result.get());
    }
  }
}
