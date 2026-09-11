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

import static com.anyilanxin.kunpeng.bpm.model.dmn.impl.DmnModelConstants.DMN_ELEMENT_ELEMENT_COLLECTION;
import static com.anyilanxin.kunpeng.bpm.model.dmn.impl.DmnModelConstants.LATEST_DMN_NS;

import com.anyilanxin.kunpeng.bpm.model.dmn.instance.DrgElement;
import com.anyilanxin.kunpeng.bpm.model.dmn.instance.DrgElementReference;
import com.anyilanxin.kunpeng.bpm.model.dmn.instance.ElementCollection;
import com.anyilanxin.kunpeng.bpm.model.dmn.instance.NamedElement;
import com.anyilanxin.kunpeng.bpm.model.xml.ModelBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementTypeBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;
import com.anyilanxin.kunpeng.bpm.model.xml.type.child.SequenceBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.type.reference.ElementReferenceCollection;
import java.util.Collection;

public class ElementCollectionImpl extends NamedElementImpl implements ElementCollection {

  protected static ElementReferenceCollection<DrgElement, DrgElementReference>
      drgElementRefCollection;

  public ElementCollectionImpl(final ModelTypeInstanceContext instanceContext) {
    super(instanceContext);
  }

  @Override
  public Collection<DrgElement> getDrgElements() {
    return drgElementRefCollection.getReferenceTargetElements(this);
  }

  public static void registerType(final ModelBuilder modelBuilder) {
    final ModelElementTypeBuilder typeBuilder =
        modelBuilder
            .defineType(ElementCollection.class, DMN_ELEMENT_ELEMENT_COLLECTION)
            .namespaceUri(LATEST_DMN_NS)
            .extendsType(NamedElement.class)
            .instanceProvider(
                new ModelTypeInstanceProvider<ElementCollection>() {
                  @Override
                  public ElementCollection newInstance(
                      final ModelTypeInstanceContext instanceContext) {
                    return new ElementCollectionImpl(instanceContext);
                  }
                });

    final SequenceBuilder sequenceBuilder = typeBuilder.sequence();

    drgElementRefCollection =
        sequenceBuilder
            .elementCollection(DrgElementReference.class)
            .uriElementReferenceCollection(DrgElement.class)
            .build();

    typeBuilder.build();
  }
}
