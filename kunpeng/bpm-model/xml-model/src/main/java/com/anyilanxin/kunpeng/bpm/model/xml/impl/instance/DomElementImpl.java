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

import static javax.xml.XMLConstants.XMLNS_ATTRIBUTE;
import static javax.xml.XMLConstants.XMLNS_ATTRIBUTE_NS_URI;

import com.anyilanxin.kunpeng.bpm.model.xml.ModelException;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.ModelInstanceImpl;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.util.DomUtil;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.util.XmlQName;
import com.anyilanxin.kunpeng.bpm.model.xml.instance.DomDocument;
import com.anyilanxin.kunpeng.bpm.model.xml.instance.DomElement;
import com.anyilanxin.kunpeng.bpm.model.xml.instance.ModelElementInstance;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.w3c.dom.*;

/**
 * @author Sebastian Menski
 */
public class DomElementImpl implements DomElement {

  private static final String MODEL_ELEMENT_KEY = "camunda.modelElementRef";

  private final Element element;
  private final Document document;

  public DomElementImpl(final Element element) {
    this.element = element;
    document = element.getOwnerDocument();
  }

  protected Element getElement() {
    return element;
  }

  @Override
  public String getNamespaceURI() {
    synchronized (document) {
      return element.getNamespaceURI();
    }
  }

  @Override
  public String getLocalName() {
    synchronized (document) {
      return element.getLocalName();
    }
  }

  @Override
  public String getPrefix() {
    synchronized (document) {
      return element.getPrefix();
    }
  }

  @Override
  public DomDocument getDocument() {
    synchronized (document) {
      final Document ownerDocument = element.getOwnerDocument();
      if (ownerDocument != null) {
        return new DomDocumentImpl(ownerDocument);
      } else {
        return null;
      }
    }
  }

  @Override
  public DomElement getRootElement() {
    synchronized (document) {
      final DomDocument document = getDocument();
      if (document != null) {
        return document.getRootElement();
      } else {
        return null;
      }
    }
  }

  @Override
  public DomElement getParentElement() {
    synchronized (document) {
      final Node parentNode = element.getParentNode();
      if (parentNode != null && parentNode instanceof Element) {
        return new DomElementImpl((Element) parentNode);
      } else {
        return null;
      }
    }
  }

  @Override
  public List<DomElement> getChildElements() {
    synchronized (document) {
      final NodeList childNodes = element.getChildNodes();
      return DomUtil.filterNodeListForElements(childNodes);
    }
  }

  @Override
  public List<DomElement> getChildElementsByNameNs(
      final String namespaceUri, final String elementName) {
    synchronized (document) {
      final NodeList childNodes = element.getChildNodes();
      return DomUtil.filterNodeListByName(childNodes, namespaceUri, elementName);
    }
  }

  @Override
  public List<DomElement> getChildElementsByNameNs(
      final Set<String> namespaceUris, final String elementName) {
    final List<DomElement> result = new ArrayList<DomElement>();
    for (final String namespace : namespaceUris) {
      if (namespace != null) {
        result.addAll(getChildElementsByNameNs(namespace, elementName));
      }
    }
    return result;
  }

  @Override
  public List<DomElement> getChildElementsByType(
      final ModelInstanceImpl modelInstance,
      final Class<? extends ModelElementInstance> elementType) {
    synchronized (document) {
      final NodeList childNodes = element.getChildNodes();
      return DomUtil.filterNodeListByType(childNodes, modelInstance, elementType);
    }
  }

  @Override
  public void replaceChild(
      final DomElement newChildDomElement, final DomElement existingChildDomElement) {
    synchronized (document) {
      final Element newElement = ((DomElementImpl) newChildDomElement).getElement();
      final Element existingElement = ((DomElementImpl) existingChildDomElement).getElement();
      try {
        element.replaceChild(newElement, existingElement);
      } catch (final DOMException e) {
        throw new ModelException(
            "Unable to replace child <"
                + existingElement
                + "> of element <"
                + element
                + "> with element <"
                + newElement
                + ">",
            e);
      }
    }
  }

  @Override
  public boolean removeChild(final DomElement childDomElement) {
    synchronized (document) {
      final Element childElement = ((DomElementImpl) childDomElement).getElement();
      try {
        element.removeChild(childElement);
        return true;
      } catch (final DOMException e) {
        return false;
      }
    }
  }

  @Override
  public void appendChild(final DomElement childDomElement) {
    synchronized (document) {
      final Element childElement = ((DomElementImpl) childDomElement).getElement();
      element.appendChild(childElement);
    }
  }

  @Override
  public void insertChildElementAfter(
      final DomElement elementToInsert, final DomElement insertAfter) {
    synchronized (document) {
      final Element newElement = ((DomElementImpl) elementToInsert).getElement();
      // find node to insert before
      final Node insertBeforeNode;
      if (insertAfter == null) {
        insertBeforeNode = element.getFirstChild();
      } else {
        insertBeforeNode = ((DomElementImpl) insertAfter).getElement().getNextSibling();
      }

      // insert before node or append if no node was found
      if (insertBeforeNode != null) {
        element.insertBefore(newElement, insertBeforeNode);
      } else {
        element.appendChild(newElement);
      }
    }
  }

