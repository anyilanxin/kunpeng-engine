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
package com.anyilanxin.kunpeng.bpm.model.xml.impl.type.attribute;

import com.anyilanxin.kunpeng.bpm.model.xml.Model;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.ModelBuildOperation;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.type.ModelElementTypeImpl;
import com.anyilanxin.kunpeng.bpm.model.xml.type.attribute.Attribute;
import com.anyilanxin.kunpeng.bpm.model.xml.type.attribute.AttributeBuilder;

/**
 * @author Daniel Meyer
 */
public abstract class AttributeBuilderImpl<T> implements AttributeBuilder<T>, ModelBuildOperation {

  private final AttributeImpl<T> attribute;
  private final ModelElementTypeImpl modelType;

  AttributeBuilderImpl(
      final String attributeName,
      final ModelElementTypeImpl modelType,
      final AttributeImpl<T> attribute) {
    this.modelType = modelType;
    this.attribute = attribute;
    attribute.setAttributeName(attributeName);
  }

  @Override
  public AttributeBuilder<T> namespace(final String namespaceUri) {
    attribute.setNamespaceUri(namespaceUri);
    return this;
  }

  @Override
  public AttributeBuilder<T> idAttribute() {
    attribute.setId();
    return this;
  }

  @Override
  public AttributeBuilder<T> defaultValue(final T defaultValue) {
    attribute.setDefaultValue(defaultValue);
    return this;
  }

  @Override
  public AttributeBuilder<T> required() {
    attribute.setRequired(true);
    return this;
  }

  @Override
  public Attribute<T> build() {
    modelType.registerAttribute(attribute);
    return attribute;
  }

  @Override
  public void performModelBuild(final Model model) {
    // do nothing
  }
}
