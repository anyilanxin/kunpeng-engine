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
import com.anyilanxin.kunpeng.bpm.model.xml.type.reference.ElementReferenceCollection;
import java.util.Collection;

public class DecisionImpl extends DrgElementImpl implements Decision {

  protected static ChildElement<Question> questionChild;
  protected static ChildElement<AllowedAnswers> allowedAnswersChild;
  protected static ChildElement<Variable> variableChild;
  protected static ChildElementCollection<InformationRequirement> informationRequirementCollection;
  protected static ChildElementCollection<KnowledgeRequirement> knowledgeRequirementCollection;
  protected static ChildElementCollection<AuthorityRequirement> authorityRequirementCollection;
  protected static ChildElementCollection<SupportedObjectiveReference>
      supportedObjectiveChildElementCollection;
  protected static ElementReferenceCollection<
          PerformanceIndicator, ImpactedPerformanceIndicatorReference>
      impactedPerformanceIndicatorRefCollection;
  protected static ElementReferenceCollection<OrganizationUnit, DecisionMakerReference>
      decisionMakerRefCollection;
  protected static ElementReferenceCollection<OrganizationUnit, DecisionOwnerReference>
      decisionOwnerRefCollection;
  protected static ChildElementCollection<UsingProcessReference> usingProcessCollection;
  protected static ChildElementCollection<UsingTaskReference> usingTaskCollection;
  protected static ChildElement<Expression> expressionChild;

  // kunpeng extensions
  protected static Attribute<String> kunpengHistoryTimeToLiveAttribute;
  protected static Attribute<String> kunpengVersionTag;

  public DecisionImpl(final ModelTypeInstanceContext instanceContext) {
    super(instanceContext);
  }

  @Override
  public Question getQuestion() {
    return questionChild.getChild(this);
  }

  @Override
  public void setQuestion(final Question question) {
    questionChild.setChild(this, question);
  }

  @Override
  public AllowedAnswers getAllowedAnswers() {
    return allowedAnswersChild.getChild(this);
  }

  @Override
  public void setAllowedAnswers(final AllowedAnswers allowedAnswers) {
    allowedAnswersChild.setChild(this, allowedAnswers);
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
  public Collection<InformationRequirement> getInformationRequirements() {
    return informationRequirementCollection.get(this);
  }

  @Override
  public Collection<KnowledgeRequirement> getKnowledgeRequirements() {
    return knowledgeRequirementCollection.get(this);
  }

  @Override
  public Collection<AuthorityRequirement> getAuthorityRequirements() {
    return authorityRequirementCollection.get(this);
  }

  @Override
  public Collection<SupportedObjectiveReference> getSupportedObjectiveReferences() {
    return supportedObjectiveChildElementCollection.get(this);
  }

  @Override
  public Collection<PerformanceIndicator> getImpactedPerformanceIndicators() {
    return impactedPerformanceIndicatorRefCollection.getReferenceTargetElements(this);
  }

  @Override
  public Collection<OrganizationUnit> getDecisionMakers() {
    return decisionMakerRefCollection.getReferenceTargetElements(this);
  }

  @Override
  public Collection<OrganizationUnit> getDecisionOwners() {
    return decisionOwnerRefCollection.getReferenceTargetElements(this);
  }

  @Override
  public Collection<UsingProcessReference> getUsingProcessReferences() {
    return usingProcessCollection.get(this);
  }

  @Override
  public Collection<UsingTaskReference> getUsingTaskReferences() {
    return usingTaskCollection.get(this);
  }

  @Override
  public Expression getExpression() {
    return expressionChild.getChild(this);
  }

  @Override
  public void setExpression(final Expression expression) {
    expressionChild.setChild(this, expression);
  }

  // kunpeng extensions

  @Override
  public String getKunpengHistoryTimeToLiveString() {
    return kunpengHistoryTimeToLiveAttribute.getValue(this);
  }

  @Override
  public void setKunpengHistoryTimeToLiveString(final String historyTimeToLive) {
    kunpengHistoryTimeToLiveAttribute.setValue(this, historyTimeToLive);
  }

  @Override
  public String getVersionTag() {
    return kunpengVersionTag.getValue(this);
  }

  @Override
  public void setVersionTag(final String inputVariable) {
    kunpengVersionTag.setValue(this, inputVariable);
  }

  public static void registerType(final ModelBuilder modelBuilder) {
    final ModelElementTypeBuilder typeBuilder =
        modelBuilder
            .defineType(Decision.class, DMN_ELEMENT_DECISION)
            .namespaceUri(LATEST_DMN_NS)
            .extendsType(DrgElement.class)
            .instanceProvider(
                new ModelTypeInstanceProvider<Decision>() {
                  @Override
                  public Decision newInstance(final ModelTypeInstanceContext instanceContext) {
                    return new DecisionImpl(instanceContext);
                  }
                });

    final SequenceBuilder sequenceBuilder = typeBuilder.sequence();

    questionChild = sequenceBuilder.element(Question.class).build();

    allowedAnswersChild = sequenceBuilder.element(AllowedAnswers.class).build();

    variableChild = sequenceBuilder.element(Variable.class).build();

    informationRequirementCollection =
        sequenceBuilder.elementCollection(InformationRequirement.class).build();

    knowledgeRequirementCollection =
        sequenceBuilder.elementCollection(KnowledgeRequirement.class).build();

    authorityRequirementCollection =
        sequenceBuilder.elementCollection(AuthorityRequirement.class).build();

    supportedObjectiveChildElementCollection =
        sequenceBuilder.elementCollection(SupportedObjectiveReference.class).build();

    impactedPerformanceIndicatorRefCollection =
        sequenceBuilder
            .elementCollection(ImpactedPerformanceIndicatorReference.class)
            .uriElementReferenceCollection(PerformanceIndicator.class)
            .build();

    decisionMakerRefCollection =
        sequenceBuilder
            .elementCollection(DecisionMakerReference.class)
            .uriElementReferenceCollection(OrganizationUnit.class)
            .build();

    decisionOwnerRefCollection =
        sequenceBuilder
            .elementCollection(DecisionOwnerReference.class)
            .uriElementReferenceCollection(OrganizationUnit.class)
            .build();

    usingProcessCollection = sequenceBuilder.elementCollection(UsingProcessReference.class).build();

    usingTaskCollection = sequenceBuilder.elementCollection(UsingTaskReference.class).build();

    expressionChild = sequenceBuilder.element(Expression.class).build();

    // kunpeng extensions

    kunpengHistoryTimeToLiveAttribute =
        typeBuilder
            .stringAttribute(KUNPENG_ATTRIBUTE_HISTORY_TIME_TO_LIVE)
            .namespace(KUNPENG_NS)
            .build();

    kunpengVersionTag =
        typeBuilder.stringAttribute(KUNPENG_ATTRIBUTE_VERSION_TAG).namespace(KUNPENG_NS).build();

    typeBuilder.build();
  }
}
