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

import static com.anyilanxin.kunpeng.bpm.model.dmn.impl.DmnModelConstants.DMN_ELEMENT_BUSINESS_KNOWLEDGE_MODEL;
import static com.anyilanxin.kunpeng.bpm.model.dmn.impl.DmnModelConstants.LATEST_DMN_NS;

import com.anyilanxin.kunpeng.bpm.model.dmn.instance.*;
import com.anyilanxin.kunpeng.bpm.model.xml.ModelBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementTypeBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;
import com.anyilanxin.kunpeng.bpm.model.xml.type.child.ChildElement;
import com.anyilanxin.kunpeng.bpm.model.xml.type.child.ChildElementCollection;
import com.anyilanxin.kunpeng.bpm.model.xml.type.child.SequenceBuilder;
import java.util.Collection;

public class BusinessKnowledgeModelImpl extends DrgElementImpl implements BusinessKnowledgeModel {

  protected static ChildElement<EncapsulatedLogic> encapsulatedLogicChild;
  protected static ChildElement<Variable> variableChild;
  protected static ChildElementCollection<KnowledgeRequirement> knowledgeRequirementCollection;
  protected static ChildElementCollection<AuthorityRequirement> authorityRequirementCollection;

  public BusinessKnowledgeModelImpl(final ModelTypeInstanceContext instanceContext) {
    super(instanceContext);
  }

  @Override
  public EncapsulatedLogic getEncapsulatedLogic() {
    return encapsulatedLogicChild.getChild(this);
  }

  @Override
  public void setEncapsulatedLogic(final EncapsulatedLogic encapsulatedLogic) {
    encapsulatedLogicChild.setChild(this, encapsulatedLogic);
  }

  @Override
  public Variable getVariable() {
    return variableChild.getChild(this);
  }

  @Override
  public void setVariable(final Variable variable) {
    variableChild.setChild(this, variable);
  }

  @Override
  public Collection<KnowledgeRequirement> getKnowledgeRequirement() {
    return knowledgeRequirementCollection.get(this);
  }

  @Override
  public Collection<AuthorityRequirement> getAuthorityRequirement() {
    return authorityRequirementCollection.get(this);
  }

  public static void registerType(final ModelBuilder modelBuilder) {
    final ModelElementTypeBuilder typeBuilder =
        modelBuilder
            .defineType(BusinessKnowledgeModel.class, DMN_ELEMENT_BUSINESS_KNOWLEDGE_MODEL)
            .namespaceUri(LATEST_DMN_NS)
            .extendsType(DrgElement.class)
            .instanceProvider(
                new ModelTypeInstanceProvider<BusinessKnowledgeModel>() {
                  @Override
                  public BusinessKnowledgeModel newInstance(
                      final ModelTypeInstanceContext instanceContext) {
                    return new BusinessKnowledgeModelImpl(instanceContext);
                  }
                });

    final SequenceBuilder sequenceBuilder = typeBuilder.sequence();

    encapsulatedLogicChild = sequenceBuilder.element(EncapsulatedLogic.class).build();

    variableChild = sequenceBuilder.element(Variable.class).build();

    knowledgeRequirementCollection =
        sequenceBuilder.elementCollection(KnowledgeRequirement.class).build();

    authorityRequirementCollection =
        sequenceBuilder.elementCollection(AuthorityRequirement.class).build();

    typeBuilder.build();
  }
}
