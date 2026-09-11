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
import com.anyilanxin.kunpeng.bpm.model.xml.UnsupportedModelOperationException;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.ModelInstanceImpl;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.instance.ModelElementInstanceImpl;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.type.ModelElementTypeImpl;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.util.ModelUtil;
import com.anyilanxin.kunpeng.bpm.model.xml.instance.DomDocument;
import com.anyilanxin.kunpeng.bpm.model.xml.instance.DomElement;
import com.anyilanxin.kunpeng.bpm.model.xml.instance.ModelElementInstance;
import com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementType;
import com.anyilanxin.kunpeng.bpm.model.xml.type.child.ChildElementCollection;
import com.anyilanxin.kunpeng.bpm.model.xml.type.reference.ElementReferenceCollection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 * @author Sebastian Menski
 */
public class ElementReferenceCollectionImpl<
        Target extends ModelElementInstance, Source extends ModelElementInstance>
    extends ReferenceImpl<Target> implements ElementReferenceCollection<Target, Source> {

  private final ChildElementCollection<Source> referenceSourceCollection;
  private ModelElementTypeImpl referenceSourceType;

  public ElementReferenceCollectionImpl(
      final ChildElementCollection<Source> referenceSourceCollection) {
    this.referenceSourceCollection = referenceSourceCollection;
  }

  @Override
  public ChildElementCollection<Source> getReferenceSourceCollection() {
    return referenceSourceCollection;
  }

  @Override
  protected void setReferenceIdentifier(
      final ModelElementInstance referenceSourceElement, final String referenceIdentifier) {
    referenceSourceElement.setTextContent(referenceIdentifier);
  }

  protected void performAddOperation(
      final ModelElementInstanceImpl referenceSourceParentElement,
      final Target referenceTargetElement) {
    final ModelInstanceImpl modelInstance = referenceSourceParentElement.getModelInstance();
    final String referenceTargetIdentifier =
        referenceTargetAttribute.getValue(referenceTargetElement);
    final ModelElementInstance existingElement =
        modelInstance.getModelElementById(referenceTargetIdentifier);

    if (existingElement == null || !existingElement.equals(referenceTargetElement)) {
      throw new ModelReferenceException(
          "Cannot create reference to model element "
              + referenceTargetElement
              + ": element is not part of model. Please connect element to the model first.");
    } else {
      final Collection<Source> referenceSourceElements =
          referenceSourceCollection.get(referenceSourceParentElement);
      final Source referenceSourceElement = modelInstance.newInstance(referenceSourceType);
      referenceSourceElements.add(referenceSourceElement);
      setReferenceIdentifier(referenceSourceElement, referenceTargetIdentifier);
    }
  }

  protected void performRemoveOperation(
      final ModelElementInstanceImpl referenceSourceParentElement,
      final Object referenceTargetElement) {
    final Collection<ModelElementInstance> referenceSourceChildElements =
        referenceSourceParentElement.getChildElementsByType(referenceSourceType);
    for (final ModelElementInstance referenceSourceChildElement : referenceSourceChildElements) {
      if (getReferenceTargetElement(referenceSourceChildElement).equals(referenceTargetElement)) {
        referenceSourceParentElement.removeChildElement(referenceSourceChildElement);
      }
    }
  }

  protected void performClearOperation(
      final ModelElementInstanceImpl referenceSourceParentElement,
      final Collection<DomElement> elementsToRemove) {
    for (final DomElement element : elementsToRemove) {
      referenceSourceParentElement.getDomElement().removeChild(element);
    }
  }

  @Override
  public String getReferenceIdentifier(final ModelElementInstance referenceSourceElement) {
    return referenceSourceElement.getTextContent();
  }

  @Override
  protected void updateReference(
      final ModelElementInstance referenceSourceElement,
      final String oldIdentifier,
      final String newIdentifier) {
    final String referencingTextContent = getReferenceIdentifier(referenceSourceElement);
    if (oldIdentifier != null && oldIdentifier.equals(referencingTextContent)) {
      setReferenceIdentifier(referenceSourceElement, newIdentifier);
    }
  }

  @Override
  protected void removeReference(
      final ModelElementInstance referenceSourceElement,
      final ModelElementInstance referenceTargetElement) {
    final ModelElementInstance parentElement = referenceSourceElement.getParentElement();
    final Collection<Source> childElementCollection = referenceSourceCollection.get(parentElement);
    childElementCollection.remove(referenceSourceElement);
  }

  public void setReferenceSourceElementType(final ModelElementTypeImpl referenceSourceType) {
    this.referenceSourceType = referenceSourceType;
  }

  @Override
  public ModelElementType getReferenceSourceElementType() {
    return referenceSourceType;
  }

  protected Collection<DomElement> getView(
      final ModelElementInstanceImpl referenceSourceParentElement) {
    final DomDocument document = referenceSourceParentElement.getModelInstance().getDocument();
    final Collection<Source> referenceSourceElements =
        referenceSourceCollection.get(referenceSourceParentElement);
    final Collection<DomElement> referenceTargetElements = new ArrayList<DomElement>();
    for (final Source referenceSourceElement : referenceSourceElements) {
      final String identifier = getReferenceIdentifier(referenceSourceElement);
      final DomElement referenceTargetElement = document.getElementById(identifier);
      if (referenceTargetElement != null) {
        referenceTargetElements.add(referenceTargetElement);
      } else {
        throw new ModelException("Unable to find a model element instance for id " + identifier);
      }
    }
    return referenceTargetElements;
  }

  @Override
  public Collection<Target> getReferenceTargetElements(
      final ModelElementInstanceImpl referenceSourceParentElement) {

    return new Collection<Target>() {

      @Override
      public int size() {
        return getView(referenceSourceParentElement).size();
      }

      @Override
      public boolean isEmpty() {
        return getView(referenceSourceParentElement).isEmpty();
      }

      @Override
      public boolean contains(final Object o) {
        if (o == null) {
          return false;
        } else if (!(o instanceof ModelElementInstanceImpl)) {
          return false;
        } else {
          return getView(referenceSourceParentElement)
              .contains(((ModelElementInstanceImpl) o).getDomElement());
        }
      }

      @Override
      public Iterator<Target> iterator() {
        final Collection<Target> modelElementCollection =
            ModelUtil.getModelElementCollection(
                getView(referenceSourceParentElement),
                referenceSourceParentElement.getModelInstance());
        return modelElementCollection.iterator();
      }

      @Override
      public Object[] toArray() {
        final Collection<Target> modelElementCollection =
            ModelUtil.getModelElementCollection(
                getView(referenceSourceParentElement),
                referenceSourceParentElement.getModelInstance());
        return modelElementCollection.toArray();
      }

      @Override
      public <T1> T1[] toArray(final T1[] a) {
        final Collection<Target> modelElementCollection =
            ModelUtil.getModelElementCollection(
                getView(referenceSourceParentElement),
                referenceSourceParentElement.getModelInstance());
        return modelElementCollection.toArray(a);
      }

      @Override
      public boolean add(final Target t) {
        if (referenceSourceCollection.isImmutable()) {
          throw new UnsupportedModelOperationException("add()", "collection is immutable");
        } else {
          if (!contains(t)) {
            performAddOperation(referenceSourceParentElement, t);
          }
          return true;
        }
      }

      @Override
      public boolean remove(final Object o) {
        if (referenceSourceCollection.isImmutable()) {
          throw new UnsupportedModelOperationException("remove()", "collection is immutable");
        } else {
          ModelUtil.ensureInstanceOf(o, ModelElementInstanceImpl.class);
          performRemoveOperation(referenceSourceParentElement, o);
          return true;
        }
      }

      @Override
      public boolean containsAll(final Collection<?> c) {
        final Collection<Target> modelElementCollection =
            ModelUtil.getModelElementCollection(
                getView(referenceSourceParentElement),
                referenceSourceParentElement.getModelInstance());
        return modelElementCollection.containsAll(c);
      }

      @Override
      public boolean addAll(final Collection<? extends Target> c) {
        if (referenceSourceCollection.isImmutable()) {
          throw new UnsupportedModelOperationException("addAll()", "collection is immutable");
        } else {
          boolean result = false;
          for (final Target o : c) {
            result |= add(o);
          }
          return result;
        }
      }

      @Override
      public boolean removeAll(final Collection<?> c) {
        if (referenceSourceCollection.isImmutable()) {
          throw new UnsupportedModelOperationException("removeAll()", "collection is immutable");
        } else {
          boolean result = false;
          for (final Object o : c) {
            result |= remove(o);
          }
          return result;
        }
      }

      @Override
      public boolean retainAll(final Collection<?> c) {
        throw new UnsupportedModelOperationException("retainAll()", "not implemented");
      }

      @Override
      public void clear() {
        if (referenceSourceCollection.isImmutable()) {
          throw new UnsupportedModelOperationException("clear()", "collection is immutable");
        } else {
          final Collection<DomElement> view = new ArrayList<DomElement>();
          for (final Source referenceSourceElement :
              referenceSourceCollection.get(referenceSourceParentElement)) {
            view.add(referenceSourceElement.getDomElement());
          }
          performClearOperation(referenceSourceParentElement, view);
        }
      }
    };
  }
}
