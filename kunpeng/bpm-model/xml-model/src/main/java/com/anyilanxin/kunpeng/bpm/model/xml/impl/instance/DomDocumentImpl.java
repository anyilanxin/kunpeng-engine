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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.transform.dom.DOMSource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author Sebastian Menski
 */
public class DomDocumentImpl implements DomDocument {

  public static final String GENERIC_NS_PREFIX = "ns";

  private final Document document;

  /** 元素 id 索引兜底（id -> DOM 元素），非校验解析时 w3c DOM 不标记 ID 类型，按需自建 */
  private Map<String, Element> elementIdIndex;

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
      }
      // 非校验解析下 w3c DOM 的 getElementById 恒为空（id 属性未被标记为 ID 类型），退化为自建索引
      return findByIdIndex(id);
    }
  }

  /**
   * 自建 id 索引查找：首次访问（或索引未命中时）全量重建一次，以覆盖解析后新增/改 id 的元素； 命中直接返回。模型元素实例缓存在 DOM UserData 上，重建 wrapper
   * 不影响元素身份。
   */
  private DomElement findByIdIndex(final String id) {
    if (elementIdIndex == null || !elementIdIndex.containsKey(id)) {
      elementIdIndex = new HashMap<>();
      collectIds(document.getDocumentElement(), elementIdIndex);
    }
    final Element found = elementIdIndex.get(id);
    return found == null ? null : new DomElementImpl(found);
  }

  /** 递归收集元素的 id 属性（同名 id 保留先出现者）。 */
  private void collectIds(final Element element, final Map<String, Element> index) {
    if (element == null) {
      return;
    }
    final String id = element.getAttribute("id");
    if (id != null && !id.isEmpty()) {
      index.putIfAbsent(id, element);
    }
    final NodeList children = element.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      final Node child = children.item(i);
      if (child instanceof Element) {
        collectIds((Element) child, index);
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
