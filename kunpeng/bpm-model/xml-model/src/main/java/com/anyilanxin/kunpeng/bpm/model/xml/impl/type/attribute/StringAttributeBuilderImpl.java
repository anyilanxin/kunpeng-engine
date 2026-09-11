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
import com.anyilanxin.kunpeng.bpm.model.xml.ModelException;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.ModelBuildOperation;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.type.ModelElementTypeImpl;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.type.reference.AttributeReferenceBuilderImpl;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.type.reference.AttributeReferenceCollectionBuilderImpl;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.type.reference.QNameAttributeReferenceBuilderImpl;
import com.anyilanxin.kunpeng.bpm.model.xml.instance.ModelElementInstance;
import com.anyilanxin.kunpeng.bpm.model.xml.type.attribute.StringAttributeBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.type.reference.AttributeReferenceBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.type.reference.AttributeReferenceCollection;
import com.anyilanxin.kunpeng.bpm.model.xml.type.reference.AttributeReferenceCollectionBuilder;

/**
 * @author Daniel Meyer
 */
public class StringAttributeBuilderImpl extends AttributeBuilderImpl<String>
    implements StringAttributeBuilder {

  private AttributeReferenceBuilder<?> referenceBuilder;

  public StringAttributeBuilderImpl(
      final String attributeName, final ModelElementTypeImpl modelType) {
    super(attributeName, modelType, new StringAttribute(modelType));
  }

  @Override
  public StringAttributeBuilder namespace(final String namespaceUri) {
    return (StringAttributeBuilder) super.namespace(namespaceUri);
  }

  @Override
  public StringAttributeBuilder defaultValue(final String defaultValue) {
    return (StringAttributeBuilder) super.defaultValue(defaultValue);
  }

  @Override
  public StringAttributeBuilder required() {
    return (StringAttributeBuilder) super.required();
  }

  @Override
  public StringAttributeBuilder idAttribute() {
    return (StringAttributeBuilder) super.idAttribute();
  }

  /**
   * Create a new {@link AttributeReferenceBuilder} for the reference source element instance
   *
   * @param referenceTargetElement the reference target model element instance
   * @return the new attribute reference builder
   */
  @Override
  public <V extends ModelElementInstance> AttributeReferenceBuilder<V> qNameAttributeReference(
      final Class<V> referenceTargetElement) {
    final AttributeImpl<String> attribute = (AttributeImpl<String>) build();
    final AttributeReferenceBuilderImpl<V> referenceBuilder =
        new QNameAttributeReferenceBuilderImpl<V>(attribute, referenceTargetElement);
    setAttributeReference(referenceBuilder);
    return referenceBuilder;
  }

  @Override
  public <V extends ModelElementInstance> AttributeReferenceBuilder<V> idAttributeReference(
      final Class<V> referenceTargetElement) {
    final AttributeImpl<String> attribute = (AttributeImpl<String>) build();
    final AttributeReferenceBuilderImpl<V> referenceBuilder =
        new AttributeReferenceBuilderImpl<V>(attribute, referenceTargetElement);
    setAttributeReference(referenceBuilder);
    return referenceBuilder;
  }

  @Override
  @SuppressWarnings("rawtypes")
  public <V extends ModelElementInstance>
      AttributeReferenceCollectionBuilder<V> idAttributeReferenceCollection(
          final Class<V> referenceTargetElement,
          final Class<? extends AttributeReferenceCollection> attributeReferenceCollection) {
    final AttributeImpl<String> attribute = (AttributeImpl<String>) build();
    final AttributeReferenceCollectionBuilder<V> referenceBuilder =
        new AttributeReferenceCollectionBuilderImpl<V>(
            attribute, referenceTargetElement, attributeReferenceCollection);
    setAttributeReference(referenceBuilder);
    return referenceBuilder;
  }

  protected <V extends ModelElementInstance> void setAttributeReference(
      final AttributeReferenceBuilder<V> referenceBuilder) {
    if (this.referenceBuilder != null) {
      throw new ModelException("An attribute cannot have more than one reference");
    }
    this.referenceBuilder = referenceBuilder;
  }

  @Override
  public void performModelBuild(final Model model) {
    super.performModelBuild(model);
    if (referenceBuilder != null) {
      ((ModelBuildOperation) referenceBuilder).performModelBuild(model);
    }
  }
}
