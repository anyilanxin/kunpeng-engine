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
package com.anyilanxin.kunpeng.bpm.model.dmn.util;

import com.anyilanxin.kunpeng.bpm.model.dmn.Dmn;
import com.anyilanxin.kunpeng.bpm.model.xml.Model;
import com.anyilanxin.kunpeng.bpm.model.xml.ModelInstance;
import com.anyilanxin.kunpeng.bpm.model.xml.instance.ModelElementInstance;
import com.anyilanxin.kunpeng.bpm.model.xml.test.GetModelElementTypeRule;
import com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementType;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import static org.assertj.core.api.Assertions.assertThat;

public class GetDmnModelElementTypeRule extends TestWatcher implements GetModelElementTypeRule {

  private ModelInstance modelInstance;
  private Model model;
  private ModelElementType modelElementType;

  @Override
  @SuppressWarnings("unchecked")
  protected void starting(final Description description) {
    String className = description.getClassName();
    assertThat(className).endsWith("Test");
    className = className.substring(0, className.length() - "Test".length());
    final Class<? extends ModelElementInstance> instanceClass;
    try {
      instanceClass = (Class<? extends ModelElementInstance>) Class.forName(className);
    } catch (final ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
    modelInstance = Dmn.createEmptyModel();
    model = modelInstance.getModel();
    modelElementType = model.getType(instanceClass);
  }

  @Override
  public ModelInstance getModelInstance() {
    return modelInstance;
  }

  @Override
  public Model getModel() {
    return model;
  }

  @Override
  public ModelElementType getModelElementType() {
    return modelElementType;
  }
}
