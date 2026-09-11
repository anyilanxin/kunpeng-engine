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
package com.anyilanxin.kunpeng.bpm.model.dmn;

import com.anyilanxin.kunpeng.bpm.model.dmn.instance.DmnElement;
import com.anyilanxin.kunpeng.bpm.model.dmn.instance.DmnModelElementInstance;
import com.anyilanxin.kunpeng.bpm.model.dmn.instance.NamedElement;
import com.anyilanxin.kunpeng.bpm.model.dmn.util.Java9CDataWhitespaceFilter;
import com.anyilanxin.kunpeng.bpm.model.dmn.util.ParseDmnModelRule;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.util.ReflectUtil;
import com.anyilanxin.kunpeng.bpm.model.xml.instance.ModelElementInstance;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.w3c.dom.Document;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.Diff;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public abstract class DmnModelTest {

  public final static String TEST_NAMESPACE = "https://anyilanxin.com/schema/1.0/dmn";

  @Rule
  public final ParseDmnModelRule parseDmnModelRule = new ParseDmnModelRule();

  @Rule
  public TemporaryFolder tmpFolder = new TemporaryFolder();

  protected DmnModelInstance modelInstance;

  @Before
  public void setup() {
    modelInstance = parseDmnModelRule.getDmnModel();
  }

  public <E extends NamedElement> E generateNamedElement(final Class<E> elementClass) {
    return generateNamedElement(elementClass, "");
  }

  public <E extends NamedElement> E generateNamedElement(final Class<E> elementClass, final Integer suffix) {
    return generateNamedElement(elementClass, "", suffix);
  }

  public <E extends NamedElement> E generateNamedElement(final Class<E> elementClass, final String name) {
    return generateNamedElement(elementClass, name, null);
  }

  public <E extends NamedElement> E generateNamedElement(final Class<E> elementClass, final String name, final Integer suffix) {
    final E element = generateElement(elementClass, suffix);
    element.setName(name);
    return element;
  }

  public <E extends DmnModelElementInstance> E generateElement(final Class<E> elementClass) {
    return generateElement(elementClass, null);
  }

  public <E extends DmnModelElementInstance> E generateElement(final Class<E> elementClass, final Integer suffix) {
    final E element = modelInstance.newInstance(elementClass);
    if (element instanceof DmnElement) {
      String identifier = elementClass.getSimpleName();
      if (suffix != null) {
        identifier += suffix.toString();
      }
      identifier = Character.toLowerCase(identifier.charAt(0)) + identifier.substring(1);
      ((DmnElement) element).setId(identifier);
    }
    return element;
  }

  protected void assertModelEqualsFile(final String expectedPath) throws Exception {
    final File actualFile = tmpFolder.newFile();
    Dmn.writeModelToFile(actualFile, modelInstance);

    final File expectedFile = ReflectUtil.getResourceAsFile(expectedPath);

    final DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
    final DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
    final Document actualDocument = docBuilder.parse(actualFile);
    final Document expectedDocument = docBuilder.parse(expectedFile);

    final Diff diff = DiffBuilder.compare(expectedDocument).withTest(actualDocument)
      .withNodeFilter(new Java9CDataWhitespaceFilter())
      .checkForSimilar()
      .build();

    if (diff.hasDifferences()) {

      final String failMsg = "XML differs:\n" + diff.getDifferences() +
        "\n\nActual XML:\n" + Dmn.convertToString(modelInstance);
      fail(failMsg);
    }
  }

  protected void assertElementIsEqualToId(final DmnModelElementInstance actualElement, final String id) {
    assertThat(actualElement).isNotNull();

    final ModelElementInstance expectedElement = modelInstance.getModelElementById(id);
    assertThat(expectedElement).isNotNull();

    assertThat(actualElement).isEqualTo(expectedElement);
  }

}
