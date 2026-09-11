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
import static com.anyilanxin.kunpeng.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ATTRIBUTE_IMPLEMENTATION;
import static com.anyilanxin.kunpeng.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ATTRIBUTE_OPERATION_REF;
import static com.anyilanxin.kunpeng.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ELEMENT_SERVICE_TASK;
import static com.anyilanxin.kunpeng.bpm.model.bpmn.impl.BpmnModelConstants.ZEEBE_NS;

import com.anyilanxin.kunpeng.bpm.model.bpmn.BpmnModelInstance;
import com.anyilanxin.kunpeng.bpm.model.bpmn.builder.ServiceTaskBuilder;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.ZeebeConstants;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.Operation;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.ServiceTask;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.Task;
import com.anyilanxin.kunpeng.bpm.model.xml.ModelBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementTypeBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;
import com.anyilanxin.kunpeng.bpm.model.xml.type.attribute.Attribute;
import com.anyilanxin.kunpeng.bpm.model.xml.type.reference.AttributeReference;

/**
 * The BPMN serviceTask element
 *
 * @author Sebastian Menski
 */
public class ServiceTaskImpl extends TaskImpl implements ServiceTask {

  protected static Attribute<String> implementationAttribute;
  protected static AttributeReference<Operation> operationRefAttribute;
  protected static Attribute<String> modelerTemplateAttribute;

  public ServiceTaskImpl(final ModelTypeInstanceContext context) {
    super(context);
  }

  public static void registerType(final ModelBuilder modelBuilder) {
    final ModelElementTypeBuilder typeBuilder =
        modelBuilder
            .defineType(ServiceTask.class, BPMN_ELEMENT_SERVICE_TASK)
            .namespaceUri(BPMN20_NS)
            .extendsType(Task.class)
            .instanceProvider(
                new ModelTypeInstanceProvider<ServiceTask>() {
                  @Override
                  public ServiceTask newInstance(final ModelTypeInstanceContext instanceContext) {
                    return new ServiceTaskImpl(instanceContext);
                  }
                });

    implementationAttribute =
        typeBuilder
            .stringAttribute(BPMN_ATTRIBUTE_IMPLEMENTATION)
            .defaultValue("##WebService")
            .build();

    operationRefAttribute =
        typeBuilder
            .stringAttribute(BPMN_ATTRIBUTE_OPERATION_REF)
            .qNameAttributeReference(Operation.class)
            .build();

    modelerTemplateAttribute =
        typeBuilder
            .stringAttribute(ZeebeConstants.ATTRIBUTE_MODELER_TEMPLATE)
            .namespace(ZEEBE_NS)
            .build();

    typeBuilder.build();
  }

  @Override
  public ServiceTaskBuilder builder() {
    return new ServiceTaskBuilder((BpmnModelInstance) modelInstance, this);
  }

  @Override
  public String getImplementation() {
    return implementationAttribute.getValue(this);
  }

  @Override
  public void setImplementation(final String implementation) {
    implementationAttribute.setValue(this, implementation);
  }

  @Override
  public Operation getOperation() {
    return operationRefAttribute.getReferenceTargetElement(this);
  }

  @Override
  public void setOperation(final Operation operation) {
    operationRefAttribute.setReferenceTargetElement(this, operation);
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
