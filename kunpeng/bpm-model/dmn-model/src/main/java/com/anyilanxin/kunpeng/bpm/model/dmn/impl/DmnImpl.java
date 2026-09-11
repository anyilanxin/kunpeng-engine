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
package com.anyilanxin.kunpeng.bpm.model.dmn.impl;

import com.anyilanxin.kunpeng.bpm.model.dmn.Dmn;
import com.anyilanxin.kunpeng.bpm.model.dmn.DmnTypeHierarchy;
import com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementType;
import java.util.List;

public class DmnImpl extends Dmn {

  private final DmnTypeHierarchy typeHierarchy = new DmnTypeHierarchy();

  public DmnImpl() {
    super();
    getDmnModel().getTypes().forEach(typeHierarchy::registerType);
  }

  public List<ModelElementType> getHierarchy(final ModelElementType type) {
    return typeHierarchy.getHierarchy(type);
  }
}
