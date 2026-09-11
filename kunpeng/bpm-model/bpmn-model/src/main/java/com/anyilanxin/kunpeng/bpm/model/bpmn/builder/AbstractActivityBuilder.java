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
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.Activity;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.BoundaryEvent;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.MultiInstanceLoopCharacteristics;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.bpmndi.BpmnShape;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.dc.Bounds;
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Consumer;

/**
 * @author Sebastian Menski
 */
public abstract class AbstractActivityBuilder<
        B extends AbstractActivityBuilder<B, E>, E extends Activity>
    extends AbstractFlowNodeBuilder<B, E>
    implements KunpengVariablesMappingBuilder<B>, KunpengExecutionListenersBuilder<B> {

  private final KunpengVariablesMappingBuilder<B> variablesMappingBuilder;
  private final KunpengExecutionListenersBuilder<B> kunpengExecutionListenersBuilder;

  protected AbstractActivityBuilder(
      final BpmnModelInstance modelInstance, final E element, final Class<?> selfType) {
    super(modelInstance, element, selfType);
    variablesMappingBuilder = new KunpengVariableMappingBuilderImpl<>(myself);
    kunpengExecutionListenersBuilder = new KunpengExecutionListenersBuilderImpl<>(myself);
  }

  public BoundaryEventBuilder boundaryEvent() {
    return boundaryEvent(null);
  }

  public BoundaryEventBuilder boundaryEvent(final String id) {
    final BoundaryEvent boundaryEvent = createSibling(BoundaryEvent.class, id);
    boundaryEvent.setAttachedTo(element);

    final BpmnShape boundaryEventBpmnShape = createBpmnShape(boundaryEvent);
    setBoundaryEventCoordinates(boundaryEventBpmnShape);

    return boundaryEvent.builder();
  }

  public BoundaryEventBuilder boundaryEvent(
      final String id, final Consumer<BoundaryEventBuilder> consumer) {
    final BoundaryEventBuilder builder = boundaryEvent(id);
    consumer.accept(builder);
    return builder;
  }

  public MultiInstanceLoopCharacteristicsBuilder multiInstance() {
    final MultiInstanceLoopCharacteristics miCharacteristics =
        createChild(MultiInstanceLoopCharacteristics.class);

    return miCharacteristics.builder();
  }

  public B multiInstance(final Consumer<MultiInstanceLoopCharacteristicsBuilder> consumer) {
    final MultiInstanceLoopCharacteristicsBuilder builder = multiInstance();
    consumer.accept(builder);
    return myself;
  }

  protected double calculateXCoordinate(final Bounds boundaryEventBounds) {
    final BpmnShape attachedToElement = findBpmnShape(element);

    double x = 0;

    if (attachedToElement != null) {

      final Bounds attachedToBounds = attachedToElement.getBounds();

      final Collection<BoundaryEvent> boundaryEvents =
          element.getParentElement().getChildElementsByType(BoundaryEvent.class);
      final Collection<BoundaryEvent> attachedBoundaryEvents = new ArrayList<>();

      for (final BoundaryEvent tmp : boundaryEvents) {
        if (tmp.getAttachedTo().equals(element)) {
          attachedBoundaryEvents.add(tmp);
        }
      }

      final double attachedToX = attachedToBounds.getX();
      final double attachedToWidth = attachedToBounds.getWidth();
      final double boundaryWidth = boundaryEventBounds.getWidth();

      switch (attachedBoundaryEvents.size()) {
        case 2:
          {
            x = attachedToX + attachedToWidth / 2 + boundaryWidth / 2;
            break;
          }
        case 3:
          {
            x = attachedToX + attachedToWidth / 2 - 1.5 * boundaryWidth;
            break;
          }
        default:
          {
            x = attachedToX + attachedToWidth / 2 - boundaryWidth / 2;
            break;
          }
      }
    }

    return x;
  }

  protected void setBoundaryEventCoordinates(final BpmnShape bpmnShape) {
    final BpmnShape activity = findBpmnShape(element);
    final Bounds boundaryBounds = bpmnShape.getBounds();

    double x = 0;
    double y = 0;

    if (activity != null) {
      final Bounds activityBounds = activity.getBounds();
      final double activityY = activityBounds.getY();
      final double activityHeight = activityBounds.getHeight();
      final double boundaryHeight = boundaryBounds.getHeight();
      x = calculateXCoordinate(boundaryBounds);
      y = activityY + activityHeight - boundaryHeight / 2;
    }

    boundaryBounds.setX(x);
    boundaryBounds.setY(y);
  }

  @Override
  public B kunpengInputExpression(final String sourceExpression, final String target) {
    return variablesMappingBuilder.kunpengInputExpression(sourceExpression, target);
  }

  @Override
  public B kunpengOutputExpression(final String sourceExpression, final String target) {
    return variablesMappingBuilder.kunpengOutputExpression(sourceExpression, target);
  }

  @Override
  public B kunpengInput(final String source, final String target) {
    return variablesMappingBuilder.kunpengInput(source, target);
  }

  @Override
  public B kunpengOutput(final String source, final String target) {
    return variablesMappingBuilder.kunpengOutput(source, target);
  }

  @Override
  public B kunpengStartExecutionListener(final String type, final String retries) {
    return kunpengExecutionListenersBuilder.kunpengStartExecutionListener(type, retries);
  }

  @Override
  public B kunpengStartExecutionListener(final String type) {
    return kunpengExecutionListenersBuilder.kunpengStartExecutionListener(type);
  }

  @Override
  public B kunpengEndExecutionListener(final String type, final String retries) {
    return kunpengExecutionListenersBuilder.kunpengEndExecutionListener(type, retries);
  }

  @Override
  public B kunpengEndExecutionListener(final String type) {
    return kunpengExecutionListenersBuilder.kunpengEndExecutionListener(type);
  }

  @Override
  public B kunpengExecutionListener(
      final Consumer<ExecutionListenerBuilder> executionListenerBuilderConsumer) {
    return kunpengExecutionListenersBuilder.kunpengExecutionListener(
        executionListenerBuilderConsumer);
  }
}
