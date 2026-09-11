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
import static com.anyilanxin.kunpeng.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ELEMENT_TASK;

import com.anyilanxin.kunpeng.bpm.model.bpmn.BpmnModelInstance;
import com.anyilanxin.kunpeng.bpm.model.bpmn.builder.AbstractTaskBuilder;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.Activity;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.Task;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.bpmndi.BpmnShape;
import com.anyilanxin.kunpeng.bpm.model.xml.ModelBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementTypeBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;
import com.anyilanxin.kunpeng.bpm.model.xml.type.attribute.Attribute;

/**
 * The BPMN task element
 *
 * @author Sebastian Menski
 */
public class TaskImpl extends ActivityImpl implements Task {

  /** camunda extensions */
  protected static Attribute<Boolean> camundaAsyncAttribute;

  public TaskImpl(final ModelTypeInstanceContext context) {
    super(context);
  }

  public static void registerType(final ModelBuilder modelBuilder) {
    final ModelElementTypeBuilder typeBuilder =
        modelBuilder
            .defineType(Task.class, BPMN_ELEMENT_TASK)
            .namespaceUri(BPMN20_NS)
            .extendsType(Activity.class)
            .instanceProvider(
                new ModelTypeInstanceProvider<Task>() {
                  @Override
                  public Task newInstance(final ModelTypeInstanceContext instanceContext) {
                    return new TaskImpl(instanceContext);
                  }
                });

    typeBuilder.build();
  }

  @Override
  @SuppressWarnings("rawtypes")
  public AbstractTaskBuilder builder() {
    return new AbstractTaskBuilder<>(
        (BpmnModelInstance) modelInstance, this, AbstractTaskBuilder.class);
  }

  @Override
  public BpmnShape getDiagramElement() {
    return (BpmnShape) super.getDiagramElement();
  }
}
