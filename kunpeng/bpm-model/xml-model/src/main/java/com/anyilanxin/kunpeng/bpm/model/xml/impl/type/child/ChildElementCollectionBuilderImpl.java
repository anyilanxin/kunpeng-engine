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

import com.anyilanxin.kunpeng.bpm.model.xml.Model;
import com.anyilanxin.kunpeng.bpm.model.xml.ModelException;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.ModelBuildOperation;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.type.ModelElementTypeImpl;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.type.reference.ElementReferenceCollectionBuilderImpl;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.type.reference.IdsElementReferenceCollectionBuilderImpl;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.type.reference.QNameElementReferenceCollectionBuilderImpl;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.type.reference.UriElementReferenceCollectionBuilderImpl;
import com.anyilanxin.kunpeng.bpm.model.xml.instance.ModelElementInstance;
import com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementType;
import com.anyilanxin.kunpeng.bpm.model.xml.type.child.ChildElementCollection;
import com.anyilanxin.kunpeng.bpm.model.xml.type.child.ChildElementCollectionBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.type.reference.ElementReferenceCollectionBuilder;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Daniel Meyer
 */
public class ChildElementCollectionBuilderImpl<T extends ModelElementInstance>
    implements ChildElementCollectionBuilder<T>, ModelBuildOperation {

  /** The {@link ModelElementType} of the element containing the collection */
  protected final ModelElementTypeImpl parentElementType;

  private final ChildElementCollectionImpl<T> collection;
  protected final Class<T> childElementType;

  private ElementReferenceCollectionBuilder<?, ?> referenceBuilder;

  private final List<ModelBuildOperation> modelBuildOperations =
      new ArrayList<ModelBuildOperation>();

  public ChildElementCollectionBuilderImpl(
      final Class<T> childElementTypeClass, final ModelElementType parentElementType) {
    childElementType = childElementTypeClass;
    this.parentElementType = (ModelElementTypeImpl) parentElementType;
    collection = createCollectionInstance();
  }

  protected ChildElementCollectionImpl<T> createCollectionInstance() {
    return new ChildElementCollectionImpl<T>(childElementType, parentElementType);
  }

  @Override
  public ChildElementCollectionBuilder<T> immutable() {
    collection.setImmutable();
    return this;
  }

  @Override
  public ChildElementCollectionBuilder<T> required() {
    collection.setMinOccurs(1);
    return this;
  }

  @Override
  public ChildElementCollectionBuilder<T> maxOccurs(final int i) {
    collection.setMaxOccurs(i);
    return this;
  }

  @Override
  public ChildElementCollectionBuilder<T> minOccurs(final int i) {
    collection.setMinOccurs(i);
    return this;
  }

  @Override
  public ChildElementCollection<T> build() {
    return collection;
  }

  @Override
  public <V extends ModelElementInstance>
      ElementReferenceCollectionBuilder<V, T> qNameElementReferenceCollection(
          final Class<V> referenceTargetType) {
    final ChildElementCollectionImpl<T> collection = (ChildElementCollectionImpl<T>) build();
    final QNameElementReferenceCollectionBuilderImpl<V, T> builder =
        new QNameElementReferenceCollectionBuilderImpl<V, T>(
            childElementType, referenceTargetType, collection);
    setReferenceBuilder(builder);
    return builder;
  }

  @Override
  public <V extends ModelElementInstance>
      ElementReferenceCollectionBuilder<V, T> idElementReferenceCollection(
          final Class<V> referenceTargetType) {
    final ChildElementCollectionImpl<T> collection = (ChildElementCollectionImpl<T>) build();
    final ElementReferenceCollectionBuilder<V, T> builder =
        new ElementReferenceCollectionBuilderImpl<V, T>(
            childElementType, referenceTargetType, collection);
    setReferenceBuilder(builder);
    return builder;
  }

  @Override
  public <V extends ModelElementInstance>
      ElementReferenceCollectionBuilder<V, T> idsElementReferenceCollection(
          final Class<V> referenceTargetType) {
    final ChildElementCollectionImpl<T> collection = (ChildElementCollectionImpl<T>) build();
    final ElementReferenceCollectionBuilder<V, T> builder =
        new IdsElementReferenceCollectionBuilderImpl<V, T>(
            childElementType, referenceTargetType, collection);
    setReferenceBuilder(builder);
    return builder;
  }

  @Override
  public <V extends ModelElementInstance>
      ElementReferenceCollectionBuilder<V, T> uriElementReferenceCollection(
          final Class<V> referenceTargetType) {
    final ChildElementCollectionImpl<T> collection = (ChildElementCollectionImpl<T>) build();
    final ElementReferenceCollectionBuilder<V, T> builder =
        new UriElementReferenceCollectionBuilderImpl<V, T>(
            childElementType, referenceTargetType, collection);
    setReferenceBuilder(builder);
    return builder;
  }

  protected void setReferenceBuilder(
      final ElementReferenceCollectionBuilder<?, ?> referenceBuilder) {
    if (this.referenceBuilder != null) {
      throw new ModelException("An collection cannot have more than one reference");
    }
    this.referenceBuilder = referenceBuilder;
    modelBuildOperations.add(referenceBuilder);
  }

  @Override
  public void performModelBuild(final Model model) {
    final ModelElementType elementType = model.getType(childElementType);
    if (elementType == null) {
      throw new ModelException(
          parentElementType
              + " declares undefined child element of type "
              + childElementType
              + ".");
    }
    parentElementType.registerChildElementType(elementType);
    parentElementType.registerChildElementCollection(collection);
    for (final ModelBuildOperation modelBuildOperation : modelBuildOperations) {
      modelBuildOperation.performModelBuild(model);
    }
  }
}
