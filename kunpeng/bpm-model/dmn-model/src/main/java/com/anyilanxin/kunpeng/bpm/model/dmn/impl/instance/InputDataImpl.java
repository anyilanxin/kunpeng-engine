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

import static com.anyilanxin.kunpeng.bpm.model.dmn.impl.DmnModelConstants.DMN_ELEMENT_INPUT_DATA;
import static com.anyilanxin.kunpeng.bpm.model.dmn.impl.DmnModelConstants.LATEST_DMN_NS;

import com.anyilanxin.kunpeng.bpm.model.dmn.instance.DrgElement;
import com.anyilanxin.kunpeng.bpm.model.dmn.instance.InformationItem;
import com.anyilanxin.kunpeng.bpm.model.dmn.instance.InputData;
import com.anyilanxin.kunpeng.bpm.model.xml.ModelBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementTypeBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;
import com.anyilanxin.kunpeng.bpm.model.xml.type.child.ChildElement;
import com.anyilanxin.kunpeng.bpm.model.xml.type.child.SequenceBuilder;

public class InputDataImpl extends DrgElementImpl implements InputData {

  protected static ChildElement<InformationItem> informationItemChild;

  public InputDataImpl(final ModelTypeInstanceContext instanceContext) {
    super(instanceContext);
  }

  @Override
  public InformationItem getInformationItem() {
    return informationItemChild.getChild(this);
  }

  @Override
  public void setInformationItem(final InformationItem informationItem) {
    informationItemChild.setChild(this, informationItem);
  }

  public static void registerType(final ModelBuilder modelBuilder) {
    final ModelElementTypeBuilder typeBuilder =
        modelBuilder
            .defineType(InputData.class, DMN_ELEMENT_INPUT_DATA)
            .namespaceUri(LATEST_DMN_NS)
            .extendsType(DrgElement.class)
            .instanceProvider(
                new ModelTypeInstanceProvider<InputData>() {
                  @Override
                  public InputData newInstance(final ModelTypeInstanceContext instanceContext) {
                    return new InputDataImpl(instanceContext);
                  }
                });

    final SequenceBuilder sequenceBuilder = typeBuilder.sequence();

    informationItemChild = sequenceBuilder.element(InformationItem.class).build();

    typeBuilder.build();
  }
}
