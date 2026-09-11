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
import static com.anyilanxin.kunpeng.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ELEMENT_THROW_EVENT;

import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.*;
import com.anyilanxin.kunpeng.bpm.model.xml.ModelBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementTypeBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.type.child.ChildElement;
import com.anyilanxin.kunpeng.bpm.model.xml.type.child.ChildElementCollection;
import com.anyilanxin.kunpeng.bpm.model.xml.type.child.SequenceBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.type.reference.ElementReferenceCollection;
import java.util.Collection;

/**
 * The BPMN throwEvent element
 *
 * @author Sebastian Menski
 */
public abstract class ThrowEventImpl extends EventImpl implements ThrowEvent {

  protected static ChildElementCollection<DataInput> dataInputCollection;
  protected static ChildElementCollection<DataInputAssociation> dataInputAssociationCollection;
  protected static ChildElement<InputSet> inputSetChild;
  protected static ChildElementCollection<EventDefinition> eventDefinitionCollection;
  protected static ElementReferenceCollection<EventDefinition, EventDefinitionRef>
      eventDefinitionRefCollection;

  public ThrowEventImpl(final ModelTypeInstanceContext context) {
    super(context);
  }

  public static void registerType(final ModelBuilder modelBuilder) {
    final ModelElementTypeBuilder typeBuilder =
        modelBuilder
            .defineType(ThrowEvent.class, BPMN_ELEMENT_THROW_EVENT)
            .namespaceUri(BPMN20_NS)
            .extendsType(Event.class)
            .abstractType();

    final SequenceBuilder sequenceBuilder = typeBuilder.sequence();

    dataInputCollection = sequenceBuilder.elementCollection(DataInput.class).build();

    dataInputAssociationCollection =
        sequenceBuilder.elementCollection(DataInputAssociation.class).build();

    inputSetChild = sequenceBuilder.element(InputSet.class).build();

    eventDefinitionCollection = sequenceBuilder.elementCollection(EventDefinition.class).build();

    eventDefinitionRefCollection =
        sequenceBuilder
            .elementCollection(EventDefinitionRef.class)
            .qNameElementReferenceCollection(EventDefinition.class)
            .build();

    typeBuilder.build();
  }

  @Override
  public Collection<DataInput> getDataInputs() {
    return dataInputCollection.get(this);
  }

  @Override
  public Collection<DataInputAssociation> getDataInputAssociations() {
    return dataInputAssociationCollection.get(this);
  }

  @Override
  public InputSet getInputSet() {
    return inputSetChild.getChild(this);
  }

  @Override
  public void setInputSet(final InputSet inputSet) {
    inputSetChild.setChild(this, inputSet);
  }

  @Override
  public Collection<EventDefinition> getEventDefinitions() {
    return eventDefinitionCollection.get(this);
  }

  @Override
  public Collection<EventDefinition> getEventDefinitionRefs() {
    return eventDefinitionRefCollection.getReferenceTargetElements(this);
  }
}
