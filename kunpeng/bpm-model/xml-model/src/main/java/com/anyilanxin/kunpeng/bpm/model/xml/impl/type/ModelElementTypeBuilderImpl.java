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
package com.anyilanxin.kunpeng.bpm.model.xml.impl.type;

import com.anyilanxin.kunpeng.bpm.model.xml.Model;
import com.anyilanxin.kunpeng.bpm.model.xml.ModelException;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.ModelBuildOperation;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.ModelImpl;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.type.attribute.*;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.type.child.SequenceBuilderImpl;
import com.anyilanxin.kunpeng.bpm.model.xml.instance.ModelElementInstance;
import com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementType;
import com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementTypeBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.type.attribute.AttributeBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.type.attribute.StringAttributeBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.type.child.SequenceBuilder;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Daniel Meyer
 */
public class ModelElementTypeBuilderImpl implements ModelElementTypeBuilder, ModelBuildOperation {

  private final ModelElementTypeImpl modelType;
  private final ModelImpl model;
  private final Class<? extends ModelElementInstance> instanceType;

  private final List<ModelBuildOperation> modelBuildOperations =
      new ArrayList<ModelBuildOperation>();
  private Class<? extends ModelElementInstance> extendedType;

  public ModelElementTypeBuilderImpl(
      final Class<? extends ModelElementInstance> instanceType,
      final String name,
      final ModelImpl model) {
    this.instanceType = instanceType;
    this.model = model;
    modelType = new ModelElementTypeImpl(model, name, instanceType);
  }

  @Override
  public ModelElementTypeBuilder extendsType(
      final Class<? extends ModelElementInstance> extendedType) {
    this.extendedType = extendedType;
    return this;
  }

  @Override
  public <T extends ModelElementInstance> ModelElementTypeBuilder instanceProvider(
      final ModelTypeInstanceProvider<T> instanceProvider) {
    modelType.setInstanceProvider(instanceProvider);
    return this;
  }

  @Override
  public ModelElementTypeBuilder namespaceUri(final String namespaceUri) {
    modelType.setTypeNamespace(namespaceUri);
    return this;
  }

  @Override
  public AttributeBuilder<Boolean> booleanAttribute(final String attributeName) {
    final BooleanAttributeBuilder builder = new BooleanAttributeBuilder(attributeName, modelType);
    modelBuildOperations.add(builder);
    return builder;
  }

  @Override
  public StringAttributeBuilder stringAttribute(final String attributeName) {
    final StringAttributeBuilderImpl builder =
        new StringAttributeBuilderImpl(attributeName, modelType);
    modelBuildOperations.add(builder);
    return builder;
  }

  @Override
  public AttributeBuilder<Integer> integerAttribute(final String attributeName) {
    final IntegerAttributeBuilder builder = new IntegerAttributeBuilder(attributeName, modelType);
    modelBuildOperations.add(builder);
    return builder;
  }

  @Override
  public AttributeBuilder<Double> doubleAttribute(final String attributeName) {
    final DoubleAttributeBuilder builder = new DoubleAttributeBuilder(attributeName, modelType);
    modelBuildOperations.add(builder);
    return builder;
  }

  @Override
  public <V extends Enum<V>> AttributeBuilder<V> enumAttribute(
      final String attributeName, final Class<V> enumType) {
    final EnumAttributeBuilder<V> builder =
        new EnumAttributeBuilder<V>(attributeName, modelType, enumType);
    modelBuildOperations.add(builder);
    return builder;
  }

  @Override
  public <V extends Enum<V>> AttributeBuilder<V> namedEnumAttribute(
      final String attributeName, final Class<V> enumType) {
    final NamedEnumAttributeBuilder<V> builder =
        new NamedEnumAttributeBuilder<V>(attributeName, modelType, enumType);
    modelBuildOperations.add(builder);
    return builder;
  }

  @Override
  public ModelElementType build() {
    model.registerType(modelType, instanceType);
    return modelType;
  }

  @Override
  public ModelElementTypeBuilder abstractType() {
    modelType.setAbstract(true);
    return this;
  }

  @Override
  public SequenceBuilder sequence() {
    final SequenceBuilderImpl builder = new SequenceBuilderImpl(modelType);
    modelBuildOperations.add(builder);
    return builder;
  }

  public void buildTypeHierarchy(final Model model) {

    // build type hierarchy
    if (extendedType != null) {
      final ModelElementTypeImpl extendedModelElementType =
          (ModelElementTypeImpl) model.getType(extendedType);
      if (extendedModelElementType == null) {
        throw new ModelException(
            "Type "
                + modelType
                + " is defined to extend "
                + extendedType
                + " but no such type is defined.");

      } else {
        modelType.setBaseType(extendedModelElementType);
        extendedModelElementType.registerExtendingType(modelType);
      }
    }
  }

  @Override
  public void performModelBuild(final Model model) {
    for (final ModelBuildOperation operation : modelBuildOperations) {
      operation.performModelBuild(model);
    }
  }
}
