/*
 * Copyright © 2017 camunda services GmbH (info@camunda.com)
 * Copyright © 2026 anyilanxin zxh (anyilanxin@aliyun.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.anyilanxin.kunpeng.bpm.model.bpmn.builder;

import com.anyilanxin.kunpeng.bpm.model.bpmn.BpmnModelInstance;
import com.anyilanxin.kunpeng.bpm.model.bpmn.builder.kunpeng.MessageBuilder;
import com.anyilanxin.kunpeng.bpm.model.bpmn.builder.kunpeng.SignalBuilder;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.*;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengInput;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengIoMapping;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengOutput;
import java.time.Duration;
import java.util.function.Consumer;

/**
 * @author Sebastian Menski
 */
public abstract class AbstractCatchEventBuilder<
        B extends AbstractCatchEventBuilder<B, E>, E extends CatchEvent>
    extends AbstractEventBuilder<B, E> implements KunpengVariablesMappingBuilder<B> {

  protected AbstractCatchEventBuilder(
      final BpmnModelInstance modelInstance, final E element, final Class<?> selfType) {
    super(modelInstance, element, selfType);
  }

  /**
   * Sets the event to be parallel multiple
   *
   * @return the builder object
   */
  public B parallelMultiple() {
    element.isParallelMultiple();
    return myself;
  }

  /**
   * Sets an event definition for the given message name. If already a message with this name exists
   * it will be used, otherwise a new message is created.
   *
   * @param messageName the name of the message
   * @return the builder object
   */
  public B message(final String messageName) {
    final MessageEventDefinition messageEventDefinition = createMessageEventDefinition(messageName);
    element.getEventDefinitions().add(messageEventDefinition);

    return myself;
  }

  public B message(final Consumer<MessageBuilder> messageBuilderConsumer) {
    final MessageEventDefinition messageEventDefinition =
        createInstance(MessageEventDefinition.class);
    element.getEventDefinitions().add(messageEventDefinition);

    final Message message = createMessage();
    final MessageBuilder builder = new MessageBuilder(modelInstance, message);

    messageBuilderConsumer.accept(builder);

    messageEventDefinition.setMessage(message);

    return myself;
  }

  public MessageEventDefinitionBuilder messageEventDefinition() {
    final MessageEventDefinition eventDefinition = createEmptyMessageEventDefinition();
    element.getEventDefinitions().add(eventDefinition);
    return new MessageEventDefinitionBuilder(modelInstance, eventDefinition);
  }

  /**
   * Sets an event definition for the given signal name. If already a signal with this name exists
   * it will be used, otherwise a new signal is created.
   *
   * @param signalName the name of the signal
   * @return the builder object
   */
  public B signal(final String signalName) {
    final SignalEventDefinition signalEventDefinition = createSignalEventDefinition(signalName);
    element.getEventDefinitions().add(signalEventDefinition);

    return myself;
  }

  public B signal(final Consumer<SignalBuilder> signalBuilderConsumer) {
    final SignalEventDefinition signalEventDefinition = createInstance(SignalEventDefinition.class);
    element.getEventDefinitions().add(signalEventDefinition);

    final Signal signal = createSignal();
    final SignalBuilder builder = new SignalBuilder(modelInstance, signal);

    signalBuilderConsumer.accept(builder);

    signalEventDefinition.setSignal(signal);

    return myself;
  }

  public SignalEventDefinitionBuilder signalEventDefinition() {
    final SignalEventDefinition eventDefinition = createEmptySignalEventDefinition();
    element.getEventDefinitions().add(eventDefinition);
    return new SignalEventDefinitionBuilder(modelInstance, eventDefinition);
  }

  public B timerWithDateExpression(final String timerDate) {
    return timerWithDate(asKunpengExpression(timerDate));
  }

  /**
   * Sets an event definition for the timer with a time date.
   *
   * @param timerDate the time date of the timer
   * @return the builder object
   */
  public B timerWithDate(final String timerDate) {
    final TimeDate timeDate = createInstance(TimeDate.class);
    timeDate.setTextContent(timerDate);

    final TimerEventDefinition timerEventDefinition = createInstance(TimerEventDefinition.class);
    timerEventDefinition.setTimeDate(timeDate);

    element.getEventDefinitions().add(timerEventDefinition);

    return myself;
  }

  /**
   * Sets an event definition for the timer with a time duration.
   *
   * @param timerDuration the duration of the timer (as feel expression, without the '=' prefix)
   * @return the builder object
   */
  public B timerWithDurationExpression(final String timerDuration) {
    return timerWithDuration(asKunpengExpression(timerDuration));
  }

  /**
   * Sets an event definition for the timer with a time duration.
   *
   * @param timerDuration the time duration of the timer
   * @return the builder object
   */
  public B timerWithDuration(final String timerDuration) {
    final TimeDuration timeDuration = createInstance(TimeDuration.class);
    timeDuration.setTextContent(timerDuration);

    final TimerEventDefinition timerEventDefinition = createInstance(TimerEventDefinition.class);
    timerEventDefinition.setTimeDuration(timeDuration);

    element.getEventDefinitions().add(timerEventDefinition);

    return myself;
  }

  /**
   * Sets an event definition for the timer with a time duration.
   *
   * @param timerDuration the time duration of the timer
   * @return the builder object
   */
  public B timerWithDuration(final Duration timerDuration) {
    return timerWithDuration(timerDuration.toString());
  }

  /**
   * Sets an event definition for the timer with a time cycle.
   *
   * @param timerCycle the time cycle of the timer (as feel expression, without the '=' prefix)
   * @return the builder object
   */
  public B timerWithCycleExpression(final String timerCycle) {
    return timerWithCycle(asKunpengExpression(timerCycle));
  }

  /**
   * Sets an event definition for the timer with a time cycle.
   *
   * @param timerCycle the time cycle of the timer
   * @return the builder object
   */
  public B timerWithCycle(final String timerCycle) {
    final TimeCycle timeCycle = createInstance(TimeCycle.class);
    timeCycle.setTextContent(timerCycle);

    final TimerEventDefinition timerEventDefinition = createInstance(TimerEventDefinition.class);
    timerEventDefinition.setTimeCycle(timeCycle);

    element.getEventDefinitions().add(timerEventDefinition);

    return myself;
  }

  public CompensateEventDefinitionBuilder compensateEventDefinition() {
    return compensateEventDefinition(null);
  }

  public CompensateEventDefinitionBuilder compensateEventDefinition(final String id) {
    final CompensateEventDefinition eventDefinition =
        createInstance(CompensateEventDefinition.class);
    if (id != null) {
      eventDefinition.setId(id);
    }

    element.getEventDefinitions().add(eventDefinition);
    return new CompensateEventDefinitionBuilder(modelInstance, eventDefinition);
  }

  public ConditionalEventDefinitionBuilder conditionalEventDefinition() {
    return conditionalEventDefinition(null);
  }

  public ConditionalEventDefinitionBuilder conditionalEventDefinition(final String id) {
    final ConditionalEventDefinition eventDefinition =
        createInstance(ConditionalEventDefinition.class);
    if (id != null) {
      eventDefinition.setId(id);
    }

    element.getEventDefinitions().add(eventDefinition);
    return new ConditionalEventDefinitionBuilder(modelInstance, eventDefinition);
  }

  @Override
  public B condition(final String condition) {
    conditionalEventDefinition().condition(condition);
    return myself;
  }

  /**
   * Sets a link event definition for the given link name.
   *
   * @param linkName the name of the link
   * @return the builder object
   */
  public B link(final String linkName) {
    linkEventDefinition().name(linkName);
    return myself;
  }

  public LinkEventDefinitionBuilder linkEventDefinition() {
    return linkEventDefinition(null);
  }

  public LinkEventDefinitionBuilder linkEventDefinition(final String id) {
    final LinkEventDefinition eventDefinition = createInstance(LinkEventDefinition.class);
    if (id != null) {
      eventDefinition.setId(id);
    }

    element.getEventDefinitions().add(eventDefinition);
    return new LinkEventDefinitionBuilder(modelInstance, eventDefinition);
  }

  @Override
  public B kunpengInputExpression(final String sourceExpression, final String target) {
    final String expression = asKunpengExpression(sourceExpression);
    return kunpengInput(expression, target);
  }

  @Override
  public B kunpengOutputExpression(final String sourceExpression, final String target) {
    final String expression = asKunpengExpression(sourceExpression);
    return kunpengOutput(expression, target);
  }

  @Override
  public B kunpengInput(final String source, final String target) {
    final KunpengIoMapping ioMapping = getCreateSingleExtensionElement(KunpengIoMapping.class);
    final KunpengInput input = createChild(ioMapping, KunpengInput.class);
    input.setSource(source);
    input.setTarget(target);

    return myself;
  }

  @Override
  public B kunpengOutput(final String source, final String target) {
    final KunpengIoMapping ioMapping = getCreateSingleExtensionElement(KunpengIoMapping.class);
    final KunpengOutput input = createChild(ioMapping, KunpengOutput.class);
    input.setSource(source);
    input.setTarget(target);

    return myself;
  }
}
