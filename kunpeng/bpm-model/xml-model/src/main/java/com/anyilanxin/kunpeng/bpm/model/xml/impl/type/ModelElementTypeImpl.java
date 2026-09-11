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
package com.anyilanxin.kunpeng.bpm.model.xml.impl.type;

import com.anyilanxin.kunpeng.bpm.model.xml.Model;
import com.anyilanxin.kunpeng.bpm.model.xml.ModelException;
import com.anyilanxin.kunpeng.bpm.model.xml.ModelInstance;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.ModelImpl;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.ModelInstanceImpl;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.util.ModelTypeException;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.util.ModelUtil;
import com.anyilanxin.kunpeng.bpm.model.xml.instance.DomDocument;
import com.anyilanxin.kunpeng.bpm.model.xml.instance.DomElement;
import com.anyilanxin.kunpeng.bpm.model.xml.instance.ModelElementInstance;
import com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementType;
import com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;
import com.anyilanxin.kunpeng.bpm.model.xml.type.attribute.Attribute;
import com.anyilanxin.kunpeng.bpm.model.xml.type.child.ChildElementCollection;
import java.util.*;

/**
 * @author Daniel Meyer
 */
public class ModelElementTypeImpl implements ModelElementType {

  private final ModelImpl model;

  private final String typeName;

  private final Class<? extends ModelElementInstance> instanceType;

  private String typeNamespace;

  private ModelElementTypeImpl baseType;

  private final List<ModelElementType> extendingTypes = new ArrayList<ModelElementType>();

  private final List<Attribute<?>> attributes = new ArrayList<Attribute<?>>();

  private final List<ModelElementType> childElementTypes = new ArrayList<ModelElementType>();

  private final List<ChildElementCollection<?>> childElementCollections =
      new ArrayList<ChildElementCollection<?>>();

  private ModelTypeInstanceProvider<?> instanceProvider;

  private boolean isAbstract;

  public ModelElementTypeImpl(
      final ModelImpl model,
      final String name,
      final Class<? extends ModelElementInstance> instanceType) {
    this.model = model;
    typeName = name;
    this.instanceType = instanceType;
  }

  @Override
  public ModelElementInstance newInstance(final ModelInstance modelInstance) {
    final ModelInstanceImpl modelInstanceImpl = (ModelInstanceImpl) modelInstance;
    final DomDocument document = modelInstanceImpl.getDocument();
    final DomElement domElement = document.createElement(typeNamespace, typeName);
    return newInstance(modelInstanceImpl, domElement);
  }

  public ModelElementInstance newInstance(
      final ModelInstanceImpl modelInstance, final DomElement domElement) {
    final ModelTypeInstanceContext modelTypeInstanceContext =
        new ModelTypeInstanceContext(domElement, modelInstance, this);
    return createModelElementInstance(modelTypeInstanceContext);
  }

  public void registerAttribute(final Attribute<?> attribute) {
    if (!attributes.contains(attribute)) {
      attributes.add(attribute);
    }
  }

  public void registerChildElementType(final ModelElementType childElementType) {
    if (!childElementTypes.contains(childElementType)) {
      childElementTypes.add(childElementType);
    }
  }

  public void registerChildElementCollection(
      final ChildElementCollection<?> childElementCollection) {
    if (!childElementCollections.contains(childElementCollection)) {
      childElementCollections.add(childElementCollection);
    }
  }

  public void registerExtendingType(final ModelElementType modelType) {
    if (!extendingTypes.contains(modelType)) {
      extendingTypes.add(modelType);
    }
  }

  protected ModelElementInstance createModelElementInstance(
      final ModelTypeInstanceContext instanceContext) {
    if (isAbstract) {
      throw new ModelTypeException(
          "Model element type " + getTypeName() + " is abstract and no instances can be created.");
    } else {
      return instanceProvider.newInstance(instanceContext);
    }
  }

  @Override
  public final List<Attribute<?>> getAttributes() {
    return attributes;
  }

  @Override
  public String getTypeName() {
    return typeName;
  }

  @Override
  public Class<? extends ModelElementInstance> getInstanceType() {
    return instanceType;
  }

  public void setTypeNamespace(final String typeNamespace) {
    this.typeNamespace = typeNamespace;
  }

  @Override
  public String getTypeNamespace() {
    return typeNamespace;
  }

  public void setBaseType(final ModelElementTypeImpl baseType) {
    if (this.baseType == null) {
      this.baseType = baseType;
    } else if (!this.baseType.equals(baseType)) {
      throw new ModelException(
          "Type can not have multiple base types. "
              + getClass()
              + " already extends type "
              + this.baseType.getClass()
              + " and can not also extend type "
              + baseType.getClass());
    }
  }

  public void setInstanceProvider(final ModelTypeInstanceProvider<?> instanceProvider) {
    this.instanceProvider = instanceProvider;
  }

  @Override
  public boolean isAbstract() {
    return isAbstract;
  }

  public void setAbstract(final boolean isAbstract) {
    this.isAbstract = isAbstract;
  }

  @Override
  public Collection<ModelElementType> getExtendingTypes() {
    return Collections.unmodifiableCollection(extendingTypes);
  }

  @Override
  public Collection<ModelElementType> getAllExtendingTypes() {
    final HashSet<ModelElementType> extendingTypes = new HashSet<ModelElementType>();
    extendingTypes.add(this);
    resolveExtendingTypes(extendingTypes);
    return extendingTypes;
  }

