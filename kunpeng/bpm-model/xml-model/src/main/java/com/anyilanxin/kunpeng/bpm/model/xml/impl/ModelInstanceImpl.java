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
import com.anyilanxin.kunpeng.bpm.model.xml.ModelBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.ModelException;
import com.anyilanxin.kunpeng.bpm.model.xml.ModelInstance;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.instance.ModelElementInstanceImpl;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.util.ModelUtil;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.validation.ModelInstanceValidator;
import com.anyilanxin.kunpeng.bpm.model.xml.instance.DomDocument;
import com.anyilanxin.kunpeng.bpm.model.xml.instance.DomElement;
import com.anyilanxin.kunpeng.bpm.model.xml.instance.ModelElementInstance;
import com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementType;
import com.anyilanxin.kunpeng.bpm.model.xml.validation.ModelElementValidator;
import com.anyilanxin.kunpeng.bpm.model.xml.validation.ValidationResults;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * An instance of a model
 *
 * @author Daniel Meyer
 * @author Sebastian Menski
 */
public class ModelInstanceImpl implements ModelInstance {

  protected final DomDocument document;
  protected ModelImpl model;
  protected final ModelBuilder modelBuilder;

  public ModelInstanceImpl(
      final ModelImpl model, final ModelBuilder modelBuilder, final DomDocument document) {
    this.model = model;
    this.modelBuilder = modelBuilder;
    this.document = document;
  }

  @Override
  public DomDocument getDocument() {
    return document;
  }

  @Override
  public ModelElementInstance getDocumentElement() {
    final DomElement rootElement = document.getRootElement();
    if (rootElement != null) {
      return ModelUtil.getModelElement(rootElement, this);
    } else {
      return null;
    }
  }

  @Override
  public void setDocumentElement(final ModelElementInstance modelElement) {
    ModelUtil.ensureInstanceOf(modelElement, ModelElementInstanceImpl.class);
    final DomElement domElement = modelElement.getDomElement();
    document.setRootElement(domElement);
  }

  @Override
  public <T extends ModelElementInstance> T newInstance(final Class<T> type) {
    return newInstance(type, null);
  }

  @Override
  public <T extends ModelElementInstance> T newInstance(final Class<T> type, final String id) {
    final ModelElementType modelElementType = model.getType(type);
    if (modelElementType != null) {
      return newInstance(modelElementType, id);
    } else {
      throw new ModelException(
          "Cannot create instance of ModelType " + type + ": no such type registered.");
    }
  }

  @Override
  public <T extends ModelElementInstance> T newInstance(final ModelElementType type) {
    return newInstance(type, null);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T extends ModelElementInstance> T newInstance(
      final ModelElementType type, final String id) {
    final ModelElementInstance modelElementInstance = type.newInstance(this);
    if (id != null && !id.isEmpty()) {
      ModelUtil.setNewIdentifier(type, modelElementInstance, id, false);
    } else {
      ModelUtil.setGeneratedUniqueIdentifier(type, modelElementInstance, false);
    }
    return (T) modelElementInstance;
  }

  @Override
  public Model getModel() {
    return model;
  }

  public ModelElementType registerGenericType(final String namespaceUri, final String localName) {
    ModelElementType elementType = model.getTypeForName(namespaceUri, localName);
    if (elementType == null) {
      elementType = modelBuilder.defineGenericType(localName, namespaceUri);
      model = (ModelImpl) modelBuilder.build();
    }
    return elementType;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T extends ModelElementInstance> T getModelElementById(final String id) {
    if (id == null) {
      return null;
    }

    final DomElement element = document.getElementById(id);
    if (element != null) {
      return (T) ModelUtil.getModelElement(element, this);
    } else {
      return null;
    }
  }

  @Override
  public Collection<ModelElementInstance> getModelElementsByType(final ModelElementType type) {
    final Collection<ModelElementType> extendingTypes = type.getAllExtendingTypes();

    final List<ModelElementInstance> instances = new ArrayList<ModelElementInstance>();
    for (final ModelElementType modelElementType : extendingTypes) {
      if (!modelElementType.isAbstract()) {
        instances.addAll(modelElementType.getInstances(this));
      }
    }
    return instances;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T extends ModelElementInstance> Collection<T> getModelElementsByType(
      final Class<T> referencingClass) {
    return (Collection<T>) getModelElementsByType(getModel().getType(referencingClass));
  }

  @Override
  public ModelInstance clone() {
    return new ModelInstanceImpl(model, modelBuilder, document.clone());
  }

  @Override
  public ValidationResults validate(final Collection<ModelElementValidator<?>> validators) {
    return new ModelInstanceValidator(this, validators).validate();
  }
}
