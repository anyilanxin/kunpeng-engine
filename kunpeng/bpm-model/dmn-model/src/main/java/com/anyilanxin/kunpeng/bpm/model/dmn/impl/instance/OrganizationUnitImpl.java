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

import static com.anyilanxin.kunpeng.bpm.model.dmn.impl.DmnModelConstants.LATEST_DMN_NS;
import static com.anyilanxin.kunpeng.bpm.model.dmn.impl.DmnModelConstants.DMN_ELEMENT_ORGANIZATION_UNIT;

import java.util.Collection;

import com.anyilanxin.kunpeng.bpm.model.dmn.instance.BusinessContextElement;
import com.anyilanxin.kunpeng.bpm.model.dmn.instance.Decision;
import com.anyilanxin.kunpeng.bpm.model.dmn.instance.DecisionMadeReference;
import com.anyilanxin.kunpeng.bpm.model.dmn.instance.DecisionOwnedReference;
import com.anyilanxin.kunpeng.bpm.model.dmn.instance.OrganizationUnit;
import com.anyilanxin.kunpeng.bpm.model.xml.ModelBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementTypeBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;
import com.anyilanxin.kunpeng.bpm.model.xml.type.child.SequenceBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.type.reference.ElementReferenceCollection;

public class OrganizationUnitImpl extends BusinessContextElementImpl implements OrganizationUnit {

  protected static ElementReferenceCollection<Decision, DecisionMadeReference> decisionDecisionMadeRefCollection;
  protected static ElementReferenceCollection<Decision, DecisionOwnedReference> decisionDecisionOwnedRefCollection;

  public OrganizationUnitImpl(ModelTypeInstanceContext instanceContext) {
    super(instanceContext);
  }

  public Collection<Decision> getDecisionsMade() {
    return decisionDecisionMadeRefCollection.getReferenceTargetElements(this);
  }

  public Collection<Decision> getDecisionsOwned() {
    return decisionDecisionOwnedRefCollection.getReferenceTargetElements(this);
  }

  public static void registerType(ModelBuilder modelBuilder) {
    ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(OrganizationUnit.class, DMN_ELEMENT_ORGANIZATION_UNIT)
      .namespaceUri(LATEST_DMN_NS)
      .extendsType(BusinessContextElement.class)
      .instanceProvider(new ModelTypeInstanceProvider<OrganizationUnit>() {
        public OrganizationUnit newInstance(ModelTypeInstanceContext instanceContext) {
          return new OrganizationUnitImpl(instanceContext);
        }
      });

    SequenceBuilder sequenceBuilder = typeBuilder.sequence();

    decisionDecisionMadeRefCollection = sequenceBuilder.elementCollection(DecisionMadeReference.class)
      .uriElementReferenceCollection(Decision.class)
      .build();

    decisionDecisionOwnedRefCollection = sequenceBuilder.elementCollection(DecisionOwnedReference.class)
      .uriElementReferenceCollection(Decision.class)
      .build();

    typeBuilder.build();
  }

}
