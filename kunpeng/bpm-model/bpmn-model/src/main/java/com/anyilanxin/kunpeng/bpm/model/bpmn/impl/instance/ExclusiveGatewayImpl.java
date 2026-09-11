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
import static com.anyilanxin.kunpeng.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ATTRIBUTE_DEFAULT;
import static com.anyilanxin.kunpeng.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ELEMENT_EXCLUSIVE_GATEWAY;

import com.anyilanxin.kunpeng.bpm.model.bpmn.BpmnModelInstance;
import com.anyilanxin.kunpeng.bpm.model.bpmn.builder.ExclusiveGatewayBuilder;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.ExclusiveGateway;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.Gateway;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.SequenceFlow;
import com.anyilanxin.kunpeng.bpm.model.xml.ModelBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementTypeBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;
import com.anyilanxin.kunpeng.bpm.model.xml.type.reference.AttributeReference;

/**
 * The BPMN exclusiveGateway element
 *
 * @author Sebastian Menski
 */
public class ExclusiveGatewayImpl extends GatewayImpl implements ExclusiveGateway {

  protected static AttributeReference<SequenceFlow> defaultAttribute;

  public ExclusiveGatewayImpl(final ModelTypeInstanceContext context) {
    super(context);
  }

  public static void registerType(final ModelBuilder modelBuilder) {
    final ModelElementTypeBuilder typeBuilder =
        modelBuilder
            .defineType(ExclusiveGateway.class, BPMN_ELEMENT_EXCLUSIVE_GATEWAY)
            .namespaceUri(BPMN20_NS)
            .extendsType(Gateway.class)
            .instanceProvider(
                new ModelTypeInstanceProvider<ExclusiveGateway>() {
                  @Override
                  public ExclusiveGateway newInstance(
                      final ModelTypeInstanceContext instanceContext) {
                    return new ExclusiveGatewayImpl(instanceContext);
                  }
                });

    defaultAttribute =
        typeBuilder
            .stringAttribute(BPMN_ATTRIBUTE_DEFAULT)
            .idAttributeReference(SequenceFlow.class)
            .build();

    typeBuilder.build();
  }

  @Override
  public ExclusiveGatewayBuilder builder() {
    return new ExclusiveGatewayBuilder((BpmnModelInstance) modelInstance, this);
  }

  @Override
  public SequenceFlow getDefault() {
    return defaultAttribute.getReferenceTargetElement(this);
  }

  @Override
  public void setDefault(final SequenceFlow defaultFlow) {
    defaultAttribute.setReferenceTargetElement(this, defaultFlow);
  }
}
