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
package com.anyilanxin.kunpeng.bpm.model.xml.impl.instance;

import com.anyilanxin.kunpeng.bpm.model.xml.Model;
import com.anyilanxin.kunpeng.bpm.model.xml.ModelBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.ModelException;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.ModelInstanceImpl;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.type.ModelElementTypeImpl;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.type.attribute.AttributeImpl;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.type.reference.ReferenceImpl;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.util.ModelUtil;
import com.anyilanxin.kunpeng.bpm.model.xml.instance.DomElement;
import com.anyilanxin.kunpeng.bpm.model.xml.instance.ModelElementInstance;
import com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementType;
import com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementTypeBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.type.attribute.Attribute;
import com.anyilanxin.kunpeng.bpm.model.xml.type.reference.Reference;
import java.util.*;

/**
 * Base class for implementing Model Elements.
 *
 * @author Daniel Meyer
 */
public class ModelElementInstanceImpl implements ModelElementInstance {

  /** the containing model instance */
  protected final ModelInstanceImpl modelInstance;

  /** the wrapped DOM {@link DomElement} */
  private final DomElement domElement;

  /** the implementing model element type */
  private final ModelElementTypeImpl elementType;

  public static void registerType(final ModelBuilder modelBuilder) {
    final ModelElementTypeBuilder typeBuilder =
        modelBuilder.defineType(ModelElementInstance.class, "").abstractType();

    typeBuilder.build();
  }

  public ModelElementInstanceImpl(final ModelTypeInstanceContext instanceContext) {
    domElement = instanceContext.getDomElement();
    modelInstance = instanceContext.getModel();
    elementType = instanceContext.getModelType();
  }

  @Override
  public DomElement getDomElement() {
    return domElement;
  }

  @Override
  public ModelInstanceImpl getModelInstance() {
    return modelInstance;
  }

  @Override
  public ModelElementInstance getParentElement() {
    final DomElement parentElement = domElement.getParentElement();
    if (parentElement != null) {
      return ModelUtil.getModelElement(parentElement, modelInstance);
    } else {
      return null;
    }
  }

  @Override
  public ModelElementType getElementType() {
    return elementType;
  }

  @Override
  public String getAttributeValue(final String attributeName) {
    return domElement.getAttribute(attributeName);
  }

  @Override
  public String getAttributeValueNs(final String namespaceUri, final String attributeName) {
    return domElement.getAttribute(namespaceUri, attributeName);
  }

  @Override
  public void setAttributeValue(final String attributeName, final String xmlValue) {
    setAttributeValue(attributeName, xmlValue, false, true);
  }

  @Override
  public void setAttributeValue(
      final String attributeName, final String xmlValue, final boolean isIdAttribute) {
    setAttributeValue(attributeName, xmlValue, isIdAttribute, true);
  }

  @Override
  public void setAttributeValue(
      final String attributeName,
      final String xmlValue,
      final boolean isIdAttribute,
      final boolean withReferenceUpdate) {
    final String oldValue = getAttributeValue(attributeName);
    if (isIdAttribute) {
      domElement.setIdAttribute(attributeName, xmlValue);
    } else {
      domElement.setAttribute(attributeName, xmlValue);
    }
    final Attribute<?> attribute = elementType.getAttribute(attributeName);
    if (attribute != null && withReferenceUpdate) {
      ((AttributeImpl<?>) attribute).updateIncomingReferences(this, xmlValue, oldValue);
    }
  }

  @Override
  public void setAttributeValueNs(
      final String namespaceUri, final String attributeName, final String xmlValue) {
    setAttributeValueNs(namespaceUri, attributeName, xmlValue, false, true);
  }

  @Override
  public void setAttributeValueNs(
      final String namespaceUri,
      final String attributeName,
      final String xmlValue,
      final boolean isIdAttribute) {
    setAttributeValueNs(namespaceUri, attributeName, xmlValue, isIdAttribute, true);
  }

  @Override
  public void setAttributeValueNs(
      final String namespaceUri,
      final String attributeName,
      final String xmlValue,
      final boolean isIdAttribute,
      final boolean withReferenceUpdate) {
    final String namespaceForSetting = determineNamespace(namespaceUri, attributeName);
    final String oldValue = getAttributeValueNs(namespaceForSetting, attributeName);
    if (isIdAttribute) {
      domElement.setIdAttribute(namespaceForSetting, attributeName, xmlValue);
    } else {
      domElement.setAttribute(namespaceForSetting, attributeName, xmlValue);
    }
    final Attribute<?> attribute = elementType.getAttribute(attributeName);
    if (attribute != null && withReferenceUpdate) {
      ((AttributeImpl<?>) attribute).updateIncomingReferences(this, xmlValue, oldValue);
    }
  }

