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
package com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.zeebe;

import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.BpmnModelConstants;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.ZeebeConstants;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance.BpmnModelElementInstanceImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.zeebe.ZeebeScript;
import com.anyilanxin.kunpeng.bpm.model.xml.ModelBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementTypeBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.type.attribute.Attribute;

public class ZeebeScriptImpl extends BpmnModelElementInstanceImpl implements ZeebeScript {

  private static Attribute<String> expressionAttribute;
  private static Attribute<String> resultVariableAttribute;

  public ZeebeScriptImpl(final ModelTypeInstanceContext instanceContext) {
    super(instanceContext);
  }

  public static void registerType(final ModelBuilder modelBuilder) {
    final ModelElementTypeBuilder typeBuilder =
        modelBuilder
            .defineType(ZeebeScript.class, ZeebeConstants.ELEMENT_SCRIPT)
            .namespaceUri(BpmnModelConstants.ZEEBE_NS)
            .instanceProvider(ZeebeScriptImpl::new);

    expressionAttribute =
        typeBuilder
            .stringAttribute(ZeebeConstants.ATTRIBUTE_EXPRESSION)
            .namespace(BpmnModelConstants.ZEEBE_NS)
            .required()
            .build();

    resultVariableAttribute =
        typeBuilder
            .stringAttribute(ZeebeConstants.ATTRIBUTE_RESULT_VARIABLE)
            .namespace(BpmnModelConstants.ZEEBE_NS)
            .required()
            .build();

    typeBuilder.build();
  }

  @Override
  public String getExpression() {
    return expressionAttribute.getValue(this);
  }

  @Override
  public void setExpression(final String expression) {
    expressionAttribute.setValue(this, expression);
  }

  @Override
  public String getResultVariable() {
    return resultVariableAttribute.getValue(this);
  }

  @Override
  public void setResultVariable(final String resultVariable) {
    resultVariableAttribute.setValue(this, resultVariable);
  }
}
