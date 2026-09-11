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

package com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance;

import com.anyilanxin.kunpeng.bpm.model.bpmn.BpmnModelException;
import com.anyilanxin.kunpeng.bpm.model.bpmn.builder.AbstractBaseElementBuilder;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.BpmnModelElementInstance;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.SubProcess;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.instance.ModelElementInstanceImpl;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.instance.ModelTypeInstanceContext;

/**
 * Shared base class for all BPMN Model Elements. Provides implementation of the {@link
 * BpmnModelElementInstance} interface.
 *
 * @author Daniel Meyer
 */
public abstract class BpmnModelElementInstanceImpl extends ModelElementInstanceImpl
    implements BpmnModelElementInstance {

  public BpmnModelElementInstanceImpl(final ModelTypeInstanceContext instanceContext) {
    super(instanceContext);
  }

  @Override
  @SuppressWarnings("rawtypes")
  public AbstractBaseElementBuilder builder() {
    throw new BpmnModelException("No builder implemented for " + this);
  }

  @Override
  public boolean isScope() {
    return this instanceof com.anyilanxin.kunpeng.bpm.model.bpmn.instance.Process
        || this instanceof SubProcess;
  }

  @Override
  public BpmnModelElementInstance getScope() {
    final BpmnModelElementInstance parentElement = (BpmnModelElementInstance) getParentElement();
    if (parentElement != null) {
      if (parentElement.isScope()) {
        return parentElement;
      } else {
        return parentElement.getScope();
      }
    } else {
      return null;
    }
  }
}