  private String determineNamespace(final String intendedNamespace, final String attributeName) {
    final boolean isSetInIntendedNamespace =
        getAttributeValueNs(intendedNamespace, attributeName) != null;

    if (isSetInIntendedNamespace) {
      return intendedNamespace;
    } else {
      final Set<String> alternativeNamespaces =
          modelInstance.getModel().getAlternativeNamespaces(intendedNamespace);

      if (alternativeNamespaces != null) {
        for (final String alternativeNamespace : alternativeNamespaces) {
          if (getAttributeValueNs(alternativeNamespace, attributeName) != null) {
            return alternativeNamespace;
          }
        }
      }

      // default to intended namespace
      return intendedNamespace;
    }
  }

  @Override
  public void removeAttribute(final String attributeName) {
    final Attribute<?> attribute = elementType.getAttribute(attributeName);
    if (attribute != null) {
      final Object identifier = attribute.getValue(this);
      if (identifier != null) {
        ((AttributeImpl<?>) attribute).unlinkReference(this, identifier);
      }
    }
    domElement.removeAttribute(attributeName);
  }

  @Override
  public void removeAttributeNs(final String namespaceUri, final String attributeName) {
    final Attribute<?> attribute = elementType.getAttribute(attributeName);
    if (attribute != null) {
      final Object identifier = attribute.getValue(this);
      if (identifier != null) {
        ((AttributeImpl<?>) attribute).unlinkReference(this, identifier);
      }
    }
    domElement.removeAttribute(namespaceUri, attributeName);
  }

  @Override
  public String getTextContent() {
    return getRawTextContent().trim();
  }

  @Override
  public void setTextContent(final String textContent) {
    domElement.setTextContent(textContent);
  }

  @Override
  public String getRawTextContent() {
    return domElement.getTextContent();
  }

  @Override
  public ModelElementInstance getUniqueChildElementByNameNs(
      final String namespaceUri, final String elementName) {
    final Model model = modelInstance.getModel();
    final List<DomElement> childElements =
        domElement.getChildElementsByNameNs(
            asSet(namespaceUri, model.getAlternativeNamespaces(namespaceUri)), elementName);
    if (!childElements.isEmpty()) {
      return ModelUtil.getModelElement(childElements.get(0), modelInstance);
    } else {
      return null;
    }
  }

  @Override
  public ModelElementInstance getUniqueChildElementByType(
      final Class<? extends ModelElementInstance> elementType) {
    final List<DomElement> childElements =
        domElement.getChildElementsByType(modelInstance, elementType);

    if (!childElements.isEmpty()) {
      return ModelUtil.getModelElement(childElements.get(0), modelInstance);
    } else {
      return null;
    }
  }

  @Override
  public void setUniqueChildElementByNameNs(final ModelElementInstance newChild) {
    ModelUtil.ensureInstanceOf(newChild, ModelElementInstanceImpl.class);
    final ModelElementInstanceImpl newChildElement = (ModelElementInstanceImpl) newChild;

    final DomElement childElement = newChildElement.getDomElement();
    final ModelElementInstance existingChild =
        getUniqueChildElementByNameNs(childElement.getNamespaceURI(), childElement.getLocalName());
    if (existingChild == null) {
      addChildElement(newChild);
    } else {
      replaceChildElement(existingChild, newChildElement);
    }
  }

  @Override
  public void replaceChildElement(
      final ModelElementInstance existingChild, final ModelElementInstance newChild) {
    final DomElement existingChildDomElement = existingChild.getDomElement();
    final DomElement newChildDomElement = newChild.getDomElement();

    // unlink (remove all references) of child elements
    ((ModelElementInstanceImpl) existingChild).unlinkAllChildReferences();

    // update incoming references from old to new child element
    updateIncomingReferences(existingChild, newChild);

    // replace the existing child with the new child in the DOM
    domElement.replaceChild(newChildDomElement, existingChildDomElement);

    // execute after replacement updates
    newChild.updateAfterReplacement();
  }

  @SuppressWarnings("unchecked")
  private void updateIncomingReferences(
      final ModelElementInstance oldInstance, final ModelElementInstance newInstance) {
    final String oldId = oldInstance.getAttributeValue("id");
    final String newId = newInstance.getAttributeValue("id");

    if (oldId == null || newId == null) {
      return;
    }

    final Collection<Attribute<?>> attributes =
        ((ModelElementTypeImpl) oldInstance.getElementType()).getAllAttributes();
    for (final Attribute<?> attribute : attributes) {
      if (attribute.isIdAttribute()) {
        for (final Reference<?> incomingReference : attribute.getIncomingReferences()) {
          ((ReferenceImpl<ModelElementInstance>) incomingReference)
              .referencedElementUpdated(newInstance, oldId, newId);
        }
      }
    }
  }

  @Override
  public void replaceWithElement(final ModelElementInstance newElement) {
    final ModelElementInstanceImpl parentElement = (ModelElementInstanceImpl) getParentElement();
    if (parentElement != null) {
      parentElement.replaceChildElement(this, newElement);
    } else {
      throw new ModelException("Unable to remove replace without parent");
    }
  }

