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

import com.anyilanxin.kunpeng.bpm.model.xml.impl.type.reference.ReferenceImpl;
import com.anyilanxin.kunpeng.bpm.model.xml.instance.ModelElementInstance;
import com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementType;
import com.anyilanxin.kunpeng.bpm.model.xml.type.attribute.Attribute;
import com.anyilanxin.kunpeng.bpm.model.xml.type.reference.Reference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Base class for implementing primitive value attributes
 *
 * @author Daniel Meyer
 */
public abstract class AttributeImpl<T> implements Attribute<T> {

  /** the local name of the attribute */
  private String attributeName;

  /** the namespace for this attribute */
  private String namespaceUri;

  /**
   * the default value for this attribute: the default value is returned by the {@link
   * #getValue(ModelElementInstance)} method in case the attribute is not set on the domElement.
   */
  private T defaultValue;

  private boolean isRequired = false;

  private boolean isIdAttribute = false;

  private final List<Reference<?>> outgoingReferences = new ArrayList<Reference<?>>();

  private final List<Reference<?>> incomingReferences = new ArrayList<Reference<?>>();

  private final ModelElementType owningElementType;

  AttributeImpl(final ModelElementType owningElementType) {
    this.owningElementType = owningElementType;
  }

  /**
   * to be implemented by subclasses: converts the raw (String) value of the attribute to the type
   * required by the model
   *
   * @return the converted value
   */
  protected abstract T convertXmlValueToModelValue(String rawValue);

  /**
   * to be implemented by subclasses: converts the raw (String) value of the attribute to the type
   * required by the model
   *
   * @return the converted value
   */
  protected abstract String convertModelValueToXmlValue(T modelValue);

  @Override
  public ModelElementType getOwningElementType() {
    return owningElementType;
  }

  /**
   * returns the value of the attribute.
   *
   * @return the value of the attribute.
   */
  @Override
  public T getValue(final ModelElementInstance modelElement) {
    String value;
    if (namespaceUri == null) {
      value = modelElement.getAttributeValue(attributeName);
    } else {
      value = modelElement.getAttributeValueNs(namespaceUri, attributeName);
      if (value == null) {
        final Set<String> alternativeNamespaces =
            owningElementType.getModel().getAlternativeNamespaces(namespaceUri);

        if (alternativeNamespaces != null) {
          final Iterator<String> namespaceIt = alternativeNamespaces.iterator();

          while (value == null && namespaceIt.hasNext()) {
            value = modelElement.getAttributeValueNs(namespaceIt.next(), attributeName);
          }
        }
      }
    }

    // default value
    if (value == null && defaultValue != null) {
      return defaultValue;
    } else {
      return convertXmlValueToModelValue(value);
    }
  }

  /**
   * sets the value of the attribute.
   *
   * <p>the value of the attribute.
   */
  @Override
  public void setValue(final ModelElementInstance modelElement, final T value) {
    setValue(modelElement, value, true);
  }

  @Override
  public void setValue(
      final ModelElementInstance modelElement, final T value, final boolean withReferenceUpdate) {
    final String xmlValue = convertModelValueToXmlValue(value);
    if (namespaceUri == null) {
      modelElement.setAttributeValue(attributeName, xmlValue, isIdAttribute, withReferenceUpdate);
    } else {
      modelElement.setAttributeValueNs(
          namespaceUri, attributeName, xmlValue, isIdAttribute, withReferenceUpdate);
    }
  }

  public void updateIncomingReferences(
      final ModelElementInstance modelElement,
      final String newIdentifier,
      final String oldIdentifier) {
    if (!incomingReferences.isEmpty()) {
      for (final Reference<?> incomingReference : incomingReferences) {
        ((ReferenceImpl<?>) incomingReference)
            .referencedElementUpdated(modelElement, oldIdentifier, newIdentifier);
      }
    }
  }

  @Override
  public T getDefaultValue() {
    return defaultValue;
  }

  public void setDefaultValue(final T defaultValue) {
    this.defaultValue = defaultValue;
  }

  @Override
  public boolean isRequired() {
    return isRequired;
  }

  /** */
  public void setRequired(final boolean required) {
    isRequired = required;
  }

  /**
   * @param namespaceUri the namespaceUri to set
   */
  public void setNamespaceUri(final String namespaceUri) {
    this.namespaceUri = namespaceUri;
  }

  /**
   * @return the namespaceUri
   */
  @Override
  public String getNamespaceUri() {
    return namespaceUri;
  }

  @Override
  public boolean isIdAttribute() {
    return isIdAttribute;
  }

  /** Indicate whether this attribute is an Id attribute */
  public void setId() {
    isIdAttribute = true;
  }

  /**
   * @return the attributeName
   */
  @Override
  public String getAttributeName() {
    return attributeName;
  }

  /**
   * @param attributeName the attributeName to set
   */
  public void setAttributeName(final String attributeName) {
    this.attributeName = attributeName;
  }

  @Override
  public void removeAttribute(final ModelElementInstance modelElement) {
    if (namespaceUri == null) {
      modelElement.removeAttribute(attributeName);
    } else {
      modelElement.removeAttributeNs(namespaceUri, attributeName);
    }
  }

  public void unlinkReference(
      final ModelElementInstance modelElement, final Object referenceIdentifier) {
    if (!incomingReferences.isEmpty()) {
      for (final Reference<?> incomingReference : incomingReferences) {
        ((ReferenceImpl<?>) incomingReference)
            .referencedElementRemoved(modelElement, referenceIdentifier);
      }
    }
  }

  /**
   * @return the incomingReferences
   */
  @Override
  public List<Reference<?>> getIncomingReferences() {
    return incomingReferences;
  }

  /**
   * @return the outgoingReferences
   */
  @Override
  public List<Reference<?>> getOutgoingReferences() {
    return outgoingReferences;
  }

  public void registerOutgoingReference(final Reference<?> ref) {
    outgoingReferences.add(ref);
  }

  public void registerIncoming(final Reference<?> ref) {
    incomingReferences.add(ref);
  }
}
