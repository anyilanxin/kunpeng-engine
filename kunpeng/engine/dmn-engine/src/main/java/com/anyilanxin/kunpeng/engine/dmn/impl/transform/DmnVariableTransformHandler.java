/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * Copyright © 2026 anyilanxin zxh(anyilanxin@aliyun.com)
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
package com.anyilanxin.kunpeng.engine.dmn.impl.transform;

import static com.anyilanxin.kunpeng.engine.dmn.impl.transform.DmnExpressionTransformHelper.createTypeDefinition;

import com.anyilanxin.kunpeng.engine.dmn.impl.DmnVariableImpl;
import com.anyilanxin.kunpeng.engine.dmn.impl.spi.transform.DmnElementTransformContext;
import com.anyilanxin.kunpeng.engine.dmn.impl.spi.transform.DmnElementTransformHandler;
import com.anyilanxin.kunpeng.engine.dmn.impl.spi.type.DmnTypeDefinition;
import com.anyilanxin.kunpeng.bpm.model.dmn.instance.Variable;

public class DmnVariableTransformHandler
    implements DmnElementTransformHandler<Variable, DmnVariableImpl> {

  public DmnVariableImpl handleElement(final DmnElementTransformContext context, final Variable variable) {
    return createFromVariable(context, variable);
  }

  protected DmnVariableImpl createFromVariable(
    final DmnElementTransformContext context, final Variable variable) {
    final DmnVariableImpl dmnVariable = createDmnElement(context, variable);

    dmnVariable.setId(variable.getId());
    dmnVariable.setName(variable.getName());

    final DmnTypeDefinition typeDefinition = createTypeDefinition(context, variable);
    dmnVariable.setTypeDefinition(typeDefinition);

    return dmnVariable;
  }

  protected DmnVariableImpl createDmnElement(
    final DmnElementTransformContext context, final Variable variable) {
    return new DmnVariableImpl();
  }
}
