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
package com.anyilanxin.kunpeng.bpm.model.xml.impl.type.reference;

import com.anyilanxin.kunpeng.bpm.model.xml.Model;
import com.anyilanxin.kunpeng.bpm.model.xml.ModelException;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.type.ModelElementTypeImpl;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.type.attribute.AttributeImpl;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.type.child.ChildElementCollectionImpl;
import com.anyilanxin.kunpeng.bpm.model.xml.instance.ModelElementInstance;
import com.anyilanxin.kunpeng.bpm.model.xml.type.reference.ElementReferenceCollection;
import com.anyilanxin.kunpeng.bpm.model.xml.type.reference.ElementReferenceCollectionBuilder;

/**
 * @author Sebastian Menski
 */
public class ElementReferenceCollectionBuilderImpl<
        Target extends ModelElementInstance, Source extends ModelElementInstance>
    implements ElementReferenceCollectionBuilder<Target, Source> {

  private final Class<Source> childElementType;
  private final Class<Target> referenceTargetClass;
  protected ElementReferenceCollectionImpl<Target, Source> elementReferenceCollectionImpl;

  public ElementReferenceCollectionBuilderImpl(
      final Class<Source> childElementType,
      final Class<Target> referenceTargetClass,
      final ChildElementCollectionImpl<Source> collection) {
    this.childElementType = childElementType;
    this.referenceTargetClass = referenceTargetClass;
    elementReferenceCollectionImpl = new ElementReferenceCollectionImpl<Target, Source>(collection);
  }

  @Override
  public ElementReferenceCollection<Target, Source> build() {
    return elementReferenceCollectionImpl;
  }

  @Override
  @SuppressWarnings("unchecked")
  public void performModelBuild(final Model model) {
    final ModelElementTypeImpl referenceTargetType =
        (ModelElementTypeImpl) model.getType(referenceTargetClass);
    final ModelElementTypeImpl referenceSourceType =
        (ModelElementTypeImpl) model.getType(childElementType);
    elementReferenceCollectionImpl.setReferenceTargetElementType(referenceTargetType);
    elementReferenceCollectionImpl.setReferenceSourceElementType(referenceSourceType);

    // the referenced attribute may be declared on a base type of the referenced type.
    final AttributeImpl<String> idAttribute =
        (AttributeImpl<String>) referenceTargetType.getAttribute("id");
    if (idAttribute != null) {
      idAttribute.registerIncoming(elementReferenceCollectionImpl);
      elementReferenceCollectionImpl.setReferenceTargetAttribute(idAttribute);
    } else {
      throw new ModelException("Unable to find id attribute of " + referenceTargetClass);
    }
  }
}
