/*
 * Copyright © 2026 anyilanxin zxh(anyilanxin@aliyun.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.bpm.parse.bpmn.transformation;

import com.anyilanxin.kunpeng.bpm.model.bpmn.instance.BpmnModelElementInstance;
import com.anyilanxin.kunpeng.bpm.model.bpmn.traversal.TypeHierarchyVisitor;
import com.anyilanxin.kunpeng.bpm.model.xml.type.ModelElementType;
import java.util.HashMap;
import java.util.Map;

/**
 * 转换遍历访问者：对模型做深度优先遍历，元素类型已注册转换器时触发转换。
 *
 * <p>与 DMN 模块的同名访问者同构：每个转换阶段持有一个访问者实例，按需注册该阶段的元素转换器。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class ElementTransformVisitor extends TypeHierarchyVisitor {
  /** 已注册的元素转换器（以其处理的元素类型为键） */
  private final Map<Class<?>, ElementTransformer<?>> transformHandlers = new HashMap<>();

  /** 当前转换上下文 */
  private BpmnTransformContext context;

  /** 获取当前转换上下文。 */
  public BpmnTransformContext getContext() {
    return context;
  }

  /**
   * 设置当前转换上下文。
   *
   * @param context 转换上下文
   */
  public void setContext(final BpmnTransformContext context) {
    this.context = context;
  }

  /**
   * 注册模型元素转换器（以其处理的元素类型为键）。
   *
   * @param transformHandler 待注册的转换器
   */
  public void registerHandler(final ElementTransformer<?> transformHandler) {
    transformHandlers.put(transformHandler.getType(), transformHandler);
  }

  /**
   * 遍历回调：当前元素类型已注册转换器时触发转换。
   *
   * @param implementedType 当前元素的模型类型
   * @param instance 当前遍历到的模型元素实例
   */
  @SuppressWarnings({"rawtypes", "unchecked"})
  @Override
  protected void visit(
      final ModelElementType implementedType, final BpmnModelElementInstance instance) {
    final ElementTransformer handler = transformHandlers.get(implementedType.getInstanceType());
    if (handler != null) {
      handler.transform(instance, context);
    }
  }
}
