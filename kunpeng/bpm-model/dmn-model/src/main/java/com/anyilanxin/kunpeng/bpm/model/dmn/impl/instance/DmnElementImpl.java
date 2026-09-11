/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
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
package com.anyilanxin.kunpeng.bpm.model.dmn.impl.instance;

import static com.anyilanxin.kunpeng.bpm.model.dmn.impl.DmnModelConstants.*;

import com.anyilanxin.kunpeng.bpm.model.dmn.instance.Description;
import com.anyilanxin.kunpeng.bpm.model.dmn.instance.DmnElement;
import com.anyilanxin.kunpeng.bpm.model.dmn.instance.ExtensionElements;
import com.anyilanxin.kunpeng.bpm.model.xml.ModelBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementTypeBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.type.attribute.Attribute;
import com.anyilanxin.kunpeng.bpm.model.xml.type.child.ChildElement;
import com.anyilanxin.kunpeng.bpm.model.xml.type.child.SequenceBuilder;

public abstract class DmnElementImpl extends DmnModelElementInstanceImpl implements DmnElement {

  protected static Attribute<String> idAttribute;
  protected static Attribute<String> labelAttribute;

  protected static ChildElement<Description> descriptionChild;
  protected static ChildElement<ExtensionElements> extensionElementsChild;

  public DmnElementImpl(final ModelTypeInstanceContext instanceContext) {
    super(instanceContext);
  }

  @Override
  public String getId() {
    return idAttribute.getValue(this);
  }

  @Override
  public void setId(final String id) {
    idAttribute.setValue(this, id);
  }

  @Override
  public String getLabel() {
    return labelAttribute.getValue(this);
  }

  @Override
  public void setLabel(final String label) {
    labelAttribute.setValue(this, label);
  }

  @Override
  public Description getDescription() {
    return descriptionChild.getChild(this);
  }

  @Override
  public void setDescription(final Description description) {
    descriptionChild.setChild(this, description);
  }

  @Override
  public ExtensionElements getExtensionElements() {
    return extensionElementsChild.getChild(this);
  }

  @Override
  public void setExtensionElements(final ExtensionElements extensionElements) {
    extensionElementsChild.setChild(this, extensionElements);
  }

  public static void registerType(final ModelBuilder modelBuilder) {
    final ModelElementTypeBuilder typeBuilder =
        modelBuilder
            .defineType(DmnElement.class, DMN_ELEMENT)
            .namespaceUri(LATEST_DMN_NS)
            .abstractType();

    idAttribute = typeBuilder.stringAttribute(DMN_ATTRIBUTE_ID).idAttribute().build();

    labelAttribute = typeBuilder.stringAttribute(DMN_ATTRIBUTE_LABEL).build();

    final SequenceBuilder sequenceBuilder = typeBuilder.sequence();

    descriptionChild = sequenceBuilder.element(Description.class).build();

    extensionElementsChild = sequenceBuilder.element(ExtensionElements.class).build();

    typeBuilder.build();
  }
}
