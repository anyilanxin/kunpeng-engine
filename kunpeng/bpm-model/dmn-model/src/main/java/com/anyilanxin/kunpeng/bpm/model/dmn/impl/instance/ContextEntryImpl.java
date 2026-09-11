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

import static com.anyilanxin.kunpeng.bpm.model.dmn.impl.DmnModelConstants.DMN_ELEMENT_CONTEXT_ENTRY;
import static com.anyilanxin.kunpeng.bpm.model.dmn.impl.DmnModelConstants.LATEST_DMN_NS;

import com.anyilanxin.kunpeng.bpm.model.dmn.instance.ContextEntry;
import com.anyilanxin.kunpeng.bpm.model.dmn.instance.Expression;
import com.anyilanxin.kunpeng.bpm.model.dmn.instance.Variable;
import com.anyilanxin.kunpeng.bpm.model.xml.ModelBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementTypeBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;
import com.anyilanxin.kunpeng.bpm.model.xml.type.child.ChildElement;
import com.anyilanxin.kunpeng.bpm.model.xml.type.child.SequenceBuilder;

public class ContextEntryImpl extends DmnModelElementInstanceImpl implements ContextEntry {

  protected static ChildElement<Variable> variableChild;
  protected static ChildElement<Expression> expressionChild;

  public ContextEntryImpl(final ModelTypeInstanceContext instanceContext) {
    super(instanceContext);
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
  public Expression getExpression() {
    return expressionChild.getChild(this);
  }

  @Override
  public void setExpression(final Expression expression) {
    expressionChild.setChild(this, expression);
  }

  public static void registerType(final ModelBuilder modelBuilder) {
    final ModelElementTypeBuilder typeBuilder =
        modelBuilder
            .defineType(ContextEntry.class, DMN_ELEMENT_CONTEXT_ENTRY)
            .namespaceUri(LATEST_DMN_NS)
            .instanceProvider(
                new ModelTypeInstanceProvider<ContextEntry>() {
                  @Override
                  public ContextEntry newInstance(final ModelTypeInstanceContext instanceContext) {
                    return new ContextEntryImpl(instanceContext);
                  }
                });

    final SequenceBuilder sequenceBuilder = typeBuilder.sequence();

    variableChild = sequenceBuilder.element(Variable.class).build();

    expressionChild = sequenceBuilder.element(Expression.class).required().build();

    typeBuilder.build();
  }
}
