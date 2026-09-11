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
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.CallActivity;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengBindingType;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengCalledElement;

/**
 * @author Sebastian Menski
 */
public class AbstractCallActivityBuilder<B extends AbstractCallActivityBuilder<B>>
    extends AbstractActivityBuilder<B, CallActivity> {

  protected AbstractCallActivityBuilder(
      final BpmnModelInstance modelInstance, final CallActivity element, final Class<?> selfType) {
    super(modelInstance, element, selfType);
  }

  /**
   * Sets the called element
   *
   * @param calledElement the process to call
   * @return the builder object
   */
  public B calledElement(final String calledElement) {
    element.setCalledElement(calledElement);
    return myself;
  }

  public B kunpengProcessId(final String processId) {
    final KunpengCalledElement calledElement =
        getCreateSingleExtensionElement(KunpengCalledElement.class);
    calledElement.setProcessId(processId);
    return myself;
  }

  public B kunpengProcessIdExpression(final String processIdExpression) {
    final KunpengCalledElement calledElement =
        getCreateSingleExtensionElement(KunpengCalledElement.class);
    calledElement.setProcessId(asKunpengExpression(processIdExpression));
    return myself;
  }

  public B kunpengPropagateAllChildVariables(final boolean propagateAllChildVariables) {
    final KunpengCalledElement calledElement =
        getCreateSingleExtensionElement(KunpengCalledElement.class);
    calledElement.setPropagateAllChildVariablesEnabled(propagateAllChildVariables);
    return myself;
  }

  public B kunpengPropagateAllParentVariables(final boolean propagateAllParentVariables) {
    final KunpengCalledElement calledElement =
        getCreateSingleExtensionElement(KunpengCalledElement.class);
    calledElement.setPropagateAllParentVariablesEnabled(propagateAllParentVariables);
    return myself;
  }

  public B kunpengBindingType(final KunpengBindingType bindingType) {
    final KunpengCalledElement calledElement =
        getCreateSingleExtensionElement(KunpengCalledElement.class);
    calledElement.setBindingType(bindingType);
    return myself;
  }

  public B kunpengVersionTag(final String versionTag) {
    final KunpengCalledElement calledElement =
        getCreateSingleExtensionElement(KunpengCalledElement.class);
    calledElement.setVersionTag(versionTag);
    return myself;
  }
}
