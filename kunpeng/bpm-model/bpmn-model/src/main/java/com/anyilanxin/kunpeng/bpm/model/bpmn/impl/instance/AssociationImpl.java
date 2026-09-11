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
import static com.anyilanxin.kunpeng.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ATTRIBUTE_ASSOCIATION_DIRECTION;
import static com.anyilanxin.kunpeng.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ATTRIBUTE_SOURCE_REF;
import static com.anyilanxin.kunpeng.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ATTRIBUTE_TARGET_REF;
import static com.anyilanxin.kunpeng.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ELEMENT_ASSOCIATION;

import com.anyilanxin.kunpeng.bpm.model.bpmn.AssociationDirection;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.Artifact;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.Association;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.BaseElement;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.bpmndi.BpmnEdge;
import com.anyilanxin.kunpeng.bpm.model.xml.ModelBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementTypeBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;
import com.anyilanxin.kunpeng.bpm.model.xml.type.attribute.Attribute;
import com.anyilanxin.kunpeng.bpm.model.xml.type.reference.AttributeReference;

/**
 * @author Sebastian Menski
 */
public class AssociationImpl extends ArtifactImpl implements Association {

  protected static AttributeReference<BaseElement> sourceRefAttribute;
  protected static AttributeReference<BaseElement> targetRefAttribute;
  protected static Attribute<AssociationDirection> associationDirectionAttribute;

  public AssociationImpl(final ModelTypeInstanceContext instanceContext) {
    super(instanceContext);
  }

  public static void registerType(final ModelBuilder modelBuilder) {
    final ModelElementTypeBuilder typeBuilder =
        modelBuilder
            .defineType(Association.class, BPMN_ELEMENT_ASSOCIATION)
            .namespaceUri(BPMN20_NS)
            .extendsType(Artifact.class)
            .instanceProvider(
                new ModelTypeInstanceProvider<Association>() {
                  @Override
                  public Association newInstance(final ModelTypeInstanceContext instanceContext) {
                    return new AssociationImpl(instanceContext);
                  }
                });

    sourceRefAttribute =
        typeBuilder
            .stringAttribute(BPMN_ATTRIBUTE_SOURCE_REF)
            .required()
            .qNameAttributeReference(BaseElement.class)
            .build();

    targetRefAttribute =
        typeBuilder
            .stringAttribute(BPMN_ATTRIBUTE_TARGET_REF)
            .required()
            .qNameAttributeReference(BaseElement.class)
            .build();

    associationDirectionAttribute =
        typeBuilder
            .enumAttribute(BPMN_ATTRIBUTE_ASSOCIATION_DIRECTION, AssociationDirection.class)
            .defaultValue(AssociationDirection.None)
            .build();

    typeBuilder.build();
  }

  @Override
  public BaseElement getSource() {
    return sourceRefAttribute.getReferenceTargetElement(this);
  }

  @Override
  public void setSource(final BaseElement source) {
    sourceRefAttribute.setReferenceTargetElement(this, source);
  }

  @Override
  public BaseElement getTarget() {
    return targetRefAttribute.getReferenceTargetElement(this);
  }

  @Override
  public void setTarget(final BaseElement target) {
    targetRefAttribute.setReferenceTargetElement(this, target);
  }

  @Override
  public AssociationDirection getAssociationDirection() {
    return associationDirectionAttribute.getValue(this);
  }

  @Override
  public void setAssociationDirection(final AssociationDirection associationDirection) {
    associationDirectionAttribute.setValue(this, associationDirection);
  }

  @Override
  public BpmnEdge getDiagramElement() {
    return (BpmnEdge) super.getDiagramElement();
  }
}
