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

import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.CorrelationProperty;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.CorrelationPropertyRetrievalExpression;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.ItemDefinition;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.RootElement;
import com.anyilanxin.kunpeng.bpm.model.xml.ModelBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementTypeBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;
import com.anyilanxin.kunpeng.bpm.model.xml.type.attribute.Attribute;
import com.anyilanxin.kunpeng.bpm.model.xml.type.child.ChildElementCollection;
import com.anyilanxin.kunpeng.bpm.model.xml.type.child.SequenceBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.type.reference.AttributeReference;
import java.util.Collection;

/**
 * The BPMN correlationProperty element
 *
 * @author Sebastian Menski
 */
public class CorrelationPropertyImpl extends RootElementImpl implements CorrelationProperty {

  protected static Attribute<String> nameAttribute;
  protected static AttributeReference<ItemDefinition> typeAttribute;
  protected static ChildElementCollection<CorrelationPropertyRetrievalExpression>
      correlationPropertyRetrievalExpressionCollection;

  public CorrelationPropertyImpl(final ModelTypeInstanceContext context) {
    super(context);
  }

  public static void registerType(final ModelBuilder modelBuilder) {
    final ModelElementTypeBuilder typeBuilder;
    typeBuilder =
        modelBuilder
            .defineType(CorrelationProperty.class, BPMN_ELEMENT_CORRELATION_PROPERTY)
            .namespaceUri(BPMN20_NS)
            .extendsType(RootElement.class)
            .instanceProvider(
                new ModelTypeInstanceProvider<CorrelationProperty>() {
                  @Override
                  public CorrelationProperty newInstance(
                      final ModelTypeInstanceContext instanceContext) {
                    return new CorrelationPropertyImpl(instanceContext);
                  }
                });

    nameAttribute = typeBuilder.stringAttribute(BPMN_ATTRIBUTE_NAME).build();

    typeAttribute =
        typeBuilder
            .stringAttribute(BPMN_ATTRIBUTE_TYPE)
            .qNameAttributeReference(ItemDefinition.class)
            .build();

    final SequenceBuilder sequenceBuilder = typeBuilder.sequence();

    correlationPropertyRetrievalExpressionCollection =
        sequenceBuilder
            .elementCollection(CorrelationPropertyRetrievalExpression.class)
            .required()
            .build();

    typeBuilder.build();
  }

  @Override
  public String getName() {
    return nameAttribute.getValue(this);
  }

  @Override
  public void setName(final String name) {
    nameAttribute.setValue(this, name);
  }

  @Override
  public ItemDefinition getType() {
    return typeAttribute.getReferenceTargetElement(this);
  }

  @Override
  public void setType(final ItemDefinition type) {
    typeAttribute.setReferenceTargetElement(this, type);
  }

  @Override
  public Collection<CorrelationPropertyRetrievalExpression>
      getCorrelationPropertyRetrievalExpressions() {
    return correlationPropertyRetrievalExpressionCollection.get(this);
  }
}
