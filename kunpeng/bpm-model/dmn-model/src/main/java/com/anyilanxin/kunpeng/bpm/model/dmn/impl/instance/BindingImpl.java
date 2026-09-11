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

import static com.anyilanxin.kunpeng.bpm.model.dmn.impl.DmnModelConstants.DMN_ELEMENT_BINDING;
import static com.anyilanxin.kunpeng.bpm.model.dmn.impl.DmnModelConstants.LATEST_DMN_NS;

import com.anyilanxin.kunpeng.bpm.model.dmn.instance.Binding;
import com.anyilanxin.kunpeng.bpm.model.dmn.instance.Expression;
import com.anyilanxin.kunpeng.bpm.model.dmn.instance.Parameter;
import com.anyilanxin.kunpeng.bpm.model.xml.ModelBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementTypeBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;
import com.anyilanxin.kunpeng.bpm.model.xml.type.child.ChildElement;
import com.anyilanxin.kunpeng.bpm.model.xml.type.child.SequenceBuilder;

public class BindingImpl extends DmnModelElementInstanceImpl implements Binding {

  protected static ChildElement<Parameter> parameterChild;
  protected static ChildElement<Expression> expressionChild;

  public BindingImpl(final ModelTypeInstanceContext instanceContext) {
    super(instanceContext);
  }

  @Override
  public Parameter getParameter() {
    return parameterChild.getChild(this);
  }

  @Override
  public void setParameter(final Parameter parameter) {
    parameterChild.setChild(this, parameter);
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
            .defineType(Binding.class, DMN_ELEMENT_BINDING)
            .namespaceUri(LATEST_DMN_NS)
            .instanceProvider(
                new ModelTypeInstanceProvider<Binding>() {
                  @Override
                  public Binding newInstance(final ModelTypeInstanceContext instanceContext) {
                    return new BindingImpl(instanceContext);
                  }
                });

    final SequenceBuilder sequenceBuilder = typeBuilder.sequence();

    parameterChild = sequenceBuilder.element(Parameter.class).required().build();

    expressionChild = sequenceBuilder.element(Expression.class).build();

    typeBuilder.build();
  }
}
