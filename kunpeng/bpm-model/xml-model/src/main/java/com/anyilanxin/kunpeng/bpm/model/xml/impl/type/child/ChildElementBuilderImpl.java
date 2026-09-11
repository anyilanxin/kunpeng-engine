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
package com.anyilanxin.kunpeng.bpm.model.xml.impl.type.child;

import com.anyilanxin.kunpeng.bpm.model.xml.impl.type.reference.ElementReferenceBuilderImpl;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.type.reference.QNameElementReferenceBuilderImpl;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.type.reference.UriElementReferenceBuilderImpl;
import com.anyilanxin.kunpeng.bpm.model.xml.instance.ModelElementInstance;
import com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementType;
import com.anyilanxin.kunpeng.bpm.model.xml.type.child.ChildElement;
import com.anyilanxin.kunpeng.bpm.model.xml.type.child.ChildElementBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.type.reference.ElementReferenceBuilder;

/**
 * @author Daniel Meyer
 */
public class ChildElementBuilderImpl<T extends ModelElementInstance>
    extends ChildElementCollectionBuilderImpl<T> implements ChildElementBuilder<T> {

  public ChildElementBuilderImpl(
      final Class<T> childElementTypeClass, final ModelElementType parentElementType) {
    super(childElementTypeClass, parentElementType);
  }

  @Override
  protected ChildElementCollectionImpl<T> createCollectionInstance() {
    return new ChildElementImpl<T>(childElementType, parentElementType);
  }

  @Override
  public ChildElementBuilder<T> immutable() {
    super.immutable();
    return this;
  }

  @Override
  public ChildElementBuilder<T> required() {
    super.required();
    return this;
  }

  @Override
  public ChildElementBuilder<T> minOccurs(final int i) {
    super.minOccurs(i);
    return this;
  }

  @Override
  public ChildElementBuilder<T> maxOccurs(final int i) {
    super.maxOccurs(i);
    return this;
  }

  @Override
  public ChildElement<T> build() {
    return (ChildElement<T>) super.build();
  }

  @Override
  public <V extends ModelElementInstance> ElementReferenceBuilder<V, T> qNameElementReference(
      final Class<V> referenceTargetType) {
    final ChildElementImpl<T> child = (ChildElementImpl<T>) build();
    final QNameElementReferenceBuilderImpl<V, T> builder =
        new QNameElementReferenceBuilderImpl<V, T>(childElementType, referenceTargetType, child);
    setReferenceBuilder(builder);
    return builder;
  }

  @Override
  public <V extends ModelElementInstance> ElementReferenceBuilder<V, T> idElementReference(
      final Class<V> referenceTargetType) {
    final ChildElementImpl<T> child = (ChildElementImpl<T>) build();
    final ElementReferenceBuilderImpl<V, T> builder =
        new ElementReferenceBuilderImpl<V, T>(childElementType, referenceTargetType, child);
    setReferenceBuilder(builder);
    return builder;
  }

  @Override
  public <V extends ModelElementInstance> ElementReferenceBuilder<V, T> uriElementReference(
      final Class<V> referenceTargetType) {
    final ChildElementImpl<T> child = (ChildElementImpl<T>) build();
    final ElementReferenceBuilderImpl<V, T> builder =
        new UriElementReferenceBuilderImpl<V, T>(childElementType, referenceTargetType, child);
    setReferenceBuilder(builder);
    return builder;
  }
}
