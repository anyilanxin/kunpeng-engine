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
import static com.anyilanxin.kunpeng.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ATTRIBUTE_CANCEL_REMAINING_INSTANCES;
import static com.anyilanxin.kunpeng.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ELEMENT_AD_HOC_SUB_PROCESS;
import static com.anyilanxin.kunpeng.bpm.model.bpmn.impl.BpmnModelConstants.ZEEBE_NS;

import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.ZeebeConstants;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.AdHocSubProcess;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.CompletionCondition;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.SubProcess;
import com.anyilanxin.kunpeng.bpm.model.xml.ModelBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementTypeBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;
import com.anyilanxin.kunpeng.bpm.model.xml.type.attribute.Attribute;
import com.anyilanxin.kunpeng.bpm.model.xml.type.child.ChildElement;
import com.anyilanxin.kunpeng.bpm.model.xml.type.child.SequenceBuilder;

public class AdHocSubProcessImpl extends SubProcessImpl implements AdHocSubProcess {

  private static Attribute<Boolean> cancelRemainingInstancesAttribute;
  private static ChildElement<CompletionCondition> completionConditionChild;
  private static Attribute<String> modelerTemplateAttribute;

  public AdHocSubProcessImpl(final ModelTypeInstanceContext context) {
    super(context);
  }

  public static void registerType(final ModelBuilder modelBuilder) {
    final ModelElementTypeBuilder typeBuilder =
        modelBuilder
            .defineType(AdHocSubProcess.class, BPMN_ELEMENT_AD_HOC_SUB_PROCESS)
            .namespaceUri(BPMN20_NS)
            .extendsType(SubProcess.class)
            .instanceProvider(
                new ModelTypeInstanceProvider<AdHocSubProcess>() {
                  @Override
                  public AdHocSubProcess newInstance(
                      final ModelTypeInstanceContext instanceContext) {
                    return new AdHocSubProcessImpl(instanceContext);
                  }
                });

    cancelRemainingInstancesAttribute =
        typeBuilder
            .booleanAttribute(BPMN_ATTRIBUTE_CANCEL_REMAINING_INSTANCES)
            .defaultValue(true)
            .build();

    final SequenceBuilder sequenceBuilder = typeBuilder.sequence();
    completionConditionChild = sequenceBuilder.element(CompletionCondition.class).build();

    modelerTemplateAttribute =
        typeBuilder
            .stringAttribute(ZeebeConstants.ATTRIBUTE_MODELER_TEMPLATE)
            .namespace(ZEEBE_NS)
            .build();

    typeBuilder.build();
  }

  @Override
  public CompletionCondition getCompletionCondition() {
    return completionConditionChild.getChild(this);
  }

  @Override
  public void setCompletionCondition(final CompletionCondition completionCondition) {
    completionConditionChild.setChild(this, completionCondition);
  }

  @Override
  public boolean isCancelRemainingInstances() {
    return cancelRemainingInstancesAttribute.getValue(this);
  }

  @Override
  public void setCancelRemainingInstances(final boolean cancelRemainingInstances) {
    cancelRemainingInstancesAttribute.setValue(this, cancelRemainingInstances);
  }

  @Override
  public String getModelerTemplate() {
    return modelerTemplateAttribute.getValue(this);
  }

  @Override
  public void setModelerTemplate(final String modelerTemplate) {
    modelerTemplateAttribute.setValue(this, modelerTemplate);
  }
}
