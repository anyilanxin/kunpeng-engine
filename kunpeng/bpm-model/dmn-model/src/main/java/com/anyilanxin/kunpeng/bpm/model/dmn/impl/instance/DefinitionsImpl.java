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
package com.anyilanxin.kunpeng.bpm.model.dmn.impl.instance;

import static com.anyilanxin.kunpeng.bpm.model.dmn.impl.DmnModelConstants.*;

import com.anyilanxin.kunpeng.bpm.model.dmn.instance.*;
import com.anyilanxin.kunpeng.bpm.model.xml.ModelBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementTypeBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.type.attribute.Attribute;
import com.anyilanxin.kunpeng.bpm.model.xml.type.child.ChildElementCollection;
import com.anyilanxin.kunpeng.bpm.model.xml.type.child.SequenceBuilder;
import java.util.Collection;

public class DefinitionsImpl extends NamedElementImpl implements Definitions {

  protected static Attribute<String> expressionLanguageAttribute;
  protected static Attribute<String> typeLanguageAttribute;
  protected static Attribute<String> namespaceAttribute;
  protected static Attribute<String> exporterAttribute;
  protected static Attribute<String> exporterVersionAttribute;

  protected static ChildElementCollection<Import> importCollection;
  protected static ChildElementCollection<ItemDefinition> itemDefinitionCollection;
  protected static ChildElementCollection<DrgElement> drgElementCollection;
  protected static ChildElementCollection<Artifact> artifactCollection;
  protected static ChildElementCollection<ElementCollection> elementCollectionCollection;
  protected static ChildElementCollection<BusinessContextElement> businessContextElementCollection;

  public DefinitionsImpl(final ModelTypeInstanceContext instanceContext) {
    super(instanceContext);
  }

  @Override
  public String getExpressionLanguage() {
    return expressionLanguageAttribute.getValue(this);
  }

  @Override
  public void setExpressionLanguage(final String expressionLanguage) {
    expressionLanguageAttribute.setValue(this, expressionLanguage);
  }

  @Override
  public String getTypeLanguage() {
    return typeLanguageAttribute.getValue(this);
  }

  @Override
  public void setTypeLanguage(final String typeLanguage) {
    typeLanguageAttribute.setValue(this, typeLanguage);
  }

  @Override
  public String getNamespace() {
    return namespaceAttribute.getValue(this);
  }

  @Override
  public void setNamespace(final String namespace) {
    namespaceAttribute.setValue(this, namespace);
  }

  @Override
  public String getExporter() {
    return exporterAttribute.getValue(this);
  }

  @Override
  public void setExporter(final String exporter) {
    exporterAttribute.setValue(this, exporter);
  }

  @Override
  public String getExporterVersion() {
    return exporterVersionAttribute.getValue(this);
  }

  @Override
  public void setExporterVersion(final String exporterVersion) {
    exporterVersionAttribute.setValue(this, exporterVersion);
  }

  @Override
  public Collection<Import> getImports() {
    return importCollection.get(this);
  }

  @Override
  public Collection<ItemDefinition> getItemDefinitions() {
    return itemDefinitionCollection.get(this);
  }

  @Override
  public Collection<DrgElement> getDrgElements() {
    return drgElementCollection.get(this);
  }

  @Override
  public Collection<Artifact> getArtifacts() {
    return artifactCollection.get(this);
  }

  @Override
  public Collection<ElementCollection> getElementCollections() {
    return elementCollectionCollection.get(this);
  }

  @Override
  public Collection<BusinessContextElement> getBusinessContextElements() {
    return businessContextElementCollection.get(this);
  }

  public static void registerType(final ModelBuilder modelBuilder) {
    final ModelElementTypeBuilder typeBuilder =
        modelBuilder
            .defineType(Definitions.class, DMN_ELEMENT_DEFINITIONS)
            .namespaceUri(LATEST_DMN_NS)
            .extendsType(NamedElement.class)
            .instanceProvider(
                new ModelElementTypeBuilder.ModelTypeInstanceProvider<Definitions>() {
                  @Override
                  public Definitions newInstance(final ModelTypeInstanceContext instanceContext) {
                    return new DefinitionsImpl(instanceContext);
                  }
                });

    expressionLanguageAttribute =
        typeBuilder
            .stringAttribute(DMN_ATTRIBUTE_EXPRESSION_LANGUAGE)
            .defaultValue("http://www.omg.org/spec/FEEL/20140401")
            .build();

    typeLanguageAttribute =
        typeBuilder
            .stringAttribute(DMN_ATTRIBUTE_TYPE_LANGUAGE)
            .defaultValue("http://www.omg.org/spec/FEEL/20140401")
            .build();

    namespaceAttribute = typeBuilder.stringAttribute(DMN_ATTRIBUTE_NAMESPACE).required().build();

    exporterAttribute = typeBuilder.stringAttribute(DMN_ATTRIBUTE_EXPORTER).build();

    exporterVersionAttribute = typeBuilder.stringAttribute(DMN_ATTRIBUTE_EXPORTER_VERSION).build();

    final SequenceBuilder sequenceBuilder = typeBuilder.sequence();

    importCollection = sequenceBuilder.elementCollection(Import.class).build();

    itemDefinitionCollection = sequenceBuilder.elementCollection(ItemDefinition.class).build();

    drgElementCollection = sequenceBuilder.elementCollection(DrgElement.class).build();

    artifactCollection = sequenceBuilder.elementCollection(Artifact.class).build();

    elementCollectionCollection =
        sequenceBuilder.elementCollection(ElementCollection.class).build();

    businessContextElementCollection =
        sequenceBuilder.elementCollection(BusinessContextElement.class).build();

    typeBuilder.build();
  }
}
