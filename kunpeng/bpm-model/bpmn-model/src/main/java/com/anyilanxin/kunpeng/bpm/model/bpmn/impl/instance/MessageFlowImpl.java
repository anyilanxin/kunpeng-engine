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
import static com.anyilanxin.kunpeng.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ATTRIBUTE_MESSAGE_REF;
import static com.anyilanxin.kunpeng.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ATTRIBUTE_NAME;
import static com.anyilanxin.kunpeng.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ATTRIBUTE_SOURCE_REF;
import static com.anyilanxin.kunpeng.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ATTRIBUTE_TARGET_REF;
import static com.anyilanxin.kunpeng.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ELEMENT_MESSAGE_FLOW;

import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.BaseElement;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.InteractionNode;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.Message;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.MessageFlow;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.bpmndi.BpmnEdge;
import com.anyilanxin.kunpeng.bpm.model.xml.ModelBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementTypeBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;
import com.anyilanxin.kunpeng.bpm.model.xml.type.attribute.Attribute;
import com.anyilanxin.kunpeng.bpm.model.xml.type.reference.AttributeReference;

/**
 * The BPMN messageFlow element
 *
 * @author Sebastian Menski
 */
public class MessageFlowImpl extends BaseElementImpl implements MessageFlow {

  protected static Attribute<String> nameAttribute;
  protected static AttributeReference<InteractionNode> sourceRefAttribute;
  protected static AttributeReference<InteractionNode> targetRefAttribute;
  protected static AttributeReference<Message> messageRefAttribute;

  public MessageFlowImpl(final ModelTypeInstanceContext instanceContext) {
    super(instanceContext);
  }

  public static void registerType(final ModelBuilder modelBuilder) {
    final ModelElementTypeBuilder typeBuilder =
        modelBuilder
            .defineType(MessageFlow.class, BPMN_ELEMENT_MESSAGE_FLOW)
            .namespaceUri(BPMN20_NS)
            .extendsType(BaseElement.class)
            .instanceProvider(
                new ModelTypeInstanceProvider<MessageFlow>() {
                  @Override
                  public MessageFlow newInstance(final ModelTypeInstanceContext instanceContext) {
                    return new MessageFlowImpl(instanceContext);
                  }
                });

    nameAttribute = typeBuilder.stringAttribute(BPMN_ATTRIBUTE_NAME).build();

    sourceRefAttribute =
        typeBuilder
            .stringAttribute(BPMN_ATTRIBUTE_SOURCE_REF)
            .required()
            .qNameAttributeReference(InteractionNode.class)
            .build();

    targetRefAttribute =
        typeBuilder
            .stringAttribute(BPMN_ATTRIBUTE_TARGET_REF)
            .required()
            .qNameAttributeReference(InteractionNode.class)
            .build();

    messageRefAttribute =
        typeBuilder
            .stringAttribute(BPMN_ATTRIBUTE_MESSAGE_REF)
            .qNameAttributeReference(Message.class)
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
  public InteractionNode getSource() {
    return sourceRefAttribute.getReferenceTargetElement(this);
  }

  @Override
  public void setSource(final InteractionNode source) {
    sourceRefAttribute.setReferenceTargetElement(this, source);
  }

  @Override
  public InteractionNode getTarget() {
    return targetRefAttribute.getReferenceTargetElement(this);
  }

  @Override
  public void setTarget(final InteractionNode target) {
    targetRefAttribute.setReferenceTargetElement(this, target);
  }

  @Override
  public Message getMessage() {
    return messageRefAttribute.getReferenceTargetElement(this);
  }

  @Override
  public void setMessage(final Message message) {
    messageRefAttribute.setReferenceTargetElement(this, message);
  }

  @Override
  public BpmnEdge getDiagramElement() {
    return (BpmnEdge) super.getDiagramElement();
  }
}
