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

import com.anyilanxin.kunpeng.bpm.model.xml.ModelException;
import com.anyilanxin.kunpeng.bpm.model.xml.ModelReferenceException;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.ModelInstanceImpl;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.instance.ModelElementInstanceImpl;
import com.anyilanxin.kunpeng.bpm.model.xml.instance.ModelElementInstance;
import com.anyilanxin.kunpeng.bpm.model.xml.type.child.ChildElement;
import com.anyilanxin.kunpeng.bpm.model.xml.type.reference.ElementReference;

/**
 * @author Sebastian Menski
 */
public class ElementReferenceImpl<
        Target extends ModelElementInstance, Source extends ModelElementInstance>
    extends ElementReferenceCollectionImpl<Target, Source>
    implements ElementReference<Target, Source> {

  public ElementReferenceImpl(final ChildElement<Source> referenceSourceCollection) {
    super(referenceSourceCollection);
  }

  private ChildElement<Source> getReferenceSourceChild() {
    return (ChildElement<Source>) getReferenceSourceCollection();
  }

  @Override
  public Source getReferenceSource(final ModelElementInstance referenceSourceParent) {
    return getReferenceSourceChild().getChild(referenceSourceParent);
  }

  private void setReferenceSource(
      final ModelElementInstance referenceSourceParent, final Source referenceSource) {
    getReferenceSourceChild().setChild(referenceSourceParent, referenceSource);
  }

  @Override
  @SuppressWarnings("unchecked")
  public Target getReferenceTargetElement(
      final ModelElementInstanceImpl referenceSourceParentElement) {
    final Source referenceSource = getReferenceSource(referenceSourceParentElement);
    if (referenceSource != null) {
      final String identifier = getReferenceIdentifier(referenceSource);
      final ModelElementInstance referenceTargetElement =
          referenceSourceParentElement.getModelInstance().getModelElementById(identifier);
      if (referenceTargetElement != null) {
        return (Target) referenceTargetElement;
      } else {
        throw new ModelException("Unable to find a model element instance for id " + identifier);
      }
    } else {
      return null;
    }
  }

  @Override
  public void setReferenceTargetElement(
      final ModelElementInstanceImpl referenceSourceParentElement,
      final Target referenceTargetElement) {
    final ModelInstanceImpl modelInstance = referenceSourceParentElement.getModelInstance();
    final String identifier = referenceTargetAttribute.getValue(referenceTargetElement);
    final ModelElementInstance existingElement = modelInstance.getModelElementById(identifier);

    if (existingElement == null || !existingElement.equals(referenceTargetElement)) {
      throw new ModelReferenceException(
          "Cannot create reference to model element "
              + referenceTargetElement
              + ": element is not part of model. Please connect element to the model first.");
    } else {
      final Source referenceSourceElement =
          modelInstance.newInstance(getReferenceSourceElementType());
      setReferenceSource(referenceSourceParentElement, referenceSourceElement);
      setReferenceIdentifier(referenceSourceElement, identifier);
    }
  }

  @Override
  public void clearReferenceTargetElement(
      final ModelElementInstanceImpl referenceSourceParentElement) {
    getReferenceSourceChild().removeChild(referenceSourceParentElement);
  }
}
