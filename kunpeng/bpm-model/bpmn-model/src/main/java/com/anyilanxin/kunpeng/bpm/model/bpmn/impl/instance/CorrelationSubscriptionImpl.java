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
import static com.anyilanxin.kunpeng.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ATTRIBUTE_CORRELATION_KEY_REF;
import static com.anyilanxin.kunpeng.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ELEMENT_CORRELATION_SUBSCRIPTION;

import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.BaseElement;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.CorrelationKey;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.CorrelationPropertyBinding;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.CorrelationSubscription;
import java.util.Collection;
import com.anyilanxin.kunpeng.bpm.model.xml.ModelBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementTypeBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;
import com.anyilanxin.kunpeng.bpm.model.xml.type.child.ChildElementCollection;
import com.anyilanxin.kunpeng.bpm.model.xml.type.child.SequenceBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.type.reference.AttributeReference;

/**
 * The BPMN correlationSubscription element
 *
 * @author Sebastian Menski
 */
public class CorrelationSubscriptionImpl extends BaseElementImpl
    implements CorrelationSubscription {

  protected static AttributeReference<CorrelationKey> correlationKeyAttribute;
  protected static ChildElementCollection<CorrelationPropertyBinding>
      correlationPropertyBindingCollection;

  public CorrelationSubscriptionImpl(final ModelTypeInstanceContext instanceContext) {
    super(instanceContext);
  }

  public static void registerType(final ModelBuilder modelBuilder) {
    final ModelElementTypeBuilder typeBuilder =
        modelBuilder
            .defineType(CorrelationSubscription.class, BPMN_ELEMENT_CORRELATION_SUBSCRIPTION)
            .namespaceUri(BPMN20_NS)
            .extendsType(BaseElement.class)
            .instanceProvider(
                new ModelTypeInstanceProvider<CorrelationSubscription>() {
                  @Override
                  public CorrelationSubscription newInstance(
                      final ModelTypeInstanceContext instanceContext) {
                    return new CorrelationSubscriptionImpl(instanceContext);
                  }
                });

    correlationKeyAttribute =
        typeBuilder
            .stringAttribute(BPMN_ATTRIBUTE_CORRELATION_KEY_REF)
            .required()
            .qNameAttributeReference(CorrelationKey.class)
            .build();

    final SequenceBuilder sequenceBuilder = typeBuilder.sequence();

    correlationPropertyBindingCollection =
        sequenceBuilder.elementCollection(CorrelationPropertyBinding.class).build();

    typeBuilder.build();
  }

  @Override
  public CorrelationKey getCorrelationKey() {
    return correlationKeyAttribute.getReferenceTargetElement(this);
  }

  @Override
  public void setCorrelationKey(final CorrelationKey correlationKey) {
    correlationKeyAttribute.setReferenceTargetElement(this, correlationKey);
  }

  @Override
  public Collection<CorrelationPropertyBinding> getCorrelationPropertyBindings() {
    return correlationPropertyBindingCollection.get(this);
  }
}