  @Override
  public boolean hasAttribute(final String localName) {
    return hasAttribute(null, localName);
  }

  @Override
  public boolean hasAttribute(final String namespaceUri, final String localName) {
    synchronized (document) {
      return element.hasAttributeNS(namespaceUri, localName);
    }
  }

  @Override
  public String getAttribute(final String attributeName) {
    return getAttribute(null, attributeName);
  }

  @Override
  public String getAttribute(final String namespaceUri, final String localName) {
    synchronized (document) {
      final XmlQName xmlQName = new XmlQName(this, namespaceUri, localName);
      final String value;
      if (xmlQName.hasLocalNamespace()) {
        value = element.getAttributeNS(null, xmlQName.getLocalName());
      } else {
        value = element.getAttributeNS(xmlQName.getNamespaceUri(), xmlQName.getLocalName());
      }
      if (value.isEmpty()) {
        return null;
      } else {
        return value;
      }
    }
  }

  @Override
  public void setAttribute(final String localName, final String value) {
    setAttribute(null, localName, value);
  }

  @Override
  public void setAttribute(final String namespaceUri, final String localName, final String value) {
    setAttribute(namespaceUri, localName, value, false);
  }

  private void setAttribute(
      final String namespaceUri,
      final String localName,
      final String value,
      final boolean isIdAttribute) {
    synchronized (document) {
      final XmlQName xmlQName = new XmlQName(this, namespaceUri, localName);
      if (xmlQName.hasLocalNamespace()) {
        element.setAttributeNS(null, xmlQName.getLocalName(), value);
        if (isIdAttribute) {
          element.setIdAttributeNS(null, xmlQName.getLocalName(), true);
        }
      } else {
        element.setAttributeNS(xmlQName.getNamespaceUri(), xmlQName.getPrefixedName(), value);
        if (isIdAttribute) {
          element.setIdAttributeNS(xmlQName.getNamespaceUri(), xmlQName.getLocalName(), true);
        }
      }
    }
  }

  @Override
  public void setIdAttribute(final String localName, final String value) {
    setIdAttribute(getNamespaceURI(), localName, value);
  }

  @Override
  public void setIdAttribute(
      final String namespaceUri, final String localName, final String value) {
    setAttribute(namespaceUri, localName, value, true);
  }

  @Override
  public void removeAttribute(final String localName) {
    removeAttribute(getNamespaceURI(), localName);
  }

  @Override
  public void removeAttribute(final String namespaceUri, final String localName) {
    synchronized (document) {
      final XmlQName xmlQName = new XmlQName(this, namespaceUri, localName);
      if (xmlQName.hasLocalNamespace()) {
        element.removeAttributeNS(null, xmlQName.getLocalName());
      } else {
        element.removeAttributeNS(xmlQName.getNamespaceUri(), xmlQName.getLocalName());
      }
    }
  }

  @Override
  public String getTextContent() {
    synchronized (document) {
      return element.getTextContent();
    }
  }

  @Override
  public void setTextContent(final String textContent) {
    synchronized (document) {
      element.setTextContent(textContent);
    }
  }

  @Override
  public void addCDataSection(final String data) {
    synchronized (document) {
      final CDATASection cdataSection = document.createCDATASection(data);
      element.appendChild(cdataSection);
    }
  }

  @Override
  public ModelElementInstance getModelElementInstance() {
    synchronized (document) {
      return (ModelElementInstance) element.getUserData(MODEL_ELEMENT_KEY);
    }
  }

  @Override
  public void setModelElementInstance(final ModelElementInstance modelElementInstance) {
    synchronized (document) {
      element.setUserData(MODEL_ELEMENT_KEY, modelElementInstance, null);
    }
  }

  @Override
  public String registerNamespace(final String namespaceUri) {
    synchronized (document) {
      final String lookupPrefix = lookupPrefix(namespaceUri);
      if (lookupPrefix == null) {
        // check if a prefix is known
        String prefix = XmlQName.KNOWN_PREFIXES.get(namespaceUri);
        // check if prefix is not already used
        if (prefix != null
            && getRootElement() != null
            && getRootElement().hasAttribute(XMLNS_ATTRIBUTE_NS_URI, prefix)) {
          prefix = null;
        }
        if (prefix == null) {
          // generate prefix
          prefix = ((DomDocumentImpl) getDocument()).getUnusedGenericNsPrefix();
        }
        registerNamespace(prefix, namespaceUri);
        return prefix;
      } else {
        return lookupPrefix;
      }
    }
  }

  @Override
  public void registerNamespace(final String prefix, final String namespaceUri) {
    synchronized (document) {
      element.setAttributeNS(XMLNS_ATTRIBUTE_NS_URI, XMLNS_ATTRIBUTE + ":" + prefix, namespaceUri);
    }
  }

  @Override
  public String lookupPrefix(final String namespaceUri) {
    synchronized (document) {
      return element.lookupPrefix(namespaceUri);
    }
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    final DomElementImpl that = (DomElementImpl) o;
    return element.equals(that.element);
  }

  @Override
  public int hashCode() {
    return element.hashCode();
  }
}
