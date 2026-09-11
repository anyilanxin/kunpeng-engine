/*
 * Copyright © 2017 camunda services GmbH (info@camunda.com)
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

package com.anyilanxin.kunpeng.bpm.model.bpmn.impl.instance;

import static com.anyilanxin.kunpeng.bpm.model.bpmn.impl.BpmnModelConstants.*;

import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.Documentation;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.Extension;
import com.anyilanxin.kunpeng.bpm.model.xml.ModelBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementTypeBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;
import com.anyilanxin.kunpeng.bpm.model.xml.type.attribute.Attribute;
import com.anyilanxin.kunpeng.bpm.model.xml.type.child.ChildElementCollection;
import com.anyilanxin.kunpeng.bpm.model.xml.type.child.SequenceBuilder;
import java.util.Collection;

/**
 * The BPMN extension element
 *
 * @author Sebastian Menski
 */
public class ExtensionImpl extends BpmnModelElementInstanceImpl implements Extension {

  protected static Attribute<String> definitionAttribute;
  protected static Attribute<Boolean> mustUnderstandAttribute;
  protected static ChildElementCollection<Documentation> documentationCollection;

  public ExtensionImpl(final ModelTypeInstanceContext instanceContext) {
    super(instanceContext);
  }

  public static void registerType(final ModelBuilder modelBuilder) {
    final ModelElementTypeBuilder typeBuilder =
        modelBuilder
            .defineType(Extension.class, BPMN_ELEMENT_EXTENSION)
            .namespaceUri(BPMN20_NS)
            .instanceProvider(
                new ModelTypeInstanceProvider<Extension>() {
                  @Override
                  public Extension newInstance(final ModelTypeInstanceContext instanceContext) {
                    return new ExtensionImpl(instanceContext);
                  }
                });

    // TODO: qname reference extension definition
    definitionAttribute = typeBuilder.stringAttribute(BPMN_ATTRIBUTE_DEFINITION).build();

    mustUnderstandAttribute =
        typeBuilder.booleanAttribute(BPMN_ATTRIBUTE_MUST_UNDERSTAND).defaultValue(false).build();

    final SequenceBuilder sequenceBuilder = typeBuilder.sequence();

    documentationCollection = sequenceBuilder.elementCollection(Documentation.class).build();

    typeBuilder.build();
  }

  @Override
  public String getDefinition() {
    return definitionAttribute.getValue(this);
  }

  @Override
  public void setDefinition(final String definition) {
    definitionAttribute.setValue(this, definition);
  }

  @Override
  public boolean mustUnderstand() {
    return mustUnderstandAttribute.getValue(this);
  }

  @Override
  public void setMustUnderstand(final boolean mustUnderstand) {
    mustUnderstandAttribute.setValue(this, mustUnderstand);
  }

  @Override
  public Collection<Documentation> getDocumentations() {
    return documentationCollection.get(this);
  }
}
