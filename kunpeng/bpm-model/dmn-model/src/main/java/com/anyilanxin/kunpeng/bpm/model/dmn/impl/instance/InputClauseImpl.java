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

import com.anyilanxin.kunpeng.bpm.model.dmn.instance.DmnElement;
import com.anyilanxin.kunpeng.bpm.model.dmn.instance.InputClause;
import com.anyilanxin.kunpeng.bpm.model.dmn.instance.InputExpression;
import com.anyilanxin.kunpeng.bpm.model.dmn.instance.InputValues;
import com.anyilanxin.kunpeng.bpm.model.xml.ModelBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementTypeBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;
import com.anyilanxin.kunpeng.bpm.model.xml.type.attribute.Attribute;
import com.anyilanxin.kunpeng.bpm.model.xml.type.child.ChildElement;
import com.anyilanxin.kunpeng.bpm.model.xml.type.child.SequenceBuilder;

public class InputClauseImpl extends DmnElementImpl implements InputClause {

  protected static ChildElement<InputExpression> inputExpressionChild;
  protected static ChildElement<InputValues> inputValuesChild;

  // camunda extensions
  protected static Attribute<String> kunpengInputVariableAttribute;

  public InputClauseImpl(final ModelTypeInstanceContext instanceContext) {
    super(instanceContext);
  }

  @Override
  public InputExpression getInputExpression() {
    return inputExpressionChild.getChild(this);
  }

  @Override
  public void setInputExpression(final InputExpression inputExpression) {
    inputExpressionChild.setChild(this, inputExpression);
  }

  @Override
  public InputValues getInputValues() {
    return inputValuesChild.getChild(this);
  }

  @Override
  public void setInputValues(final InputValues inputValues) {
    inputValuesChild.setChild(this, inputValues);
  }

  // kunpeng extensions

  @Override
  public String getKunpengInputVariable() {
    return kunpengInputVariableAttribute.getValue(this);
  }

  @Override
  public void setKunpengInputVariable(final String inputVariable) {
    kunpengInputVariableAttribute.setValue(this, inputVariable);
  }

  public static void registerType(final ModelBuilder modelBuilder) {
    final ModelElementTypeBuilder typeBuilder =
        modelBuilder
            .defineType(InputClause.class, DMN_ELEMENT_INPUT_CLAUSE)
            .namespaceUri(LATEST_DMN_NS)
            .extendsType(DmnElement.class)
            .instanceProvider(
                new ModelTypeInstanceProvider<InputClause>() {
                  @Override
                  public InputClause newInstance(final ModelTypeInstanceContext instanceContext) {
                    return new InputClauseImpl(instanceContext);
                  }
                });

    final SequenceBuilder sequenceBuilder = typeBuilder.sequence();

    inputExpressionChild = sequenceBuilder.element(InputExpression.class).required().build();

    inputValuesChild = sequenceBuilder.element(InputValues.class).build();

    // kunpeng extensions

    kunpengInputVariableAttribute =
        typeBuilder.stringAttribute(KUNPENG_ATTRIBUTE_INPUT_VARIABLE).namespace(KUNPENG_NS).build();

    typeBuilder.build();
  }
}
