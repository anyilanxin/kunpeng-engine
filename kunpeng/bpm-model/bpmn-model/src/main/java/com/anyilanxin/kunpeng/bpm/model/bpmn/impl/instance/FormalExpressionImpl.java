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

import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.Expression;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.FormalExpression;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.ItemDefinition;
import com.anyilanxin.kunpeng.bpm.model.xml.ModelBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementTypeBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;
import com.anyilanxin.kunpeng.bpm.model.xml.type.attribute.Attribute;
import com.anyilanxin.kunpeng.bpm.model.xml.type.reference.AttributeReference;

/**
 * The BPMN formalExpression element
 *
 * @author Sebastian Menski
 */
public class FormalExpressionImpl extends ExpressionImpl implements FormalExpression {

  protected static Attribute<String> languageAttribute;
  protected static AttributeReference<ItemDefinition> evaluatesToTypeRefAttribute;

  public FormalExpressionImpl(final ModelTypeInstanceContext instanceContext) {
    super(instanceContext);
  }

  public static void registerType(final ModelBuilder modelBuilder) {
    final ModelElementTypeBuilder typeBuilder =
        modelBuilder
            .defineType(FormalExpression.class, BPMN_ELEMENT_FORMAL_EXPRESSION)
            .namespaceUri(BPMN20_NS)
            .extendsType(Expression.class)
            .instanceProvider(
                new ModelTypeInstanceProvider<FormalExpression>() {
                  @Override
                  public FormalExpression newInstance(
                      final ModelTypeInstanceContext instanceContext) {
                    return new FormalExpressionImpl(instanceContext);
                  }
                });

    languageAttribute = typeBuilder.stringAttribute(BPMN_ATTRIBUTE_LANGUAGE).build();

    evaluatesToTypeRefAttribute =
        typeBuilder
            .stringAttribute(BPMN_ATTRIBUTE_EVALUATES_TO_TYPE_REF)
            .qNameAttributeReference(ItemDefinition.class)
            .build();

    typeBuilder.build();
  }

  @Override
  public String getLanguage() {
    return languageAttribute.getValue(this);
  }

  @Override
  public void setLanguage(final String language) {
    languageAttribute.setValue(this, language);
  }

  @Override
  public ItemDefinition getEvaluatesToType() {
    return evaluatesToTypeRefAttribute.getReferenceTargetElement(this);
  }

  @Override
  public void setEvaluatesToType(final ItemDefinition evaluatesToType) {
    evaluatesToTypeRefAttribute.setReferenceTargetElement(this, evaluatesToType);
  }
}
