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

import com.anyilanxin.kunpeng.bpm.model.xml.impl.instance.ModelElementInstanceImpl;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.type.ModelElementTypeImpl;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.util.ModelUtil;
import com.anyilanxin.kunpeng.bpm.model.xml.instance.ModelElementInstance;
import com.anyilanxin.kunpeng.bpm.model.xml.type.child.ChildElement;

/**
 * Represents a single Child Element (ie. maxOccurs = 1);
 *
 * @author Daniel Meyer
 */
public class ChildElementImpl<T extends ModelElementInstance> extends ChildElementCollectionImpl<T>
    implements ChildElement<T> {

  public ChildElementImpl(
      final Class<T> childElementTypeChild, final ModelElementTypeImpl parentElementType) {
    super(childElementTypeChild, parentElementType);
    maxOccurs = 1;
  }

  /** the add operation replaces the child */
  private void performAddOperation(final ModelElementInstanceImpl modelElement, final T e) {
    modelElement.setUniqueChildElementByNameNs(e);
  }

  @Override
  public void setChild(final ModelElementInstance element, final T newChildElement) {
    performAddOperation((ModelElementInstanceImpl) element, newChildElement);
  }

  @Override
  @SuppressWarnings("unchecked")
  public T getChild(final ModelElementInstance element) {
    final ModelElementInstanceImpl elementInstanceImpl = (ModelElementInstanceImpl) element;

    final ModelElementInstance childElement =
        elementInstanceImpl.getUniqueChildElementByType(childElementTypeClass);
    if (childElement != null) {
      ModelUtil.ensureInstanceOf(childElement, childElementTypeClass);
      return (T) childElement;
    } else {
      return null;
    }
  }

  @Override
  public boolean removeChild(final ModelElementInstance element) {
    final ModelElementInstanceImpl childElement = (ModelElementInstanceImpl) getChild(element);
    final ModelElementInstanceImpl elementInstanceImpl = (ModelElementInstanceImpl) element;
    return elementInstanceImpl.removeChildElement(childElement);
  }
}
