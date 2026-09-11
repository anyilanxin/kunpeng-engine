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

import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.DataObject;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.DataState;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.FlowElement;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.ItemDefinition;
import com.anyilanxin.kunpeng.bpm.model.xml.ModelBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementTypeBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;
import com.anyilanxin.kunpeng.bpm.model.xml.type.attribute.Attribute;
import com.anyilanxin.kunpeng.bpm.model.xml.type.child.ChildElement;
import com.anyilanxin.kunpeng.bpm.model.xml.type.child.SequenceBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.type.reference.AttributeReference;

/**
 * The BPMN dataObject element
 *
 * @author Dario Campagna
 */
public class DataObjectImpl extends FlowElementImpl implements DataObject {

  protected static AttributeReference<ItemDefinition> itemSubjectRefAttribute;
  protected static Attribute<Boolean> isCollectionAttribute;
  protected static ChildElement<DataState> dataStateChild;

  public DataObjectImpl(final ModelTypeInstanceContext instanceContext) {
    super(instanceContext);
  }

  public static void registerType(final ModelBuilder modelBuilder) {
    final ModelElementTypeBuilder typeBuilder =
        modelBuilder
            .defineType(DataObject.class, BPMN_ELEMENT_DATA_OBJECT)
            .namespaceUri(BPMN20_NS)
            .extendsType(FlowElement.class)
            .instanceProvider(
                new ModelTypeInstanceProvider<DataObject>() {
                  @Override
                  public DataObject newInstance(final ModelTypeInstanceContext instanceContext) {
                    return new DataObjectImpl(instanceContext);
                  }
                });

    itemSubjectRefAttribute =
        typeBuilder
            .stringAttribute(BPMN_ATTRIBUTE_ITEM_SUBJECT_REF)
            .qNameAttributeReference(ItemDefinition.class)
            .build();

    isCollectionAttribute =
        typeBuilder.booleanAttribute(BPMN_ATTRIBUTE_IS_COLLECTION).defaultValue(false).build();

    final SequenceBuilder sequenceBuilder = typeBuilder.sequence();

    dataStateChild = sequenceBuilder.element(DataState.class).build();

    typeBuilder.build();
  }

  @Override
  public ItemDefinition getItemSubject() {
    return itemSubjectRefAttribute.getReferenceTargetElement(this);
  }

  @Override
  public void setItemSubject(final ItemDefinition itemSubject) {
    itemSubjectRefAttribute.setReferenceTargetElement(this, itemSubject);
  }

  @Override
  public DataState getDataState() {
    return dataStateChild.getChild(this);
  }

  @Override
  public void setDataState(final DataState dataState) {
    dataStateChild.setChild(this, dataState);
  }

  @Override
  public boolean isCollection() {
    return isCollectionAttribute.getValue(this);
  }

  @Override
  public void setCollection(final boolean isCollection) {
    isCollectionAttribute.setValue(this, isCollection);
  }
}
