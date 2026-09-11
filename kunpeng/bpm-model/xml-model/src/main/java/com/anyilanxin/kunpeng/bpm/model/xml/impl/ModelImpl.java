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
package com.anyilanxin.kunpeng.bpm.model.xml.impl;

import com.anyilanxin.kunpeng.bpm.model.xml.Model;
import com.anyilanxin.kunpeng.bpm.model.xml.ModelException;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.util.ModelUtil;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.util.QName;
import com.anyilanxin.kunpeng.bpm.model.xml.instance.ModelElementInstance;
import com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementType;
import java.util.*;

/**
 * A model contains all defined types and the relationship between them.
 *
 * @author Daniel Meyer
 */
public class ModelImpl implements Model {

  private final Map<QName, ModelElementType> typesByName = new HashMap<>();
  private final Map<Class<? extends ModelElementInstance>, ModelElementType> typesByClass =
      new HashMap<>();
  private final String modelName;

  protected final Map<String, Set<String>> actualNsToAlternative = new HashMap<>();
  protected final Map<String, String> alternativeNsToActual = new HashMap<String, String>();

  /**
   * Create a new {@link Model} with a model name.
   *
   * @param modelName the model name to identify the model
   */
  public ModelImpl(final String modelName) {
    this.modelName = modelName;
  }

  /**
   * Declares an alternative namespace for an actual so that during lookup of elements/attributes
   * both will be considered. This can be used if a newer namespaces replaces an older one but XML
   * files with the old one should still be parseable.
   *
   * @param alternativeNs
   * @param actualNs
   * @throws IllegalArgumentException if the alternative is already used or if the actual namespace
   *     has an alternative
   */
  public void declareAlternativeNamespace(final String alternativeNs, final String actualNs) {
    Set<String> alternativeNamespaces = actualNsToAlternative.get(actualNs);
    if (alternativeNamespaces == null) {
      // linked hash set for consistent iteration order
      alternativeNamespaces = new LinkedHashSet<String>();
      actualNsToAlternative.put(actualNs, alternativeNamespaces);
    }

    alternativeNamespaces.add(alternativeNs);
    alternativeNsToActual.put(alternativeNs, actualNs);
  }

  public void undeclareAlternativeNamespace(final String alternativeNs) {
    if (!alternativeNsToActual.containsKey(alternativeNs)) {
      return;
    }
    final String actual = alternativeNsToActual.remove(alternativeNs);
    actualNsToAlternative.remove(actual);
  }

  @Override
  public Set<String> getAlternativeNamespaces(final String actualNs) {
    return actualNsToAlternative.get(actualNs);
  }

  @Override
  public String getAlternativeNamespace(final String actualNs) {
    final Set<String> alternatives = getAlternativeNamespaces(actualNs);

    if (alternatives == null || alternatives.size() == 0) {
      return null;
    } else if (alternatives.size() == 1) {
      return alternatives.iterator().next();
    } else {
      throw new ModelException("There is more than one alternative namespace registered");
    }
  }

  @Override
  public String getActualNamespace(final String alternativeNs) {
    return alternativeNsToActual.get(alternativeNs);
  }

  @Override
  public Collection<ModelElementType> getTypes() {
    return new ArrayList<>(typesByName.values());
  }

  @Override
  public ModelElementType getType(final Class<? extends ModelElementInstance> instanceClass) {
    return typesByClass.get(instanceClass);
  }

  @Override
  public ModelElementType getTypeForName(final String typeName) {
    return getTypeForName(null, typeName);
  }

  @Override
  public ModelElementType getTypeForName(final String namespaceUri, final String typeName) {
    return typesByName.get(ModelUtil.getQName(namespaceUri, typeName));
  }

  /**
   * Registers a {@link ModelElementType} in this {@link Model}.
   *
   * @param modelElementType the element type to register
   * @param instanceType the instance class of the type to register
   */
  public void registerType(
      final ModelElementType modelElementType,
      final Class<? extends ModelElementInstance> instanceType) {
    final QName qName =
        ModelUtil.getQName(modelElementType.getTypeNamespace(), modelElementType.getTypeName());
    typesByName.put(qName, modelElementType);
    typesByClass.put(instanceType, modelElementType);
  }

  @Override
  public String getModelName() {
    return modelName;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((modelName == null) ? 0 : modelName.hashCode());
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
    final ModelImpl other = (ModelImpl) obj;
    if (modelName == null) {
      if (other.modelName != null) {
        return false;
      }
    } else if (!modelName.equals(other.modelName)) {
      return false;
    }
    return true;
  }
}
