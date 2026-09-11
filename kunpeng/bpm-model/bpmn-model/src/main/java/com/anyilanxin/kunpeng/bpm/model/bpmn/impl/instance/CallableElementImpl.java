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

import static com.anyilanxin.kunpeng.bpm.model.bpmn.impl.BpmnModelConstants.*;

import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.*;
import com.anyilanxin.kunpeng.bpm.model.xml.ModelBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import com.anyilanxin.kunpeng.bpm.model.xml.instance.ModelElementInstance;
import com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementTypeBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;
import com.anyilanxin.kunpeng.bpm.model.xml.type.attribute.Attribute;
import com.anyilanxin.kunpeng.bpm.model.xml.type.child.ChildElement;
import com.anyilanxin.kunpeng.bpm.model.xml.type.child.ChildElementCollection;
import com.anyilanxin.kunpeng.bpm.model.xml.type.child.SequenceBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.type.reference.ElementReferenceCollection;
import java.util.Collection;

/**
 * The BPMN callableElement element
 *
 * @author Daniel Meyer
 * @author Sebastian Menski
 */
public class CallableElementImpl extends RootElementImpl implements CallableElement {

  protected static Attribute<String> nameAttribute;
  protected static ElementReferenceCollection<Interface, SupportedInterfaceRef>
      supportedInterfaceRefCollection;
  protected static ChildElement<IoSpecification> ioSpecificationChild;
  protected static ChildElementCollection<IoBinding> ioBindingCollection;

  public CallableElementImpl(final ModelTypeInstanceContext instanceContext) {
    super(instanceContext);
  }

  public static void registerType(final ModelBuilder bpmnModelBuilder) {
    final ModelElementTypeBuilder typeBuilder =
        bpmnModelBuilder
            .defineType(CallableElement.class, BPMN_ELEMENT_CALLABLE_ELEMENT)
            .namespaceUri(BPMN20_NS)
            .extendsType(RootElement.class)
            .instanceProvider(
                new ModelTypeInstanceProvider<ModelElementInstance>() {
                  @Override
                  public ModelElementInstance newInstance(
                      final ModelTypeInstanceContext instanceContext) {
                    return new CallableElementImpl(instanceContext);
                  }
                });

    nameAttribute = typeBuilder.stringAttribute(BPMN_ATTRIBUTE_NAME).build();

    final SequenceBuilder sequenceBuilder = typeBuilder.sequence();

    supportedInterfaceRefCollection =
        sequenceBuilder
            .elementCollection(SupportedInterfaceRef.class)
            .qNameElementReferenceCollection(Interface.class)
            .build();

    ioSpecificationChild = sequenceBuilder.element(IoSpecification.class).build();

    ioBindingCollection = sequenceBuilder.elementCollection(IoBinding.class).build();

    typeBuilder.build();
  }

  @Override
  public String getName() {
    return nameAttribute.getValue(this);
  }

  @Override
  public void setName(final String name) {
    nameAttribute.setValue(this, name);
  }

  @Override
  public Collection<Interface> getSupportedInterfaces() {
    return supportedInterfaceRefCollection.getReferenceTargetElements(this);
  }

  @Override
  public IoSpecification getIoSpecification() {
    return ioSpecificationChild.getChild(this);
  }

  @Override
  public void setIoSpecification(final IoSpecification ioSpecification) {
    ioSpecificationChild.setChild(this, ioSpecification);
  }

  @Override
  public Collection<IoBinding> getIoBindings() {
    return ioBindingCollection.get(this);
  }
}
