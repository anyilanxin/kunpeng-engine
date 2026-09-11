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
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.ServiceTask;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengBindingType;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengLinkedResource;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.kunpeng.KunpengLinkedResources;
import java.util.function.Consumer;

/**
 * @author Sebastian Menski
 */
public abstract class AbstractServiceTaskBuilder<B extends AbstractServiceTaskBuilder<B>>
    extends AbstractJobWorkerTaskBuilder<B, ServiceTask> {

  protected AbstractServiceTaskBuilder(
      final BpmnModelInstance modelInstance, final ServiceTask element, final Class<?> selfType) {
    super(modelInstance, element, selfType);
  }

  /**
   * Sets the implementation of the build service task.
   *
   * @param implementation the implementation to set
   * @return the builder object
   */
  public B implementation(final String implementation) {
    element.setImplementation(implementation);
    return myself;
  }

  public B kunpengLinkedResources(
      final Consumer<LinkedResourceBuilder> linkedResourceBuilderConsumer) {
    final KunpengLinkedResource linkedResource = createLinkedResourceElement();
    linkedResource.setBindingType(KunpengBindingType.latest);

    final LinkedResourceBuilder builder = new LinkedResourceBuilder(linkedResource, myself);
    linkedResourceBuilderConsumer.accept(builder);
    return myself;
  }

  private KunpengLinkedResource createLinkedResourceElement() {
    final KunpengLinkedResources linkedResources =
        myself.getCreateSingleExtensionElement(KunpengLinkedResources.class);
    return myself.createChild(linkedResources, KunpengLinkedResource.class);
  }
}
