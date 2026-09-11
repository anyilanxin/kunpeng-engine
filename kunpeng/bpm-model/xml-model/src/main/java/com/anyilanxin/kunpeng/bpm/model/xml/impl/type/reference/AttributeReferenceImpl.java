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

import com.anyilanxin.kunpeng.bpm.model.xml.impl.type.attribute.AttributeImpl;
import com.anyilanxin.kunpeng.bpm.model.xml.instance.ModelElementInstance;
import com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementType;
import com.anyilanxin.kunpeng.bpm.model.xml.type.attribute.Attribute;
import com.anyilanxin.kunpeng.bpm.model.xml.type.reference.AttributeReference;

/**
 * @author Sebastian Menski
 */
public class AttributeReferenceImpl<T extends ModelElementInstance> extends ReferenceImpl<T>
    implements AttributeReference<T> {

  protected final AttributeImpl<String> referenceSourceAttribute;

  public AttributeReferenceImpl(final AttributeImpl<String> referenceSourceAttribute) {
    this.referenceSourceAttribute = referenceSourceAttribute;
  }

  @Override
  public String getReferenceIdentifier(final ModelElementInstance referenceSourceElement) {
    return referenceSourceAttribute.getValue(referenceSourceElement);
  }

  @Override
  protected void setReferenceIdentifier(
      final ModelElementInstance referenceSourceElement, final String referenceIdentifier) {
    referenceSourceAttribute.setValue(referenceSourceElement, referenceIdentifier);
  }

  /**
   * Get the reference source attribute
   *
   * @return the reference source attribute
   */
  @Override
  public Attribute<String> getReferenceSourceAttribute() {
    return referenceSourceAttribute;
  }

  @Override
  public ModelElementType getReferenceSourceElementType() {
    return referenceSourceAttribute.getOwningElementType();
  }

  @Override
  protected void updateReference(
      final ModelElementInstance referenceSourceElement,
      final String oldIdentifier,
      final String newIdentifier) {
    final String referencingAttributeValue = getReferenceIdentifier(referenceSourceElement);
    if (oldIdentifier != null && oldIdentifier.equals(referencingAttributeValue)) {
      setReferenceIdentifier(referenceSourceElement, newIdentifier);
    }
  }

  @Override
  protected void removeReference(
      final ModelElementInstance referenceSourceElement,
      final ModelElementInstance referenceTargetElement) {
    referenceSourceAttribute.removeAttribute(referenceSourceElement);
  }
}
