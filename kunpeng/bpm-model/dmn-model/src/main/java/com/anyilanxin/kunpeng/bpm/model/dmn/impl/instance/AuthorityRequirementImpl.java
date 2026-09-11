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

import static com.anyilanxin.kunpeng.bpm.model.dmn.impl.DmnModelConstants.DMN_ELEMENT_AUTHORITY_REQUIREMENT;
import static com.anyilanxin.kunpeng.bpm.model.dmn.impl.DmnModelConstants.LATEST_DMN_NS;

import com.anyilanxin.kunpeng.bpm.model.dmn.instance.*;
import com.anyilanxin.kunpeng.bpm.model.xml.ModelBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementTypeBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;
import com.anyilanxin.kunpeng.bpm.model.xml.type.child.SequenceBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.type.reference.ElementReference;

public class AuthorityRequirementImpl extends DmnModelElementInstanceImpl
    implements AuthorityRequirement {

  protected static ElementReference<Decision, RequiredDecisionReference> requiredDecisionRef;
  protected static ElementReference<InputData, RequiredInputReference> requiredInputRef;
  protected static ElementReference<KnowledgeSource, RequiredAuthorityReference>
      requiredAuthorityRef;

  public AuthorityRequirementImpl(final ModelTypeInstanceContext instanceContext) {
    super(instanceContext);
  }

  @Override
  public Decision getRequiredDecision() {
    return requiredDecisionRef.getReferenceTargetElement(this);
  }

  @Override
  public void setRequiredDecision(final Decision requiredDecision) {
    requiredDecisionRef.setReferenceTargetElement(this, requiredDecision);
  }

  @Override
  public InputData getRequiredInput() {
    return requiredInputRef.getReferenceTargetElement(this);
  }

  @Override
  public void setRequiredInput(final InputData requiredInput) {
    requiredInputRef.setReferenceTargetElement(this, requiredInput);
  }

  @Override
  public KnowledgeSource getRequiredAuthority() {
    return requiredAuthorityRef.getReferenceTargetElement(this);
  }

  @Override
  public void setRequiredAuthority(final KnowledgeSource requiredAuthority) {
    requiredAuthorityRef.setReferenceTargetElement(this, requiredAuthority);
  }

  public static void registerType(final ModelBuilder modelBuilder) {
    final ModelElementTypeBuilder typeBuilder =
        modelBuilder
            .defineType(AuthorityRequirement.class, DMN_ELEMENT_AUTHORITY_REQUIREMENT)
            .namespaceUri(LATEST_DMN_NS)
            .instanceProvider(
                new ModelTypeInstanceProvider<AuthorityRequirement>() {
                  @Override
                  public AuthorityRequirement newInstance(
                      final ModelTypeInstanceContext instanceContext) {
                    return new AuthorityRequirementImpl(instanceContext);
                  }
                });

    final SequenceBuilder sequenceBuilder = typeBuilder.sequence();

    requiredDecisionRef =
        sequenceBuilder
            .element(RequiredDecisionReference.class)
            .uriElementReference(Decision.class)
            .build();

    requiredInputRef =
        sequenceBuilder
            .element(RequiredInputReference.class)
            .uriElementReference(InputData.class)
            .build();

    requiredAuthorityRef =
        sequenceBuilder
            .element(RequiredAuthorityReference.class)
            .uriElementReference(KnowledgeSource.class)
            .build();

    typeBuilder.build();
  }
}
