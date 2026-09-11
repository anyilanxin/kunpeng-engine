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

import static com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;

import com.anyilanxin.kunpeng.bpm.model.xml.Model;
import com.anyilanxin.kunpeng.bpm.model.xml.ModelBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.instance.ModelElementInstanceImpl;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.type.ModelElementTypeBuilderImpl;
import com.anyilanxin.kunpeng.bpm.model.xml.instance.ModelElementInstance;
import com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementType;
import com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementTypeBuilder;
import java.util.ArrayList;
import java.util.List;

/**
 * This builder is used to define and create a new model.
 *
 * @author Daniel Meyer
 */
public class ModelBuilderImpl extends ModelBuilder {

  private final List<ModelElementTypeBuilderImpl> typeBuilders =
      new ArrayList<ModelElementTypeBuilderImpl>();
  private final ModelImpl model;

  public ModelBuilderImpl(final String modelName) {
    model = new ModelImpl(modelName);
  }

  @Override
  public ModelBuilder alternativeNamespace(final String alternativeNs, final String actualNs) {
    model.declareAlternativeNamespace(alternativeNs, actualNs);
    return this;
  }

  @Override
  public ModelElementTypeBuilder defineType(
      final Class<? extends ModelElementInstance> modelInstanceType, final String typeName) {
    final ModelElementTypeBuilderImpl typeBuilder =
        new ModelElementTypeBuilderImpl(modelInstanceType, typeName, model);
    typeBuilders.add(typeBuilder);
    return typeBuilder;
  }

  @Override
  public ModelElementType defineGenericType(final String typeName, final String typeNamespaceUri) {
    final ModelElementTypeBuilder typeBuilder =
        defineType(ModelElementInstance.class, typeName)
            .namespaceUri(typeNamespaceUri)
            .instanceProvider(
                new ModelTypeInstanceProvider<ModelElementInstance>() {
                  @Override
                  public ModelElementInstance newInstance(
                      final ModelTypeInstanceContext instanceContext) {
                    return new ModelElementInstanceImpl(instanceContext);
                  }
                });

    return typeBuilder.build();
  }

  @Override
  public Model build() {
    for (final ModelElementTypeBuilderImpl typeBuilder : typeBuilders) {
      typeBuilder.buildTypeHierarchy(model);
    }
    for (final ModelElementTypeBuilderImpl typeBuilder : typeBuilders) {
      typeBuilder.performModelBuild(model);
    }
    return model;
  }
}
