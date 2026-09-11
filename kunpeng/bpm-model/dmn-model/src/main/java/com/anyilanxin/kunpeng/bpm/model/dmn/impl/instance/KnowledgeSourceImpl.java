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

import com.anyilanxin.kunpeng.bpm.model.dmn.instance.*;
import com.anyilanxin.kunpeng.bpm.model.xml.ModelBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementTypeBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;
import com.anyilanxin.kunpeng.bpm.model.xml.type.attribute.Attribute;
import com.anyilanxin.kunpeng.bpm.model.xml.type.child.ChildElement;
import com.anyilanxin.kunpeng.bpm.model.xml.type.child.ChildElementCollection;
import com.anyilanxin.kunpeng.bpm.model.xml.type.child.SequenceBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.type.reference.ElementReference;
import java.util.Collection;

public class KnowledgeSourceImpl extends DrgElementImpl implements KnowledgeSource {

  protected static Attribute<String> locationUriAttribute;

  protected static ChildElementCollection<AuthorityRequirement> authorityRequirementCollection;
  protected static ChildElement<Type> typeChild;
  protected static ElementReference<OrganizationUnit, OwnerReference> ownerRef;

  public KnowledgeSourceImpl(final ModelTypeInstanceContext instanceContext) {
    super(instanceContext);
  }

  @Override
  public String getLocationUri() {
    return locationUriAttribute.getValue(this);
  }

  @Override
  public void setLocationUri(final String locationUri) {
    locationUriAttribute.setValue(this, locationUri);
  }

  @Override
  public Collection<AuthorityRequirement> getAuthorityRequirement() {
    return authorityRequirementCollection.get(this);
  }

  @Override
  public Type getType() {
    return typeChild.getChild(this);
  }

  @Override
  public void setType(final Type type) {
    typeChild.setChild(this, type);
  }

  @Override
  public OrganizationUnit getOwner() {
    return ownerRef.getReferenceTargetElement(this);
  }

  @Override
  public void setOwner(final OrganizationUnit owner) {
    ownerRef.setReferenceTargetElement(this, owner);
  }

  public static void registerType(final ModelBuilder modelBuilder) {
    final ModelElementTypeBuilder typeBuilder =
        modelBuilder
            .defineType(KnowledgeSource.class, DMN_ELEMENT_KNOWLEDGE_SOURCE)
            .namespaceUri(LATEST_DMN_NS)
            .extendsType(DrgElement.class)
            .instanceProvider(
                new ModelTypeInstanceProvider<KnowledgeSource>() {
                  @Override
                  public KnowledgeSource newInstance(
                      final ModelTypeInstanceContext instanceContext) {
                    return new KnowledgeSourceImpl(instanceContext);
                  }
                });

    locationUriAttribute = typeBuilder.stringAttribute(DMN_ATTRIBUTE_LOCATION_URI).build();

    final SequenceBuilder sequenceBuilder = typeBuilder.sequence();

    authorityRequirementCollection =
        sequenceBuilder.elementCollection(AuthorityRequirement.class).build();

    typeChild = sequenceBuilder.element(Type.class).build();

    ownerRef =
        sequenceBuilder
            .element(OwnerReference.class)
            .uriElementReference(OrganizationUnit.class)
            .build();

    typeBuilder.build();
  }
}
