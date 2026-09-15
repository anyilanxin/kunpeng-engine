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
package com.anyilanxin.kunpeng.bpm.model.bpmn.traversal;

import com.anyilanxin.kunpeng.bpm.model.bpmn.BpmnModelInstance;
import com.anyilanxin.kunpeng.bpm.model.bpmn.impl.BpmnModelInstanceImpl;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.BpmnModelElementInstance;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.Definitions;
import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.Process;
import com.anyilanxin.kunpeng.bpm.model.xml.impl.util.ModelUtil;
import com.anyilanxin.kunpeng.bpm.model.xml.instance.DomElement;
import com.anyilanxin.kunpeng.bpm.model.xml.instance.ModelElementInstance;
import com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Walks the elements of a {@link BpmnModelInstance} and invokes the provided {@link
 * ModelElementVisitor} for every element.
 *
 * <p>The following order is guaranteed (anything that is not listed here is not guaranteed):
 *
 * <ul>
 *   <li>An element is visited only after its parent has been visited (top-down)
 *   <li>An element's child is visisted before any not yet visited sibling (depth-first)
 * </ul>
 *
 * <p>We can add more constraints to this as we see fit (e.g. certain BPMN elements in the same
 * scope can be visited in a defined order to make transformation more convenient)
 *
 * <p>Depth-first is nice for transformation so we can have some kind of stack with transformation
 * state.
 */
public class ModelWalker {

  private static final Logger LOG = LoggerFactory.getLogger(ModelWalker.class);

  private final BpmnModelInstanceImpl modelInstance;
  private final Deque<BpmnModelElementInstance> elementsToVisit = new LinkedList<>();

  /** 子元素列表缓存（DOM 元素 -> 已过滤的 BPMN 子元素），同一 walker 的多次遍历间复用 */
  private final Map<DomElement, List<BpmnModelElementInstance>> childrenCache = new HashMap<>();

  public ModelWalker(final BpmnModelInstance modelInstance) {
    this.modelInstance = (BpmnModelInstanceImpl) modelInstance;
  }

  public void walk(final ModelElementVisitor visitor) {
    final Definitions rootElement = modelInstance.getDefinitions();

    elementsToVisit.add(rootElement); // top-down

    BpmnModelElementInstance currentElement;
    while ((currentElement = elementsToVisit.poll()) != null) {

      // add a new check here for ignore non-executable processes
      if (isNonExecutableProcess(currentElement)) {
        continue;
      }

      visitor.visit(currentElement);
      // depth-first
      for (final BpmnModelElementInstance child : childrenOf(currentElement)) {
        elementsToVisit.addFirst(child);
      }
    }
  }

  /** 获取元素的 BPMN 子元素列表（非 BPMN 命名空间的子元素忽略并记日志）；结果在本 walker 实例内缓存， 多阶段转换的重复遍历不再重复物化集合与包装查找。 */
  private List<BpmnModelElementInstance> childrenOf(final BpmnModelElementInstance element) {
    final DomElement domElement = element.getDomElement();
    final List<BpmnModelElementInstance> cached = childrenCache.get(domElement);
    if (cached != null) {
      return cached;
    }
    final Collection<ModelElementInstance> children =
        ModelUtil.getModelElementCollection(domElement.getChildElements(), modelInstance);
    final List<BpmnModelElementInstance> filtered = new ArrayList<>(children.size());
    for (final ModelElementInstance child : children) {
      if (child instanceof BpmnModelElementInstance) {
        filtered.add((BpmnModelElementInstance) child);
      } else {
        final ModelElementType elementType = child.getElementType();
        LOG.debug(
            "Ignoring unknown BPMN element '{}:{}'",
            elementType.getTypeNamespace(),
            elementType.getTypeName());
      }
    }
    childrenCache.put(domElement, filtered);
    return filtered;
  }

  private boolean isNonExecutableProcess(final BpmnModelElementInstance element) {
    if (element instanceof Process) {
      final Process process = (Process) element;
      return !process.isExecutable();
    } else {
      return false;
    }
  }
}
