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

import com.anyilanxin.kunpeng.bpm.model.dmn.instance.Artifact;
import com.anyilanxin.kunpeng.bpm.model.dmn.instance.Text;
import com.anyilanxin.kunpeng.bpm.model.dmn.instance.TextAnnotation;
import com.anyilanxin.kunpeng.bpm.model.xml.ModelBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementTypeBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;
import com.anyilanxin.kunpeng.bpm.model.xml.type.attribute.Attribute;
import com.anyilanxin.kunpeng.bpm.model.xml.type.child.ChildElement;
import com.anyilanxin.kunpeng.bpm.model.xml.type.child.SequenceBuilder;

public class TextAnnotationImpl extends ArtifactImpl implements TextAnnotation {

  protected static Attribute<String> textFormatAttribute;

  protected static ChildElement<Text> textChild;

  @Override
  public String getTextFormat() {
    return textFormatAttribute.getValue(this);
  }

  @Override
  public void setTextFormat(final String textFormat) {
    textFormatAttribute.setValue(this, textFormat);
  }

  @Override
  public Text getText() {
    return textChild.getChild(this);
  }

  @Override
  public void setText(final Text text) {
    textChild.setChild(this, text);
  }

  public TextAnnotationImpl(final ModelTypeInstanceContext instanceContext) {
    super(instanceContext);
  }

  public static void registerType(final ModelBuilder modelBuilder) {
    final ModelElementTypeBuilder typeBuilder =
        modelBuilder
            .defineType(TextAnnotation.class, DMN_ELEMENT_TEXT_ANNOTATION)
            .namespaceUri(LATEST_DMN_NS)
            .extendsType(Artifact.class)
            .instanceProvider(
                new ModelTypeInstanceProvider<TextAnnotation>() {
                  @Override
                  public TextAnnotation newInstance(
                      final ModelTypeInstanceContext instanceContext) {
                    return new TextAnnotationImpl(instanceContext);
                  }
                });

    textFormatAttribute =
        typeBuilder.stringAttribute(DMN_ATTRIBUTE_TEXT_FORMAT).defaultValue("text/plain").build();

    final SequenceBuilder sequenceBuilder = typeBuilder.sequence();

    textChild = sequenceBuilder.element(Text.class).build();

    typeBuilder.build();
  }
}
