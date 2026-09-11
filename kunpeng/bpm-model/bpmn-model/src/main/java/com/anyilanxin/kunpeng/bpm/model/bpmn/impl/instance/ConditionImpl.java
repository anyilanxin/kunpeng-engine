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

import static com.anyilanxin.kunpeng.bpm.model.bpmn.impl.BpmnModelConstants.BPMN20_NS;
import static com.anyilanxin.kunpeng.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ELEMENT_CONDITION;
import static com.anyilanxin.kunpeng.bpm.model.bpmn.impl.BpmnModelConstants.XSI_ATTRIBUTE_TYPE;
import static com.anyilanxin.kunpeng.bpm.model.bpmn.impl.BpmnModelConstants.XSI_NS;

import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.Condition;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.FormalExpression;
import com.anyilanxin.kunpeng.bpm.model.xml.ModelBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementTypeBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;
import com.anyilanxin.kunpeng.bpm.model.xml.type.attribute.Attribute;

/**
 * The BPMN condition element of the BPMN tConditionalEventDefinition type
 *
 * @author Sebastian Menski
 */
public class ConditionImpl extends FormalExpressionImpl implements Condition {

  protected static Attribute<String> typeAttribute;

  public ConditionImpl(final ModelTypeInstanceContext instanceContext) {
    super(instanceContext);
  }

  public static void registerType(final ModelBuilder modelBuilder) {
    final ModelElementTypeBuilder typeBuilder =
        modelBuilder
            .defineType(Condition.class, BPMN_ELEMENT_CONDITION)
            .namespaceUri(BPMN20_NS)
            .extendsType(FormalExpression.class)
            .instanceProvider((ModelTypeInstanceProvider<Condition>) ConditionImpl::new);

    typeAttribute =
        typeBuilder
            .stringAttribute(XSI_ATTRIBUTE_TYPE)
            .namespace(XSI_NS)
            .defaultValue("tFormalExpression")
            .build();

    typeBuilder.build();
  }

  @Override
  public String getType() {
    return typeAttribute.getValue(this);
  }

  @Override
  public void setType(final String type) {
    typeAttribute.setValue(this, type);
  }
}
