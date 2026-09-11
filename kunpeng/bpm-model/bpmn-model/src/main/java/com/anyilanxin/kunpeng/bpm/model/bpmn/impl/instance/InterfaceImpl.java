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

import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.Interface;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.Operation;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.RootElement;
import com.anyilanxin.kunpeng.bpm.model.xml.ModelBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementTypeBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;
import com.anyilanxin.kunpeng.bpm.model.xml.type.attribute.Attribute;
import com.anyilanxin.kunpeng.bpm.model.xml.type.child.ChildElementCollection;
import com.anyilanxin.kunpeng.bpm.model.xml.type.child.SequenceBuilder;
import java.util.Collection;

/**
 * The BPMN interface element
 *
 * @author Sebastian Menski
 */
public class InterfaceImpl extends RootElementImpl implements Interface {

  protected static Attribute<String> nameAttribute;
  protected static Attribute<String> implementationRefAttribute;
  protected static ChildElementCollection<Operation> operationCollection;

  public InterfaceImpl(final ModelTypeInstanceContext context) {
    super(context);
  }

  public static void registerType(final ModelBuilder modelBuilder) {
    final ModelElementTypeBuilder typeBuilder =
        modelBuilder
            .defineType(Interface.class, BPMN_ELEMENT_INTERFACE)
            .namespaceUri(BPMN20_NS)
            .extendsType(RootElement.class)
            .instanceProvider(
                new ModelTypeInstanceProvider<Interface>() {
                  @Override
                  public Interface newInstance(final ModelTypeInstanceContext instanceContext) {
                    return new InterfaceImpl(instanceContext);
                  }
                });

    nameAttribute = typeBuilder.stringAttribute(BPMN_ATTRIBUTE_NAME).required().build();

    implementationRefAttribute =
        typeBuilder.stringAttribute(BPMN_ATTRIBUTE_IMPLEMENTATION_REF).build();

    final SequenceBuilder sequenceBuilder = typeBuilder.sequence();

    operationCollection = sequenceBuilder.elementCollection(Operation.class).required().build();

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
  public String getImplementationRef() {
    return implementationRefAttribute.getValue(this);
  }

  @Override
  public void setImplementationRef(final String implementationRef) {
    implementationRefAttribute.setValue(this, implementationRef);
  }

  @Override
  public Collection<Operation> getOperations() {
    return operationCollection.get(this);
  }
}
