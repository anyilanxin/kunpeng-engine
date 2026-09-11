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

import static com.anyilanxin.kunpeng.bpm.model.bpmn.impl.BpmnModelConstants.BPMN20_NS;
import static com.anyilanxin.kunpeng.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ELEMENT_EXTENSION_ELEMENTS;

import com.anyilanxin.kunpeng.bpm.model.bpmn.Query;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.QueryImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.ExtensionElements;
import com.anyilanxin.kunpeng.bpm.model.xml.ModelBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.util.ModelUtil;
import com.anyilanxin.kunpeng.bpm.model.xml.instance.ModelElementInstance;
import com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementType;
import com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementTypeBuilder;
import java.util.Collection;

/**
 * The BPMN extensionElements element
 *
 * @author Daniel Meyer
 * @author Sebastian Menski
 */
public class ExtensionElementsImpl extends BpmnModelElementInstanceImpl
    implements ExtensionElements {

  public ExtensionElementsImpl(final ModelTypeInstanceContext context) {
    super(context);
  }

  public static void registerType(final ModelBuilder modelBuilder) {

    final ModelElementTypeBuilder typeBuilder =
        modelBuilder
            .defineType(ExtensionElements.class, BPMN_ELEMENT_EXTENSION_ELEMENTS)
            .namespaceUri(BPMN20_NS)
            .instanceProvider(
                new ModelElementTypeBuilder.ModelTypeInstanceProvider<ExtensionElements>() {
                  @Override
                  public ExtensionElements newInstance(
                      final ModelTypeInstanceContext instanceContext) {
                    return new ExtensionElementsImpl(instanceContext);
                  }
                });

    typeBuilder.build();
  }

  @Override
  public Collection<ModelElementInstance> getElements() {
    return ModelUtil.getModelElementCollection(getDomElement().getChildElements(), modelInstance);
  }

  @Override
  public Query<ModelElementInstance> getElementsQuery() {
    return new QueryImpl<ModelElementInstance>(getElements());
  }

  @Override
  public ModelElementInstance addExtensionElement(
      final String namespaceUri, final String localName) {
    final ModelElementType extensionElementType =
        modelInstance.registerGenericType(namespaceUri, localName);
    final ModelElementInstance extensionElement = extensionElementType.newInstance(modelInstance);
    addChildElement(extensionElement);
    return extensionElement;
  }

  @Override
  public <T extends ModelElementInstance> T addExtensionElement(
      final Class<T> extensionElementClass) {
    final ModelElementInstance extensionElement = modelInstance.newInstance(extensionElementClass);
    addChildElement(extensionElement);
    return extensionElementClass.cast(extensionElement);
  }

  @Override
  public void addChildElement(final ModelElementInstance extensionElement) {
    getDomElement().appendChild(extensionElement.getDomElement());
  }
}
