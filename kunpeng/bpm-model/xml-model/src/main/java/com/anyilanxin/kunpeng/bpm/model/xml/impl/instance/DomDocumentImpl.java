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

import static javax.xml.XMLConstants.XMLNS_ATTRIBUTE_NS_URI;

import com.anyilanxin.kunpeng.bpm.model.xml.ModelException;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.util.DomUtil;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.util.XmlQName;
import com.anyilanxin.kunpeng.bpm.model.xml.instance.DomDocument;
import com.anyilanxin.kunpeng.bpm.model.xml.instance.DomElement;
import java.util.List;
import javax.xml.transform.dom.DOMSource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * @author Sebastian Menski
 */
public class DomDocumentImpl implements DomDocument {

  public static final String GENERIC_NS_PREFIX = "ns";

  private final Document document;

  public DomDocumentImpl(final Document document) {
    this.document = document;
  }

  @Override
  public DomElement getRootElement() {
    synchronized (document) {
      final Element documentElement = document.getDocumentElement();
      if (documentElement != null) {
        return new DomElementImpl(documentElement);
      } else {
        return null;
      }
    }
  }

  @Override
  public void setRootElement(final DomElement rootElement) {
    synchronized (document) {
      final Element documentElement = document.getDocumentElement();
      final Element newDocumentElement = ((DomElementImpl) rootElement).getElement();
      if (documentElement != null) {
        document.replaceChild(newDocumentElement, documentElement);
      } else {
        document.appendChild(newDocumentElement);
      }
    }
  }

  @Override
  public DomElement createElement(final String namespaceUri, final String localName) {
    synchronized (document) {
      final XmlQName xmlQName = new XmlQName(this, namespaceUri, localName);
      final Element element =
          document.createElementNS(xmlQName.getNamespaceUri(), xmlQName.getPrefixedName());
      return new DomElementImpl(element);
    }
  }

  @Override
  public DomElement getElementById(final String id) {
    synchronized (document) {
      final Element element = document.getElementById(id);
      if (element != null) {
        return new DomElementImpl(element);
      } else {
        return null;
      }
    }
  }

  @Override
  public List<DomElement> getElementsByNameNs(final String namespaceUri, final String localName) {
    synchronized (document) {
      final NodeList elementsByTagNameNS = document.getElementsByTagNameNS(namespaceUri, localName);
      return DomUtil.filterNodeListByName(elementsByTagNameNS, namespaceUri, localName);
    }
  }

  @Override
  public DOMSource getDomSource() {
    return new DOMSource(document);
  }

  @Override
  public String registerNamespace(final String namespaceUri) {
    synchronized (document) {
      final DomElement rootElement = getRootElement();
      if (rootElement != null) {
        return rootElement.registerNamespace(namespaceUri);
      } else {
        throw new ModelException(
            "Unable to define a new namespace without a root document element");
      }
    }
  }

  @Override
  public void registerNamespace(final String prefix, final String namespaceUri) {
    synchronized (document) {
      final DomElement rootElement = getRootElement();
      if (rootElement != null) {
        rootElement.registerNamespace(prefix, namespaceUri);
      } else {
        throw new ModelException(
            "Unable to define a new namespace without a root document element");
      }
    }
  }

  protected String getUnusedGenericNsPrefix() {
    synchronized (document) {
      final Element documentElement = document.getDocumentElement();
      if (documentElement == null) {
        return GENERIC_NS_PREFIX + "0";
      } else {
        for (int i = 0; i < Integer.MAX_VALUE; i++) {
          if (!documentElement.hasAttributeNS(XMLNS_ATTRIBUTE_NS_URI, GENERIC_NS_PREFIX + i)) {
            return GENERIC_NS_PREFIX + i;
          }
        }
        throw new ModelException("Unable to find an unused namespace prefix");
      }
    }
  }

  @Override
  public DomDocument clone() {
    synchronized (document) {
      return new DomDocumentImpl((Document) document.cloneNode(true));
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

    final DomDocumentImpl that = (DomDocumentImpl) o;
    return document.equals(that.document);
  }

  @Override
  public int hashCode() {
    return document.hashCode();
  }
}
