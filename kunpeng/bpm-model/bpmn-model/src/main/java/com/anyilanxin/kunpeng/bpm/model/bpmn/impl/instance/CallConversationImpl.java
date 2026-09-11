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
import static com.anyilanxin.kunpeng.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ATTRIBUTE_CALLED_COLLABORATION_REF;
import static com.anyilanxin.kunpeng.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ELEMENT_CALL_CONVERSATION;

import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.CallConversation;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.ConversationNode;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.GlobalConversation;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.ParticipantAssociation;
import com.anyilanxin.kunpeng.bpm.model.xml.ModelBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementTypeBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;
import com.anyilanxin.kunpeng.bpm.model.xml.type.child.ChildElementCollection;
import com.anyilanxin.kunpeng.bpm.model.xml.type.child.SequenceBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.type.reference.AttributeReference;
import java.util.Collection;

/**
 * The BPMN callConversation element
 *
 * @author Sebastian Menski
 */
public class CallConversationImpl extends ConversationNodeImpl implements CallConversation {

  protected static AttributeReference<GlobalConversation> calledCollaborationRefAttribute;
  protected static ChildElementCollection<ParticipantAssociation> participantAssociationCollection;

  public CallConversationImpl(final ModelTypeInstanceContext instanceContext) {
    super(instanceContext);
  }

  public static void registerType(final ModelBuilder modelBuilder) {
    final ModelElementTypeBuilder typeBuilder =
        modelBuilder
            .defineType(CallConversation.class, BPMN_ELEMENT_CALL_CONVERSATION)
            .namespaceUri(BPMN20_NS)
            .extendsType(ConversationNode.class)
            .instanceProvider(
                new ModelTypeInstanceProvider<CallConversation>() {
                  @Override
                  public CallConversation newInstance(
                      final ModelTypeInstanceContext instanceContext) {
                    return new CallConversationImpl(instanceContext);
                  }
                });

    calledCollaborationRefAttribute =
        typeBuilder
            .stringAttribute(BPMN_ATTRIBUTE_CALLED_COLLABORATION_REF)
            .qNameAttributeReference(GlobalConversation.class)
            .build();

    final SequenceBuilder sequenceBuilder = typeBuilder.sequence();

    participantAssociationCollection =
        sequenceBuilder.elementCollection(ParticipantAssociation.class).build();

    typeBuilder.build();
  }

  @Override
  public GlobalConversation getCalledCollaboration() {
    return calledCollaborationRefAttribute.getReferenceTargetElement(this);
  }

  @Override
  public void setCalledCollaboration(final GlobalConversation calledCollaboration) {
    calledCollaborationRefAttribute.setReferenceTargetElement(this, calledCollaboration);
  }

  @Override
  public Collection<ParticipantAssociation> getParticipantAssociations() {
    return participantAssociationCollection.get(this);
  }
}
