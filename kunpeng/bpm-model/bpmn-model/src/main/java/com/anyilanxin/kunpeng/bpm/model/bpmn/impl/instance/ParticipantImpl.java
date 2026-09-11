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
import static com.anyilanxin.kunpeng.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ATTRIBUTE_NAME;
import static com.anyilanxin.kunpeng.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ATTRIBUTE_PROCESS_REF;
import static com.anyilanxin.kunpeng.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ELEMENT_PARTICIPANT;

import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.BaseElement;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.EndPoint;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.Interface;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.Participant;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.ParticipantMultiplicity;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.Process;
import com.anyilanxin.kunpeng.bpm.model.xml.ModelBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementTypeBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;
import com.anyilanxin.kunpeng.bpm.model.xml.type.attribute.Attribute;
import com.anyilanxin.kunpeng.bpm.model.xml.type.child.ChildElement;
import com.anyilanxin.kunpeng.bpm.model.xml.type.child.SequenceBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.type.reference.AttributeReference;
import com.anyilanxin.kunpeng.bpm.model.xml.type.reference.ElementReferenceCollection;
import java.util.Collection;

/**
 * The BPMN participant element
 *
 * @author Sebastian Menski
 */
public class ParticipantImpl extends BaseElementImpl implements Participant {

  protected static Attribute<String> nameAttribute;
  protected static AttributeReference<Process> processRefAttribute;
  protected static ElementReferenceCollection<Interface, InterfaceRef> interfaceRefCollection;
  protected static ElementReferenceCollection<EndPoint, EndPointRef> endPointRefCollection;
  protected static ChildElement<ParticipantMultiplicity> participantMultiplicityChild;

  public ParticipantImpl(final ModelTypeInstanceContext instanceContext) {
    super(instanceContext);
  }

  public static void registerType(final ModelBuilder modelBuilder) {
    final ModelElementTypeBuilder typeBuilder =
        modelBuilder
            .defineType(Participant.class, BPMN_ELEMENT_PARTICIPANT)
            .namespaceUri(BPMN20_NS)
            .extendsType(BaseElement.class)
            .instanceProvider(
                new ModelTypeInstanceProvider<Participant>() {
                  @Override
                  public Participant newInstance(final ModelTypeInstanceContext instanceContext) {
                    return new ParticipantImpl(instanceContext);
                  }
                });

    nameAttribute = typeBuilder.stringAttribute(BPMN_ATTRIBUTE_NAME).build();

    processRefAttribute =
        typeBuilder
            .stringAttribute(BPMN_ATTRIBUTE_PROCESS_REF)
            .qNameAttributeReference(Process.class)
            .build();

    final SequenceBuilder sequenceBuilder = typeBuilder.sequence();

    interfaceRefCollection =
        sequenceBuilder
            .elementCollection(InterfaceRef.class)
            .qNameElementReferenceCollection(Interface.class)
            .build();

    endPointRefCollection =
        sequenceBuilder
            .elementCollection(EndPointRef.class)
            .qNameElementReferenceCollection(EndPoint.class)
            .build();

    participantMultiplicityChild = sequenceBuilder.element(ParticipantMultiplicity.class).build();

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
  public Process getProcess() {
    return processRefAttribute.getReferenceTargetElement(this);
  }

  @Override
  public void setProcess(final Process process) {
    processRefAttribute.setReferenceTargetElement(this, process);
  }

  @Override
  public Collection<Interface> getInterfaces() {
    return interfaceRefCollection.getReferenceTargetElements(this);
  }

  @Override
  public Collection<EndPoint> getEndPoints() {
    return endPointRefCollection.getReferenceTargetElements(this);
  }

  @Override
  public ParticipantMultiplicity getParticipantMultiplicity() {
    return participantMultiplicityChild.getChild(this);
  }

  @Override
  public void setParticipantMultiplicity(final ParticipantMultiplicity participantMultiplicity) {
    participantMultiplicityChild.setChild(this, participantMultiplicity);
  }
}
