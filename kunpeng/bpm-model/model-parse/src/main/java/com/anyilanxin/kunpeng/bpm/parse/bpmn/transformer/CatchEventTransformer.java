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

import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.Association;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.CatchEvent;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.CompensateEventDefinition;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.ErrorEventDefinition;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.EscalationEventDefinition;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.EventDefinition;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.LinkEventDefinition;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.MessageEventDefinition;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.SignalEventDefinition;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.TimerEventDefinition;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnActivity;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnCatchEventElement;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnCompensation;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnError;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnEscalation;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnEventType;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnLink;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnProcess;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.transformation.BpmnTransformContext;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.transformation.ElementTransformer;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.transformation.TransformHelper;
import java.util.Collection;

/**
 * 捕获事件转换器：按第一个事件定义装配事件载荷（消息/定时/错误/升级/信号/链接/补偿）。
 *
 * <p>定时载荷在此编译为延迟到运行期的定时构建工厂；补偿载荷依据关联线（association）定位补偿处理器。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class CatchEventTransformer implements ElementTransformer<CatchEvent> {

  /** 返回本转换器处理的模型元素类型（CatchEvent 覆盖开始/中间捕获/边界事件）。 */
  @Override
  public Class<CatchEvent> getType() {
    return CatchEvent.class;
  }

  /**
   * 装配第一个事件定义对应的载荷。
   *
   * @param element 捕获事件模型元素
   * @param context 转换上下文
   */
  @Override
  public void transform(final CatchEvent element, final BpmnTransformContext context) {
    final Collection<EventDefinition> eventDefinitions = element.getEventDefinitions();
    if (eventDefinitions == null || eventDefinitions.isEmpty()) {
      return;
    }
    final BpmnProcess process = context.getCurrentProcess();
    final BpmnCatchEventElement executableElement =
        process.getElementById(element.getId(), BpmnCatchEventElement.class);
    final EventDefinition eventDefinition = eventDefinitions.iterator().next();
    if (eventDefinition instanceof final MessageEventDefinition messageDefinition) {
      applyMessageDefinition(context, executableElement, messageDefinition);
    } else if (eventDefinition instanceof final TimerEventDefinition timerDefinition) {
      applyTimerDefinition(context, executableElement, timerDefinition);
    } else if (eventDefinition instanceof final ErrorEventDefinition errorDefinition) {
      applyErrorDefinition(context, executableElement, errorDefinition);
    } else if (eventDefinition instanceof final LinkEventDefinition linkDefinition) {
      applyLinkDefinition(context, executableElement, linkDefinition);
    } else if (eventDefinition instanceof final EscalationEventDefinition escalationDefinition) {
      applyEscalationDefinition(context, executableElement, escalationDefinition);
    } else if (eventDefinition instanceof final SignalEventDefinition signalDefinition) {
      applySignalDefinition(context, executableElement, signalDefinition);
    } else if (eventDefinition instanceof final CompensateEventDefinition compensateDefinition) {
      applyCompensationDefinition(context, element, executableElement, compensateDefinition);
    }
  }

  /** 装配消息载荷。 */
  private void applyMessageDefinition(
      final BpmnTransformContext context,
      final BpmnCatchEventElement executableElement,
      final MessageEventDefinition messageEventDefinition) {
    executableElement.setMessage(context.getMessage(messageEventDefinition.getMessage().getId()));
    executableElement.setEventType(BpmnEventType.MESSAGE);
  }

  /** 装配定时载荷（timeDuration/timeCycle/timeDate 三选一）。 */
  private void applyTimerDefinition(
      final BpmnTransformContext context,
      final BpmnCatchEventElement executableElement,
      final TimerEventDefinition timerEventDefinition) {
    executableElement.setEventType(BpmnEventType.TIMER);
    if (timerEventDefinition.getTimeDuration() != null) {
      final String duration = timerEventDefinition.getTimeDuration().getTextContent();
      executableElement.setTimerProperties(
          TransformHelper.durationTimer(context.parseExpression(duration), duration));
    } else if (timerEventDefinition.getTimeCycle() != null) {
      final String cycle = timerEventDefinition.getTimeCycle().getTextContent();
      executableElement.setTimerProperties(
          TransformHelper.cycleTimer(context.parseExpression(cycle), cycle));
    } else if (timerEventDefinition.getTimeDate() != null) {
      final String timeDate = timerEventDefinition.getTimeDate().getTextContent();
      executableElement.setTimerProperties(
          TransformHelper.dateTimer(context.parseExpression(timeDate), timeDate));
    }
  }

  /** 装配错误载荷：errorRef 缺省时以空错误码兜底，便于捕获事件匹配。 */
  private void applyErrorDefinition(
      final BpmnTransformContext context,
      final BpmnCatchEventElement executableElement,
      final ErrorEventDefinition errorEventDefinition) {
    final var error = errorEventDefinition.getError();
    final BpmnError executableError;
    if (error == null) {
      executableError = new BpmnError("");
      executableError.setErrorCode("");
    } else {
      executableError = context.getError(error.getId());
    }
    executableElement.setError(executableError);
    executableElement.setEventType(BpmnEventType.ERROR);
  }

  /** 装配升级载荷：escalationRef 缺省时以空升级码兜底。 */
  private void applyEscalationDefinition(
      final BpmnTransformContext context,
      final BpmnCatchEventElement executableElement,
      final EscalationEventDefinition escalationEventDefinition) {
    final var escalation = escalationEventDefinition.getEscalation();
    final BpmnEscalation executableEscalation;
    if (escalation == null) {
      executableEscalation = new BpmnEscalation("");
      executableEscalation.setEscalationCode("");
    } else {
      executableEscalation = context.getEscalation(escalation.getId());
    }
    executableElement.setEscalation(executableEscalation);
    executableElement.setEventType(BpmnEventType.ESCALATION);
  }

  /** 装配信号载荷。 */
  private void applySignalDefinition(
      final BpmnTransformContext context,
      final BpmnCatchEventElement executableElement,
      final SignalEventDefinition signalEventDefinition) {
    executableElement.setSignal(context.getSignal(signalEventDefinition.getSignal().getId()));
    executableElement.setEventType(BpmnEventType.SIGNAL);
  }

  /** 装配链接载荷：以链接事件定义名建立索引供链接抛出事件反查。 */
  private void applyLinkDefinition(
      final BpmnTransformContext context,
      final BpmnCatchEventElement executableElement,
      final LinkEventDefinition linkEventDefinition) {
    executableElement.setLink(true);
    executableElement.setEventType(BpmnEventType.LINK);
    final BpmnLink link = new BpmnLink(linkEventDefinition.getId());
    link.setName(linkEventDefinition.getName());
    link.setCatchEvent(executableElement);
    context.addLink(link);
  }

  /** 装配补偿载荷：沿以本事件为源的关联线定位补偿处理器活动。 */
  private void applyCompensationDefinition(
      final BpmnTransformContext context,
      final CatchEvent element,
      final BpmnCatchEventElement executableElement,
      final CompensateEventDefinition eventDefinition) {
    final Collection<Association> associations =
        element.getParentElement().getChildElementsByType(Association.class);
    String compensationHandlerId = null;
    if (associations != null) {
      for (final Association association : associations) {
        if (association.getSource().getId().equals(element.getId())) {
          compensationHandlerId = association.getTarget().getId();
          break;
        }
      }
    }
    if (compensationHandlerId != null) {
      final BpmnActivity compensationHandler =
          context.getCurrentProcess().getElementById(compensationHandlerId, BpmnActivity.class);
      final BpmnCompensation compensation = new BpmnCompensation(eventDefinition.getId());
      compensation.setCompensationHandler(compensationHandler);
      executableElement.setCompensation(compensation);
    }
    executableElement.setEventType(BpmnEventType.COMPENSATION);
  }
}
