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
package com.anyilanxin.kunpeng.bpm.model.xml.type.reference;

import com.anyilanxin.kunpeng.bpm.model.xml.ModelException;
import com.anyilanxin.kunpeng.bpm.model.xml.UnsupportedModelOperationException;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.ModelInstanceImpl;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.instance.ModelElementInstanceImpl;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.type.attribute.AttributeImpl;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.type.reference.AttributeReferenceImpl;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.util.ModelUtil;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.util.StringUtil;
import com.anyilanxin.kunpeng.bpm.model.xml.instance.DomDocument;
import com.anyilanxin.kunpeng.bpm.model.xml.instance.DomElement;
import com.anyilanxin.kunpeng.bpm.model.xml.instance.ModelElementInstance;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * @author Roman Smirnov
 * @author Sebastian Menski
 */
public abstract class AttributeReferenceCollection<T extends ModelElementInstance>
    extends AttributeReferenceImpl<T> implements AttributeReference<T> {

  protected String separator = " ";

  public AttributeReferenceCollection(final AttributeImpl<String> referenceSourceAttribute) {
    super(referenceSourceAttribute);
  }

  @Override
  protected void updateReference(
      final ModelElementInstance referenceSourceElement,
      final String oldIdentifier,
      final String newIdentifier) {
    String referencingIdentifier = getReferenceIdentifier(referenceSourceElement);
    final List<String> references =
        StringUtil.splitListBySeparator(referencingIdentifier, separator);
    if (oldIdentifier != null && references.contains(oldIdentifier)) {
      referencingIdentifier = referencingIdentifier.replace(oldIdentifier, newIdentifier);
      setReferenceIdentifier(referenceSourceElement, newIdentifier);
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  protected void removeReference(
      final ModelElementInstance referenceSourceElement,
      final ModelElementInstance referenceTargetElement) {
    String identifier = getReferenceIdentifier(referenceSourceElement);
    final List<String> references = StringUtil.splitListBySeparator(identifier, separator);
    final String identifierToRemove = getTargetElementIdentifier((T) referenceTargetElement);
    references.remove(identifierToRemove);
    identifier = StringUtil.joinList(references, separator);
    setReferenceIdentifier(referenceSourceElement, identifier);
  }

  protected abstract String getTargetElementIdentifier(T referenceTargetElement);

  private Collection<DomElement> getView(final ModelElementInstance referenceSourceElement) {
    final DomDocument document = referenceSourceElement.getModelInstance().getDocument();

    final String identifier = getReferenceIdentifier(referenceSourceElement);
    final List<String> references = StringUtil.splitListBySeparator(identifier, separator);

    final Collection<DomElement> referenceTargetElements = new ArrayList<DomElement>();
    for (final String reference : references) {
      final DomElement referenceTargetElement = document.getElementById(reference);
      if (referenceTargetElement != null) {
        referenceTargetElements.add(referenceTargetElement);
      } else {
        throw new ModelException("Unable to find a model element instance for id " + identifier);
      }
    }
    return referenceTargetElements;
  }

  public Collection<T> getReferenceTargetElements(
      final ModelElementInstance referenceSourceElement) {

    return new Collection<T>() {

      @Override
      public int size() {
        return getView(referenceSourceElement).size();
      }

      @Override
      public boolean isEmpty() {
        return getView(referenceSourceElement).isEmpty();
      }

      @Override
      public boolean contains(final Object o) {
        if (o == null) {
          return false;
        } else if (!(o instanceof ModelElementInstanceImpl)) {
          return false;
        } else {
          return getView(referenceSourceElement)
              .contains(((ModelElementInstanceImpl) o).getDomElement());
        }
      }

      @Override
      public Iterator<T> iterator() {
        final Collection<T> modelElementCollection =
            ModelUtil.getModelElementCollection(
                getView(referenceSourceElement),
                (ModelInstanceImpl) referenceSourceElement.getModelInstance());
        return modelElementCollection.iterator();
      }

      @Override
      public Object[] toArray() {
        final Collection<T> modelElementCollection =
            ModelUtil.getModelElementCollection(
                getView(referenceSourceElement),
                (ModelInstanceImpl) referenceSourceElement.getModelInstance());
        return modelElementCollection.toArray();
      }

      @Override
      public <T1> T1[] toArray(final T1[] a) {
        final Collection<T> modelElementCollection =
            ModelUtil.getModelElementCollection(
                getView(referenceSourceElement),
                (ModelInstanceImpl) referenceSourceElement.getModelInstance());
        return modelElementCollection.toArray(a);
      }

      @Override
      public boolean add(final T t) {
        if (!contains(t)) {
          performAddOperation(referenceSourceElement, t);
        }
        return true;
      }

      @Override
      public boolean remove(final Object o) {
        ModelUtil.ensureInstanceOf(o, ModelElementInstanceImpl.class);
        performRemoveOperation(referenceSourceElement, o);
        return true;
      }

      @Override
      public boolean containsAll(final Collection<?> c) {
        final Collection<T> modelElementCollection =
            ModelUtil.getModelElementCollection(
                getView(referenceSourceElement),
                (ModelInstanceImpl) referenceSourceElement.getModelInstance());
        return modelElementCollection.containsAll(c);
      }

      @Override
      public boolean addAll(final Collection<? extends T> c) {
        boolean result = false;
        for (final T o : c) {
          result |= add(o);
        }
        return result;
      }

      @Override
      public boolean removeAll(final Collection<?> c) {
        boolean result = false;
        for (final Object o : c) {
          result |= remove(o);
        }
        return result;
      }

      @Override
      public boolean retainAll(final Collection<?> c) {
        throw new UnsupportedModelOperationException("retainAll()", "not implemented");
      }

      @Override
      public void clear() {
        performClearOperation(referenceSourceElement);
      }
    };
  }

  protected void performClearOperation(final ModelElementInstance referenceSourceElement) {
    setReferenceIdentifier(referenceSourceElement, "");
  }

  @Override
  protected void setReferenceIdentifier(
      final ModelElementInstance referenceSourceElement, final String referenceIdentifier) {
    if (referenceIdentifier != null && !referenceIdentifier.isEmpty()) {
      super.setReferenceIdentifier(referenceSourceElement, referenceIdentifier);
    } else {
      referenceSourceAttribute.removeAttribute(referenceSourceElement);
    }
  }

  /**
   * @param referenceSourceElement
   * @param o
   */
  protected void performRemoveOperation(
      final ModelElementInstance referenceSourceElement, final Object o) {
    removeReference(referenceSourceElement, (ModelElementInstance) o);
  }

  protected void performAddOperation(
      final ModelElementInstance referenceSourceElement, final T referenceTargetElement) {
    String identifier = getReferenceIdentifier(referenceSourceElement);
    final List<String> references = StringUtil.splitListBySeparator(identifier, separator);

    final String targetIdentifier = getTargetElementIdentifier(referenceTargetElement);
    references.add(targetIdentifier);

    identifier = StringUtil.joinList(references, separator);

    setReferenceIdentifier(referenceSourceElement, identifier);
  }
}
