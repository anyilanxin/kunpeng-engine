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
import com.anyilanxin.kunpeng.bpm.model.xml.UnsupportedModelOperationException;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.instance.ModelElementInstanceImpl;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.type.ModelElementTypeImpl;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.util.ModelUtil;
import com.anyilanxin.kunpeng.bpm.model.xml.instance.DomElement;
import com.anyilanxin.kunpeng.bpm.model.xml.instance.ModelElementInstance;
import com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementType;
import com.anyilanxin.kunpeng.bpm.model.xml.type.child.ChildElementCollection;
import java.util.Collection;
import java.util.Iterator;

/**
 * This collection is a view on an the children of a Model Element.
 *
 * @author Daniel Meyer
 */
public class ChildElementCollectionImpl<T extends ModelElementInstance>
    implements ChildElementCollection<T> {

  protected final Class<T> childElementTypeClass;

  /** the containing type of the collection */
  private final ModelElementType parentElementType;

  /** the minimal count of child elements in the collection */
  private int minOccurs = 0;

  /**
   * the maximum count of child elements in the collection. An unbounded collection has a negative
   * maxOccurs.
   */
  protected int maxOccurs = -1;

  /** indicates whether this collection is mutable. */
  private boolean isMutable = true;

  public ChildElementCollectionImpl(
      final Class<T> childElementTypeClass, final ModelElementTypeImpl parentElementType) {
    this.childElementTypeClass = childElementTypeClass;
    this.parentElementType = parentElementType;
  }

  public void setImmutable() {
    setMutable(false);
  }

  public void setMutable(final boolean isMutable) {
    this.isMutable = isMutable;
  }

  @Override
  public boolean isImmutable() {
    return !isMutable;
  }

  // view /////////////////////////////////////////////////////////

  /**
   * Internal method providing access to the view represented by this collection.
   *
   * @return the view represented by this collection
   */
  private Collection<DomElement> getView(final ModelElementInstanceImpl modelElement) {
    return modelElement
        .getDomElement()
        .getChildElementsByType(modelElement.getModelInstance(), childElementTypeClass);
  }

  @Override
  public int getMinOccurs() {
    return minOccurs;
  }

  public void setMinOccurs(final int minOccurs) {
    this.minOccurs = minOccurs;
  }

  @Override
  public int getMaxOccurs() {
    return maxOccurs;
  }

  @Override
  public ModelElementType getChildElementType(final Model model) {
    return model.getType(childElementTypeClass);
  }

  @Override
  public Class<T> getChildElementTypeClass() {
    return childElementTypeClass;
  }

  @Override
  public ModelElementType getParentElementType() {
    return parentElementType;
  }

  public void setMaxOccurs(final int maxOccurs) {
    this.maxOccurs = maxOccurs;
  }

  /** the "add" operation used by the collection */
  private void performAddOperation(final ModelElementInstanceImpl modelElement, final T e) {
    modelElement.addChildElement(e);
  }

  /** the "remove" operation used by this collection */
  private boolean performRemoveOperation(
      final ModelElementInstanceImpl modelElement, final Object e) {
    return modelElement.removeChildElement((ModelElementInstanceImpl) e);
  }

  /** the "clear" operation used by this collection */
  private void performClearOperation(
      final ModelElementInstanceImpl modelElement, final Collection<DomElement> elementsToRemove) {
    final Collection<ModelElementInstance> modelElements =
        ModelUtil.getModelElementCollection(elementsToRemove, modelElement.getModelInstance());
    for (final ModelElementInstance element : modelElements) {
      modelElement.removeChildElement(element);
    }
  }

  @Override
  public Collection<T> get(final ModelElementInstance element) {

    final ModelElementInstanceImpl modelElement = (ModelElementInstanceImpl) element;

    return new Collection<T>() {

      @Override
      public boolean contains(final Object o) {
        if (o == null) {
          return false;

        } else if (!(o instanceof ModelElementInstanceImpl)) {
          return false;

        } else {
          return getView(modelElement).contains(((ModelElementInstanceImpl) o).getDomElement());
        }
      }

      @Override
      public boolean containsAll(final Collection<?> c) {
        for (final Object elementToCheck : c) {
          if (!contains(elementToCheck)) {
            return false;
          }
        }
        return true;
      }

      @Override
      public boolean isEmpty() {
        return getView(modelElement).isEmpty();
      }

      @Override
      public Iterator<T> iterator() {
        final Collection<T> modelElementCollection =
            ModelUtil.getModelElementCollection(
                getView(modelElement), modelElement.getModelInstance());
        return modelElementCollection.iterator();
      }

      @Override
      public Object[] toArray() {
        final Collection<T> modelElementCollection =
            ModelUtil.getModelElementCollection(
                getView(modelElement), modelElement.getModelInstance());
        return modelElementCollection.toArray();
      }

      @Override
      public <U> U[] toArray(final U[] a) {
        final Collection<T> modelElementCollection =
            ModelUtil.getModelElementCollection(
                getView(modelElement), modelElement.getModelInstance());
        return modelElementCollection.toArray(a);
      }

      @Override
      public int size() {
        return getView(modelElement).size();
      }

      @Override
      public boolean add(final T e) {
        if (!isMutable) {
          throw new UnsupportedModelOperationException("add()", "collection is immutable");
        }
        performAddOperation(modelElement, e);
        return true;
      }

      @Override
      public boolean addAll(final Collection<? extends T> c) {
        if (!isMutable) {
          throw new UnsupportedModelOperationException("addAll()", "collection is immutable");
        }
        boolean result = false;
        for (final T t : c) {
          result |= add(t);
        }
        return result;
      }

      @Override
      public void clear() {
        if (!isMutable) {
          throw new UnsupportedModelOperationException("clear()", "collection is immutable");
        }
        final Collection<DomElement> view = getView(modelElement);
        performClearOperation(modelElement, view);
      }

      @Override
      public boolean remove(final Object e) {
        if (!isMutable) {
          throw new UnsupportedModelOperationException("remove()", "collection is immutable");
        }
        ModelUtil.ensureInstanceOf(e, ModelElementInstanceImpl.class);
        return performRemoveOperation(modelElement, e);
      }

      @Override
      public boolean removeAll(final Collection<?> c) {
        if (!isMutable) {
          throw new UnsupportedModelOperationException("removeAll()", "collection is immutable");
        }
        boolean result = false;
        for (final Object t : c) {
          result |= remove(t);
        }
        return result;
      }

      @Override
      public boolean retainAll(final Collection<?> c) {
        throw new UnsupportedModelOperationException("retainAll()", "not implemented");
      }
    };
  }
}
