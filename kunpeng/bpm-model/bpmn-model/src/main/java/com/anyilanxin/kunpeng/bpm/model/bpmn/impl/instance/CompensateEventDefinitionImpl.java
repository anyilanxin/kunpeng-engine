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
import static com.anyilanxin.kunpeng.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ATTRIBUTE_ACTIVITY_REF;
import static com.anyilanxin.kunpeng.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ATTRIBUTE_WAIT_FOR_COMPLETION;
import static com.anyilanxin.kunpeng.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ELEMENT_COMPENSATE_EVENT_DEFINITION;

import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.Activity;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.CompensateEventDefinition;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.EventDefinition;
import com.anyilanxin.kunpeng.bpm.model.xml.ModelBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementTypeBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;
import com.anyilanxin.kunpeng.bpm.model.xml.type.attribute.Attribute;
import com.anyilanxin.kunpeng.bpm.model.xml.type.reference.AttributeReference;

/**
 * The BPMN compensateEventDefinition element
 *
 * @author Sebastian Menski
 */
public class CompensateEventDefinitionImpl extends EventDefinitionImpl
    implements CompensateEventDefinition {

  protected static Attribute<Boolean> waitForCompletionAttribute;
  protected static AttributeReference<Activity> activityRefAttribute;

  public CompensateEventDefinitionImpl(final ModelTypeInstanceContext context) {
    super(context);
  }

  public static void registerType(final ModelBuilder modelBuilder) {
    final ModelElementTypeBuilder typeBuilder =
        modelBuilder
            .defineType(CompensateEventDefinition.class, BPMN_ELEMENT_COMPENSATE_EVENT_DEFINITION)
            .namespaceUri(BPMN20_NS)
            .extendsType(EventDefinition.class)
            .instanceProvider(
                new ModelTypeInstanceProvider<CompensateEventDefinition>() {
                  @Override
                  public CompensateEventDefinition newInstance(
                      final ModelTypeInstanceContext instanceContext) {
                    return new CompensateEventDefinitionImpl(instanceContext);
                  }
                });

    waitForCompletionAttribute =
        typeBuilder.booleanAttribute(BPMN_ATTRIBUTE_WAIT_FOR_COMPLETION).build();

    activityRefAttribute =
        typeBuilder
            .stringAttribute(BPMN_ATTRIBUTE_ACTIVITY_REF)
            .qNameAttributeReference(Activity.class)
            .build();

    typeBuilder.build();
  }

  @Override
  public boolean isWaitForCompletion() {
    return waitForCompletionAttribute.getValue(this);
  }

  @Override
  public void setWaitForCompletion(final boolean isWaitForCompletion) {
    waitForCompletionAttribute.setValue(this, isWaitForCompletion);
  }

  @Override
  public Activity getActivity() {
    return activityRefAttribute.getReferenceTargetElement(this);
  }

  @Override
  public void setActivity(final Activity activity) {
    activityRefAttribute.setReferenceTargetElement(this, activity);
  }
}
