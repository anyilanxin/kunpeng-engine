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
import static com.anyilanxin.kunpeng.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ATTRIBUTE_CATEGORY_VALUE_REF;
import static com.anyilanxin.kunpeng.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ELEMENT_GROUP;

import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.Artifact;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.CategoryValue;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.Group;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.bpmndi.BpmnEdge;
import com.anyilanxin.kunpeng.bpm.model.xml.ModelBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementTypeBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;
import com.anyilanxin.kunpeng.bpm.model.xml.type.reference.AttributeReference;

public class GroupImpl extends ArtifactImpl implements Group {

  protected static AttributeReference<CategoryValue> categoryValueRefAttribute;

  public GroupImpl(final ModelTypeInstanceContext instanceContext) {
    super(instanceContext);
  }

  public static void registerType(final ModelBuilder modelBuilder) {
    final ModelElementTypeBuilder typeBuilder =
        modelBuilder
            .defineType(Group.class, BPMN_ELEMENT_GROUP)
            .namespaceUri(BPMN20_NS)
            .extendsType(Artifact.class)
            .instanceProvider(
                new ModelTypeInstanceProvider<Group>() {
                  @Override
                  public Group newInstance(final ModelTypeInstanceContext instanceContext) {
                    return new GroupImpl(instanceContext);
                  }
                });

    categoryValueRefAttribute =
        typeBuilder
            .stringAttribute(BPMN_ATTRIBUTE_CATEGORY_VALUE_REF)
            .qNameAttributeReference(CategoryValue.class)
            .build();

    typeBuilder.build();
  }

  @Override
  public CategoryValue getCategory() {
    return categoryValueRefAttribute.getReferenceTargetElement(this);
  }

  @Override
  public void setCategory(final CategoryValue categoryValue) {
    categoryValueRefAttribute.setReferenceTargetElement(this, categoryValue);
  }

  @Override
  public BpmnEdge getDiagramElement() {
    return (BpmnEdge) super.getDiagramElement();
  }
}
