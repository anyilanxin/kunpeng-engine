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
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.CompletionCondition;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.LoopCardinality;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.MultiInstanceLoopCharacteristics;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengLoopCharacteristics;

/**
 * @author Thorben Lindhauer
 */
public class AbstractMultiInstanceLoopCharacteristicsBuilder<
        B extends AbstractMultiInstanceLoopCharacteristicsBuilder<B>>
    extends AbstractBaseElementBuilder<B, MultiInstanceLoopCharacteristics> {

  protected AbstractMultiInstanceLoopCharacteristicsBuilder(
      final BpmnModelInstance modelInstance,
      final MultiInstanceLoopCharacteristics element,
      final Class<?> selfType) {
    super(modelInstance, element, selfType);
  }

  /**
   * Sets the multi instance loop characteristics to be sequential.
   *
   * @return the builder object
   */
  public B sequential() {
    element.setSequential(true);
    return myself;
  }

  /**
   * Sets the multi instance loop characteristics to be parallel.
   *
   * @return the builder object
   */
  public B parallel() {
    element.setSequential(false);
    return myself;
  }

  /**
   * Sets the cardinality expression.
   *
   * @param expression the cardinality expression
   * @return the builder object
   */
  public B cardinality(final String expression) {
    final LoopCardinality cardinality = getCreateSingleChild(LoopCardinality.class);
    cardinality.setTextContent(expression);

    return myself;
  }

  /**
   * Sets the completion condition expression.
   *
   * @param expression the completion condition expression
   * @return the builder object
   */
  public B completionCondition(final String expression) {
    final CompletionCondition condition = getCreateSingleChild(CompletionCondition.class);
    condition.setTextContent(expression);

    return myself;
  }

  /**
   * Finishes the building of a multi instance loop characteristics.
   *
   * @return the parent activity builder
   */
  public <T extends AbstractActivityBuilder> T multiInstanceDone() {
    return (T) ((Activity) element.getParentElement()).builder();
  }

  public B collection(final String collection) {
    final KunpengLoopCharacteristics characteristics =
        getCreateSingleExtensionElement(KunpengLoopCharacteristics.class);
    characteristics.setCollection(collection);
    return myself;
  }

  public B elementVariable(final String elementVariable) {
    final KunpengLoopCharacteristics characteristics =
        getCreateSingleExtensionElement(KunpengLoopCharacteristics.class);
    characteristics.setElementVariable(elementVariable);
    return myself;
  }
}
