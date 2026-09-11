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

import com.anyilanxin.kunpeng.bpm.model.dmn.instance.Import;
import com.anyilanxin.kunpeng.bpm.model.dmn.instance.ImportedElement;
import com.anyilanxin.kunpeng.bpm.model.dmn.instance.ImportedValues;
import com.anyilanxin.kunpeng.bpm.model.xml.ModelBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementTypeBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;
import com.anyilanxin.kunpeng.bpm.model.xml.type.attribute.Attribute;
import com.anyilanxin.kunpeng.bpm.model.xml.type.child.ChildElement;
import com.anyilanxin.kunpeng.bpm.model.xml.type.child.SequenceBuilder;

public class ImportedValuesImpl extends ImportImpl implements ImportedValues {

  protected static Attribute<String> expressionLanguageAttribute;

  protected static ChildElement<ImportedElement> importedElementChild;

  public ImportedValuesImpl(final ModelTypeInstanceContext instanceContext) {
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
  public ImportedElement getImportedElement() {
    return importedElementChild.getChild(this);
  }

  @Override
  public void setImportedElement(final ImportedElement importedElement) {
    importedElementChild.setChild(this, importedElement);
  }

  public static void registerType(final ModelBuilder modelBuilder) {
    final ModelElementTypeBuilder typeBuilder =
        modelBuilder
            .defineType(ImportedValues.class, DMN_ELEMENT_IMPORTED_VALUES)
            .namespaceUri(LATEST_DMN_NS)
            .extendsType(Import.class)
            .instanceProvider(
                new ModelTypeInstanceProvider<ImportedValues>() {
                  @Override
                  public ImportedValues newInstance(
                      final ModelTypeInstanceContext instanceContext) {
                    return new ImportedValuesImpl(instanceContext);
                  }
                });

    expressionLanguageAttribute =
        typeBuilder.stringAttribute(DMN_ATTRIBUTE_EXPRESSION_LANGUAGE).build();

    final SequenceBuilder sequenceBuilder = typeBuilder.sequence();

    importedElementChild = sequenceBuilder.element(ImportedElement.class).required().build();

    typeBuilder.build();
  }
}
