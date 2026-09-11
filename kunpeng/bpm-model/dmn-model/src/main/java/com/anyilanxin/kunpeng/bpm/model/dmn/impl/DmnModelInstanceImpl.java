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
package com.anyilanxin.kunpeng.bpm.model.dmn.impl;

import com.anyilanxin.kunpeng.bpm.model.dmn.DmnModelInstance;
import com.anyilanxin.kunpeng.bpm.model.dmn.instance.Definitions;
import com.anyilanxin.kunpeng.bpm.model.xml.ModelBuilder;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.ModelImpl;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.ModelInstanceImpl;
import com.anyilanxin.kunpeng.bpm.model.xml.instance.DomDocument;

public class DmnModelInstanceImpl extends ModelInstanceImpl implements DmnModelInstance {

  public DmnModelInstanceImpl(
      final ModelImpl model, final ModelBuilder modelBuilder, final DomDocument document) {
    super(model, modelBuilder, document);
  }

  @Override
  public Definitions getDefinitions() {
    return (Definitions) getDocumentElement();
  }

  @Override
  public void setDefinitions(final Definitions definitions) {
    setDocumentElement(definitions);
  }

  @Override
  public DmnModelInstance clone() {
    return new DmnModelInstanceImpl(model, modelBuilder, document.clone());
  }
}
