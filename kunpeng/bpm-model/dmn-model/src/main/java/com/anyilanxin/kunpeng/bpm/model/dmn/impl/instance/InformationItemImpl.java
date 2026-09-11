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

import com.anyilanxin.kunpeng.bpm.model.dmn.instance.InformationItem;
import com.anyilanxin.kunpeng.bpm.model.dmn.instance.NamedElement;
import com.anyilanxin.kunpeng.bpm.model.xml.ModelBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementTypeBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;
import com.anyilanxin.kunpeng.bpm.model.xml.type.attribute.Attribute;

public class InformationItemImpl extends NamedElementImpl implements InformationItem {

  protected static Attribute<String> typeRefAttribute;

  public InformationItemImpl(final ModelTypeInstanceContext instanceContext) {
    super(instanceContext);
  }

  @Override
  public String getTypeRef() {
    return typeRefAttribute.getValue(this);
  }

  @Override
  public void setTypeRef(final String typeRef) {
    typeRefAttribute.setValue(this, typeRef);
  }

  public static void registerType(final ModelBuilder modelBuilder) {
    final ModelElementTypeBuilder typeBuilder =
        modelBuilder
            .defineType(InformationItem.class, DMN_ELEMENT_INFORMATION_ITEM)
            .namespaceUri(LATEST_DMN_NS)
            .extendsType(NamedElement.class)
            .instanceProvider(
                new ModelTypeInstanceProvider<InformationItem>() {
                  @Override
                  public InformationItem newInstance(
                      final ModelTypeInstanceContext instanceContext) {
                    return new InformationItemImpl(instanceContext);
                  }
                });

    typeRefAttribute = typeBuilder.stringAttribute(DMN_ATTRIBUTE_TYPE_REF).build();

    typeBuilder.build();
  }
}