  @Override
  public void addChildElement(final ModelElementInstance newChild) {
    ModelUtil.ensureInstanceOf(newChild, ModelElementInstanceImpl.class);
    final ModelElementInstance elementToInsertAfter = findElementToInsertAfter(newChild);
    insertElementAfter(newChild, elementToInsertAfter);
  }

  @Override
  public boolean removeChildElement(final ModelElementInstance child) {
    final ModelElementInstanceImpl childImpl = (ModelElementInstanceImpl) child;
    childImpl.unlinkAllReferences();
    childImpl.unlinkAllChildReferences();
    return domElement.removeChild(child.getDomElement());
  }

  @Override
  public Collection<ModelElementInstance> getChildElementsByType(
      final ModelElementType childElementType) {
    final List<ModelElementInstance> instances = new ArrayList<ModelElementInstance>();
    for (final ModelElementType extendingType : childElementType.getExtendingTypes()) {
      instances.addAll(getChildElementsByType(extendingType));
    }
    final Model model = modelInstance.getModel();
    final Set<String> alternativeNamespaces =
        model.getAlternativeNamespaces(childElementType.getTypeNamespace());
    final List<DomElement> elements =
        domElement.getChildElementsByNameNs(
            asSet(childElementType.getTypeNamespace(), alternativeNamespaces),
            childElementType.getTypeName());
    instances.addAll(ModelUtil.getModelElementCollection(elements, modelInstance));
    return instances;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T extends ModelElementInstance> Collection<T> getChildElementsByType(
      final Class<T> childElementClass) {
    return (Collection<T>)
        getChildElementsByType(getModelInstance().getModel().getType(childElementClass));
  }

  /**
   * Returns the element after which the new element should be inserted in the DOM document.
   *
   * @param elementToInsert the new element to insert
   * @return the element to insert after or null
   */
  private ModelElementInstance findElementToInsertAfter(
      final ModelElementInstance elementToInsert) {
    final List<ModelElementType> childElementTypes = elementType.getAllChildElementTypes();
    final List<DomElement> childDomElements = domElement.getChildElements();
    final Collection<ModelElementInstance> childElements =
        ModelUtil.getModelElementCollection(childDomElements, modelInstance);

    ModelElementInstance insertAfterElement = null;
    final int newElementTypeIndex =
        ModelUtil.getIndexOfElementType(elementToInsert, childElementTypes);
    for (final ModelElementInstance childElement : childElements) {
      final int childElementTypeIndex =
          ModelUtil.getIndexOfElementType(childElement, childElementTypes);
      if (newElementTypeIndex >= childElementTypeIndex) {
        insertAfterElement = childElement;
      } else {
        break;
      }
    }
    return insertAfterElement;
  }

  @Override
  public void insertElementAfter(
      final ModelElementInstance elementToInsert, final ModelElementInstance insertAfterElement) {
    if (insertAfterElement == null || insertAfterElement.getDomElement() == null) {
      domElement.insertChildElementAfter(elementToInsert.getDomElement(), null);
    } else {
      domElement.insertChildElementAfter(
          elementToInsert.getDomElement(), insertAfterElement.getDomElement());
    }
  }

  @Override
  public void updateAfterReplacement() {
    // do nothing
  }

  /** Removes all reference to this. */
  private void unlinkAllReferences() {
    final Collection<Attribute<?>> attributes = elementType.getAllAttributes();
    for (final Attribute<?> attribute : attributes) {
      final Object identifier = attribute.getValue(this);
      if (identifier != null) {
        ((AttributeImpl<?>) attribute).unlinkReference(this, identifier);
      }
    }
  }

  /** Removes every reference to children of this. */
  private void unlinkAllChildReferences() {
    final List<ModelElementType> childElementTypes = elementType.getAllChildElementTypes();
    for (final ModelElementType type : childElementTypes) {
      final Collection<ModelElementInstance> childElementsForType = getChildElementsByType(type);
      for (final ModelElementInstance childElement : childElementsForType) {
        ((ModelElementInstanceImpl) childElement).unlinkAllReferences();
      }
    }
  }

  protected <T> Set<T> asSet(final T element, final Set<T> elements) {
    final Set<T> result = new HashSet<T>();
    result.add(element);

    if (elements != null) {
      result.addAll(elements);
    }

    return result;
  }

  @Override
  public int hashCode() {
    return domElement.hashCode();
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj == null) {
      return false;
    } else if (obj == this) {
      return true;
    } else if (!(obj instanceof ModelElementInstanceImpl)) {
      return false;
    } else {
      final ModelElementInstanceImpl other = (ModelElementInstanceImpl) obj;
      return other.domElement.equals(domElement);
    }
  }
}
