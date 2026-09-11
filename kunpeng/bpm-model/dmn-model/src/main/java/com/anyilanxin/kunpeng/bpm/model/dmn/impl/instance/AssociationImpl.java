/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
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
package com.anyilanxin.kunpeng.bpm.model.dmn.impl.instance;

import static com.anyilanxin.kunpeng.bpm.model.dmn.impl.DmnModelConstants.*;

import com.anyilanxin.kunpeng.bpm.model.dmn.AssociationDirection;
import com.anyilanxin.kunpeng.bpm.model.dmn.instance.*;
import com.anyilanxin.kunpeng.bpm.model.xml.ModelBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementTypeBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;
import com.anyilanxin.kunpeng.bpm.model.xml.type.attribute.Attribute;
import com.anyilanxin.kunpeng.bpm.model.xml.type.child.SequenceBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.type.reference.ElementReference;

public class AssociationImpl extends ArtifactImpl implements Association {

  protected static Attribute<AssociationDirection> associationDirectionAttribute;

  protected static ElementReference<DmnElement, SourceRef> sourceRef;
  protected static ElementReference<DmnElement, TargetRef> targetRef;

  public AssociationImpl(final ModelTypeInstanceContext instanceContext) {
    super(instanceContext);
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
  public DmnElement getSource() {
    return sourceRef.getReferenceTargetElement(this);
  }

  @Override
  public void setSource(final DmnElement source) {
    sourceRef.setReferenceTargetElement(this, source);
  }

  @Override
  public DmnElement getTarget() {
    return targetRef.getReferenceTargetElement(this);
  }

  @Override
  public void setTarget(final DmnElement target) {
    targetRef.setReferenceTargetElement(this, target);
  }

  public static void registerType(final ModelBuilder modelBuilder) {
    final ModelElementTypeBuilder typeBuilder =
        modelBuilder
            .defineType(Association.class, DMN_ELEMENT_ASSOCIATION)
            .namespaceUri(LATEST_DMN_NS)
            .extendsType(Artifact.class)
            .instanceProvider(
                new ModelTypeInstanceProvider<Association>() {
                  @Override
                  public Association newInstance(final ModelTypeInstanceContext instanceContext) {
                    return new AssociationImpl(instanceContext);
                  }
                });

    associationDirectionAttribute =
        typeBuilder
            .enumAttribute(DMN_ATTRIBUTE_ASSOCIATION_DIRECTION, AssociationDirection.class)
            .defaultValue(AssociationDirection.None)
            .build();

    final SequenceBuilder sequenceBuilder = typeBuilder.sequence();

    sourceRef =
        sequenceBuilder
            .element(SourceRef.class)
            .required()
            .uriElementReference(DmnElement.class)
            .build();

    targetRef =
        sequenceBuilder
            .element(TargetRef.class)
            .required()
            .uriElementReference(DmnElement.class)
            .build();

    typeBuilder.build();
  }
}
