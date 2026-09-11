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

import com.anyilanxin.kunpeng.bpm.model.dmn.BuiltinAggregator;
import com.anyilanxin.kunpeng.bpm.model.dmn.DecisionTableOrientation;
import com.anyilanxin.kunpeng.bpm.model.dmn.HitPolicy;
import com.anyilanxin.kunpeng.bpm.model.dmn.instance.*;
import com.anyilanxin.kunpeng.bpm.model.xml.ModelBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementTypeBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;
import com.anyilanxin.kunpeng.bpm.model.xml.type.attribute.Attribute;
import com.anyilanxin.kunpeng.bpm.model.xml.type.child.ChildElementCollection;
import com.anyilanxin.kunpeng.bpm.model.xml.type.child.SequenceBuilder;
import java.util.Collection;

public class DecisionTableImpl extends ExpressionImpl implements DecisionTable {

  protected static Attribute<HitPolicy> hitPolicyAttribute;
  protected static Attribute<BuiltinAggregator> aggregationAttribute;
  protected static Attribute<DecisionTableOrientation> preferredOrientationAttribute;
  protected static Attribute<String> outputLabelAttribute;

  protected static ChildElementCollection<Input> inputCollection;
  protected static ChildElementCollection<Output> outputCollection;
  protected static ChildElementCollection<Rule> ruleCollection;

  public DecisionTableImpl(final ModelTypeInstanceContext instanceContext) {
    super(instanceContext);
  }

  @Override
  public HitPolicy getHitPolicy() {
    return hitPolicyAttribute.getValue(this);
  }

  @Override
  public void setHitPolicy(final HitPolicy hitPolicy) {
    hitPolicyAttribute.setValue(this, hitPolicy);
  }

  @Override
  public BuiltinAggregator getAggregation() {
    return aggregationAttribute.getValue(this);
  }

  @Override
  public void setAggregation(final BuiltinAggregator aggregation) {
    aggregationAttribute.setValue(this, aggregation);
  }

  @Override
  public DecisionTableOrientation getPreferredOrientation() {
    return preferredOrientationAttribute.getValue(this);
  }

  @Override
  public void setPreferredOrientation(final DecisionTableOrientation preferredOrientation) {
    preferredOrientationAttribute.setValue(this, preferredOrientation);
  }

  @Override
  public String getOutputLabel() {
    return outputLabelAttribute.getValue(this);
  }

  @Override
  public void setOutputLabel(final String outputLabel) {
    outputLabelAttribute.setValue(this, outputLabel);
  }

  @Override
  public Collection<Input> getInputs() {
    return inputCollection.get(this);
  }

  @Override
  public Collection<Output> getOutputs() {
    return outputCollection.get(this);
  }

  @Override
  public Collection<Rule> getRules() {
    return ruleCollection.get(this);
  }

  public static void registerType(final ModelBuilder modelBuilder) {
    final ModelElementTypeBuilder typeBuilder =
        modelBuilder
            .defineType(DecisionTable.class, DMN_ELEMENT_DECISION_TABLE)
            .namespaceUri(LATEST_DMN_NS)
            .extendsType(Expression.class)
            .instanceProvider(
                new ModelTypeInstanceProvider<DecisionTable>() {
                  @Override
                  public DecisionTable newInstance(final ModelTypeInstanceContext instanceContext) {
                    return new DecisionTableImpl(instanceContext);
                  }
                });

    hitPolicyAttribute =
        typeBuilder
            .namedEnumAttribute(DMN_ATTRIBUTE_HIT_POLICY, HitPolicy.class)
            .defaultValue(HitPolicy.UNIQUE)
            .build();

    aggregationAttribute =
        typeBuilder.enumAttribute(DMN_ATTRIBUTE_AGGREGATION, BuiltinAggregator.class).build();

    preferredOrientationAttribute =
        typeBuilder
            .namedEnumAttribute(DMN_ATTRIBUTE_PREFERRED_ORIENTATION, DecisionTableOrientation.class)
            .defaultValue(DecisionTableOrientation.Rule_as_Row)
            .build();

    outputLabelAttribute = typeBuilder.stringAttribute(DMN_ATTRIBUTE_OUTPUT_LABEL).build();

    final SequenceBuilder sequenceBuilder = typeBuilder.sequence();

    inputCollection = sequenceBuilder.elementCollection(Input.class).build();

    outputCollection = sequenceBuilder.elementCollection(Output.class).required().build();

    ruleCollection = sequenceBuilder.elementCollection(Rule.class).build();

    typeBuilder.build();
  }
}
