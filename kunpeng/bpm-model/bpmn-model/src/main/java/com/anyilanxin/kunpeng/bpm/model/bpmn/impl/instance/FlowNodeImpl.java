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

import com.anyilanxin.kunpeng.bpm.model.bpmn.BpmnModelException;
import com.anyilanxin.kunpeng.bpm.model.bpmn.Query;
import com.anyilanxin.kunpeng.bpm.model.bpmn.builder.AbstractFlowNodeBuilder;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.QueryImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.FlowElement;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.FlowNode;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.SequenceFlow;
import com.anyilanxin.kunpeng.bpm.model.xml.ModelBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import com.anyilanxin.kunpeng.bpm.model.xml.instance.ModelElementInstance;
import com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementTypeBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.type.child.SequenceBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.type.reference.AttributeReference;
import com.anyilanxin.kunpeng.bpm.model.xml.type.reference.ElementReferenceCollection;
import com.anyilanxin.kunpeng.bpm.model.xml.type.reference.Reference;
import java.util.Collection;
import java.util.HashSet;

/**
 * The BPMN flowNode element
 *
 * @author Sebastian Menski
 */
public abstract class FlowNodeImpl extends FlowElementImpl implements FlowNode {

  protected static ElementReferenceCollection<SequenceFlow, Incoming> incomingCollection;
  protected static ElementReferenceCollection<SequenceFlow, Outgoing> outgoingCollection;

  public FlowNodeImpl(final ModelTypeInstanceContext context) {
    super(context);
  }

  public static void registerType(final ModelBuilder modelBuilder) {
    final ModelElementTypeBuilder typeBuilder =
        modelBuilder
            .defineType(FlowNode.class, BPMN_ELEMENT_FLOW_NODE)
            .namespaceUri(BPMN20_NS)
            .extendsType(FlowElement.class)
            .abstractType();

    final SequenceBuilder sequenceBuilder = typeBuilder.sequence();

    incomingCollection =
        sequenceBuilder
            .elementCollection(Incoming.class)
            .qNameElementReferenceCollection(SequenceFlow.class)
            .build();

    outgoingCollection =
        sequenceBuilder
            .elementCollection(Outgoing.class)
            .qNameElementReferenceCollection(SequenceFlow.class)
            .build();

    typeBuilder.build();
  }

  @Override
  @SuppressWarnings("rawtypes")
  public AbstractFlowNodeBuilder builder() {
    throw new BpmnModelException(
        "No builder implemented for type "
            + getElementType().getTypeNamespace()
            + ":"
            + getElementType().getTypeName());
  }

  @Override
  @SuppressWarnings("rawtypes")
  public void updateAfterReplacement() {
    super.updateAfterReplacement();
    final Collection<Reference> incomingReferences =
        getIncomingReferencesByType(SequenceFlow.class);
    for (final Reference<?> reference : incomingReferences) {
      for (final ModelElementInstance sourceElement : reference.findReferenceSourceElements(this)) {
        final String referenceIdentifier = reference.getReferenceIdentifier(sourceElement);

        if (referenceIdentifier != null
            && referenceIdentifier.equals(getId())
            && reference instanceof AttributeReference) {
          final String attributeName =
              ((AttributeReference) reference).getReferenceSourceAttribute().getAttributeName();
          if (attributeName.equals(BPMN_ATTRIBUTE_SOURCE_REF)) {
            getOutgoing().add((SequenceFlow) sourceElement);
          } else if (attributeName.equals(BPMN_ATTRIBUTE_TARGET_REF)) {
            getIncoming().add((SequenceFlow) sourceElement);
          }
        }
      }
    }
  }

  @Override
  public Collection<SequenceFlow> getIncoming() {
    return incomingCollection.getReferenceTargetElements(this);
  }

  @Override
  public Collection<SequenceFlow> getOutgoing() {
    return outgoingCollection.getReferenceTargetElements(this);
  }

  @Override
  public Query<FlowNode> getPreviousNodes() {
    final Collection<FlowNode> previousNodes = new HashSet<>();
    for (final SequenceFlow sequenceFlow : getIncoming()) {
      previousNodes.add(sequenceFlow.getSource());
    }
    return new QueryImpl<>(previousNodes);
  }

  @Override
  public Query<FlowNode> getSucceedingNodes() {
    final Collection<FlowNode> succeedingNodes = new HashSet<>();
    for (final SequenceFlow sequenceFlow : getOutgoing()) {
      succeedingNodes.add(sequenceFlow.getTarget());
    }
    return new QueryImpl<>(succeedingNodes);
  }
}
