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

import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.Activity;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.CompensateEventDefinition;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.EscalationEventDefinition;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.EventDefinition;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.IntermediateThrowEvent;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.LinkEventDefinition;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.MessageEventDefinition;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.SignalEventDefinition;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengPublishMessage;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengTaskDefinition;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnActivity;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnCompensation;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnEventType;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnIntermediateThrowEvent;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnJobProperties;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.element.BpmnMessage;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.transformation.BpmnTransformContext;
import com.anyilanxin.kunpeng.bpm.parse.bpmn.transformation.ElementTransformer;
import java.util.Collection;

/**
 * 中间抛出事件转换器：按第一个事件定义装配抛出语义——无操作、消息（任务型或发布型）、链接、升级、信号与补偿。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class IntermediateThrowEventTransformer
    implements ElementTransformer<IntermediateThrowEvent> {

  /** 返回本转换器处理的模型元素类型。 */
  @Override
  public Class<IntermediateThrowEvent> getType() {
    return IntermediateThrowEvent.class;
  }

  /**
   * 装配中间抛出事件：默认无操作语义，存在事件定义时按定义覆写。
   *
   * @param element 中间抛出事件模型元素
   * @param context 转换上下文
   */
  @Override
  public void transform(final IntermediateThrowEvent element, final BpmnTransformContext context) {
    final BpmnIntermediateThrowEvent throwEvent =
        context
            .getCurrentProcess()
            .getElementById(element.getId(), BpmnIntermediateThrowEvent.class);

    throwEvent.setEventType(BpmnEventType.NONE);
    final Collection<EventDefinition> eventDefinitions = element.getEventDefinitions();
    if (eventDefinitions == null || eventDefinitions.isEmpty()) {
      return;
    }
    final EventDefinition eventDefinition = eventDefinitions.iterator().next();
    if (eventDefinition instanceof final MessageEventDefinition messageDefinition) {
      applyMessageThrowDefinition(element, context, throwEvent, messageDefinition);
    } else if (eventDefinition instanceof final LinkEventDefinition linkDefinition) {
      throwEvent.setLink(context.getLink(linkDefinition.getName()));
      throwEvent.setEventType(BpmnEventType.LINK);
    } else if (eventDefinition instanceof final EscalationEventDefinition escalationDefinition) {
      throwEvent.setEscalation(context.getEscalation(escalationDefinition.getEscalation().getId()));
      throwEvent.setEventType(BpmnEventType.ESCALATION);
    } else if (eventDefinition instanceof final SignalEventDefinition signalDefinition) {
      throwEvent.setSignal(context.getSignal(signalDefinition.getSignal().getId()));
      throwEvent.setEventType(BpmnEventType.SIGNAL);
    } else if (eventDefinition instanceof final CompensateEventDefinition compensateDefinition) {
      applyCompensationDefinition(context, throwEvent, compensateDefinition);
    }
  }

  /** 装配消息抛出：声明任务定义时走任务型，否则装配发布型属性（消息名、关联键、存活时长）。 */
  private void applyMessageThrowDefinition(
      final IntermediateThrowEvent element,
      final BpmnTransformContext context,
      final BpmnIntermediateThrowEvent executableElement,
      final MessageEventDefinition messageEventDefinition) {
    executableElement.setEventType(BpmnEventType.MESSAGE);
    final KunpengTaskDefinition taskDefinition =
        element.getSingleExtensionElement(KunpengTaskDefinition.class);
    if (taskDefinition != null) {
      BpmnJobProperties jobProperties = executableElement.getJobProperties();
      if (jobProperties == null) {
        jobProperties = new BpmnJobProperties();
        executableElement.setJobProperties(jobProperties);
      }
      JobWorkerTaskTransformer.applyTaskDefinition(jobProperties, taskDefinition, context);
      return;
    }
    final BpmnMessage message = context.getMessage(messageEventDefinition.getMessage().getId());
    final KunpengPublishMessage publishMessage =
        messageEventDefinition.getSingleExtensionElement(KunpengPublishMessage.class);
    final BpmnIntermediateThrowEvent.MessagePublishProperties publishProperties =
        new BpmnIntermediateThrowEvent.MessagePublishProperties();
    publishProperties.setMessageNameExpression(message.getMessageNameExpression());
    publishProperties.setMessageName(message.getMessageName());
    publishProperties.setCorrelationKeyExpression(
        context.parseExpression(publishMessage.getCorrelationKey()));
    final String timeToLive = publishMessage.getTimeToLive();
    if (timeToLive != null && !timeToLive.isBlank()) {
      publishProperties.setTimeToLiveExpression(context.parseExpression(timeToLive));
    }
    executableElement.setMessagePublishProperties(publishProperties);
  }

  /** 装配补偿抛出：activityRef 引用的活动作为补偿处理器。 */
  private void applyCompensationDefinition(
      final BpmnTransformContext context,
      final BpmnIntermediateThrowEvent executableElement,
      final CompensateEventDefinition eventDefinition) {
    final BpmnCompensation compensation = new BpmnCompensation(eventDefinition.getId());
    final Activity activityRef = eventDefinition.getActivity();
    if (activityRef != null) {
      compensation.setReferencedCompensationActivity(
          context.getCurrentProcess().getElementById(activityRef.getId(), BpmnActivity.class));
    }
    executableElement.setCompensation(compensation);
    executableElement.setEventType(BpmnEventType.COMPENSATION);
  }
}
