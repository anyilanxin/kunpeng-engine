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
package com.anyilanxin.kunpeng.bpm.model.dmn.traversal;

import com.anyilanxin.kunpeng.bpm.model.dmn.Dmn;
import com.anyilanxin.kunpeng.bpm.model.dmn.impl.DmnImpl;
import com.anyilanxin.kunpeng.bpm.model.dmn.instance.DmnModelElementInstance;
import com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementType;
import java.util.List;

/**
 * Maintains a registry of visitors per element type (e.g. one for FlowNode, one for ServiceTask,
 * etc.). When visiting an element, it calls the #
 *
 * <ul>
 *   <li>A visitor for a super type is visited before a sub type
 */
public abstract class TypeHierarchyVisitor implements ModelElementVisitor {

  @Override
  public void visit(final DmnModelElementInstance instance) {
    final ModelElementType type = instance.getElementType();
    final List<ModelElementType> typeHierarchy = getTypeHierarchy(type);

    for (final ModelElementType implementedType : typeHierarchy) {
      visit(implementedType, instance);
    }
  }

  protected abstract void visit(ModelElementType implementedType, DmnModelElementInstance instance);

  private List<ModelElementType> getTypeHierarchy(final ModelElementType type) {
    return ((DmnImpl) Dmn.INSTANCE).getHierarchy(type);
  }
}
