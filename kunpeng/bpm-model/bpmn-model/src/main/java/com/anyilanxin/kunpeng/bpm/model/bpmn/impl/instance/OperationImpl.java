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

import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.BaseElement;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.Error;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.Message;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.Operation;
import com.anyilanxin.kunpeng.bpm.model.xml.ModelBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementTypeBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;
import com.anyilanxin.kunpeng.bpm.model.xml.type.attribute.Attribute;
import com.anyilanxin.kunpeng.bpm.model.xml.type.child.SequenceBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.type.reference.ElementReference;
import com.anyilanxin.kunpeng.bpm.model.xml.type.reference.ElementReferenceCollection;
import java.util.Collection;

/**
 * The BPMN operation element
 *
 * @author Sebastian Menski
 */
public class OperationImpl extends BaseElementImpl implements Operation {

  protected static Attribute<String> nameAttribute;
  protected static Attribute<String> implementationRefAttribute;
  protected static ElementReference<Message, InMessageRef> inMessageRefChild;
  protected static ElementReference<Message, OutMessageRef> outMessageRefChild;
  protected static ElementReferenceCollection<Error, ErrorRef> errorRefCollection;

  public OperationImpl(final ModelTypeInstanceContext instanceContext) {
    super(instanceContext);
  }

  public static void registerType(final ModelBuilder modelBuilder) {
    final ModelElementTypeBuilder typeBuilder =
        modelBuilder
            .defineType(Operation.class, BPMN_ELEMENT_OPERATION)
            .namespaceUri(BPMN20_NS)
            .extendsType(BaseElement.class)
            .instanceProvider(
                new ModelTypeInstanceProvider<Operation>() {
                  @Override
                  public Operation newInstance(final ModelTypeInstanceContext instanceContext) {
                    return new OperationImpl(instanceContext);
                  }
                });

    nameAttribute = typeBuilder.stringAttribute(BPMN_ATTRIBUTE_NAME).required().build();

    implementationRefAttribute =
        typeBuilder.stringAttribute(BPMN_ELEMENT_IMPLEMENTATION_REF).build();

    final SequenceBuilder sequenceBuilder = typeBuilder.sequence();

    inMessageRefChild =
        sequenceBuilder
            .element(InMessageRef.class)
            .required()
            .qNameElementReference(Message.class)
            .build();

    outMessageRefChild =
        sequenceBuilder.element(OutMessageRef.class).qNameElementReference(Message.class).build();

    errorRefCollection =
        sequenceBuilder
            .elementCollection(ErrorRef.class)
            .qNameElementReferenceCollection(Error.class)
            .build();

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
  public Message getInMessage() {
    return inMessageRefChild.getReferenceTargetElement(this);
  }

  @Override
  public void setInMessage(final Message message) {
    inMessageRefChild.setReferenceTargetElement(this, message);
  }

  @Override
  public Message getOutMessage() {
    return outMessageRefChild.getReferenceTargetElement(this);
  }

  @Override
  public void setOutMessage(final Message message) {
    outMessageRefChild.setReferenceTargetElement(this, message);
  }

  @Override
  public Collection<Error> getErrors() {
    return errorRefCollection.getReferenceTargetElements(this);
  }
}