  /**
   * Resolve all types recursively which are extending this type
   *
   * @param allExtendingTypes set of calculated extending types
   */
  public void resolveExtendingTypes(final Set<ModelElementType> allExtendingTypes) {
    for (final ModelElementType modelElementType : extendingTypes) {
      final ModelElementTypeImpl modelElementTypeImpl = (ModelElementTypeImpl) modelElementType;
      if (!allExtendingTypes.contains(modelElementTypeImpl)) {
        allExtendingTypes.add(modelElementType);
        modelElementTypeImpl.resolveExtendingTypes(allExtendingTypes);
      }
    }
  }

  /**
   * Resolve all types which are base types of this type
   *
   * @param baseTypes list of calculated base types
   */
  public void resolveBaseTypes(final List<ModelElementType> baseTypes) {
    if (baseType != null) {
      baseTypes.add(baseType);
      baseType.resolveBaseTypes(baseTypes);
    }
  }

  @Override
  public ModelElementType getBaseType() {
    return baseType;
  }

  @Override
  public Model getModel() {
    return model;
  }

  @Override
  public List<ModelElementType> getChildElementTypes() {
    return childElementTypes;
  }

  @Override
  public List<ModelElementType> getAllChildElementTypes() {
    final List<ModelElementType> allChildElementTypes = new ArrayList<ModelElementType>();
    if (baseType != null) {
      allChildElementTypes.addAll(baseType.getAllChildElementTypes());
    }
    allChildElementTypes.addAll(childElementTypes);
    return allChildElementTypes;
  }

  public List<ChildElementCollection<?>> getChildElementCollections() {
    return childElementCollections;
  }

  public List<ChildElementCollection<?>> getAllChildElementCollections() {
    final List<ChildElementCollection<?>> allChildElementCollections =
        new ArrayList<ChildElementCollection<?>>();
    if (baseType != null) {
      allChildElementCollections.addAll(baseType.getAllChildElementCollections());
    }
    allChildElementCollections.addAll(childElementCollections);
    return allChildElementCollections;
  }

  @Override
  public Collection<ModelElementInstance> getInstances(final ModelInstance modelInstance) {
    final ModelInstanceImpl modelInstanceImpl = (ModelInstanceImpl) modelInstance;
    final DomDocument document = modelInstanceImpl.getDocument();

    final List<DomElement> elements = getElementsByNameNs(document, typeNamespace);

    final List<ModelElementInstance> resultList = new ArrayList<ModelElementInstance>();
    for (final DomElement element : elements) {
      resultList.add(ModelUtil.getModelElement(element, modelInstanceImpl, this));
    }
    return resultList;
  }

  protected List<DomElement> getElementsByNameNs(
      final DomDocument document, final String namespaceURI) {
    List<DomElement> elements = document.getElementsByNameNs(namespaceURI, typeName);

    if (elements.isEmpty()) {
      final Set<String> alternativeNamespaces = getModel().getAlternativeNamespaces(namespaceURI);

      if (alternativeNamespaces != null) {
        final Iterator<String> namespaceIt = alternativeNamespaces.iterator();
        while (elements.isEmpty() && namespaceIt.hasNext()) {
          elements = getElementsByNameNs(document, namespaceIt.next());
        }
      }
    }

    return elements;
  }

  /**
   * Test if a element type is a base type of this type. So this type extends the given element
   * type.
   *
   * @param elementType the element type to test
   * @return true if {@code childElementTypeClass} is a base type of this type, else otherwise
   */
  public boolean isBaseTypeOf(final ModelElementType elementType) {
    if (equals(elementType)) {
      return true;
    } else {
      final Collection<ModelElementType> baseTypes = ModelUtil.calculateAllBaseTypes(elementType);
      return baseTypes.contains(this);
    }
  }

  /**
   * Returns a list of all attributes, including the attributes of all base types.
   *
   * @return the list of all attributes
   */
  public Collection<Attribute<?>> getAllAttributes() {
    final List<Attribute<?>> allAttributes = new ArrayList<Attribute<?>>();
    allAttributes.addAll(getAttributes());
    final Collection<ModelElementType> baseTypes = ModelUtil.calculateAllBaseTypes(this);
    for (final ModelElementType baseType : baseTypes) {
      allAttributes.addAll(baseType.getAttributes());
    }
    return allAttributes;
  }

  /**
   * Return the attribute for the attribute name
   *
   * @param attributeName the name of the attribute
   * @return the attribute or null if it not exists
   */
  @Override
  public Attribute<?> getAttribute(final String attributeName) {
    for (final Attribute<?> attribute : getAllAttributes()) {
      if (attribute.getAttributeName().equals(attributeName)) {
        return attribute;
      }
    }
    return null;
  }

  public ChildElementCollection<?> getChildElementCollection(
      final ModelElementType childElementType) {
    for (final ChildElementCollection<?> childElementCollection : getChildElementCollections()) {
      if (childElementType.equals(childElementCollection.getChildElementType(model))) {
        return childElementCollection;
      }
    }
    return null;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((model == null) ? 0 : model.hashCode());
    result = prime * result + ((typeName == null) ? 0 : typeName.hashCode());
    result = prime * result + ((typeNamespace == null) ? 0 : typeNamespace.hashCode());
    return result;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final ModelElementTypeImpl other = (ModelElementTypeImpl) obj;
    if (model == null) {
      if (other.model != null) {
        return false;
      }
    } else if (!model.equals(other.model)) {
      return false;
    }
    if (typeName == null) {
      if (other.typeName != null) {
        return false;
      }
    } else if (!typeName.equals(other.typeName)) {
      return false;
    }
    if (typeNamespace == null) {
      if (other.typeNamespace != null) {
        return false;
      }
    } else if (!typeNamespace.equals(other.typeNamespace)) {
      return false;
    }
    return true;
  }
}
