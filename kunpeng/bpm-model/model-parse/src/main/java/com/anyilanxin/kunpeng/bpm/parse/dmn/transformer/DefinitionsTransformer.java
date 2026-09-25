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
package com.anyilanxin.kunpeng.bpm.parse.dmn.transformer;

import com.anyilanxin.kunpeng.bpm.model.dmn.instance.Definitions;
import com.anyilanxin.kunpeng.bpm.parse.dmn.element.DmnDecisionRequirementsGraphImpl;
import com.anyilanxin.kunpeng.bpm.parse.dmn.transformation.ModelElementTransformer;
import com.anyilanxin.kunpeng.bpm.parse.dmn.transformation.TransformContext;

/**
 * 将 DMN Definitions（根定义）转换为运行时决策需求图 DmnDecisionRequirementsGraph，并记录全局表达式语言。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class DefinitionsTransformer implements ModelElementTransformer<Definitions> {
  /** 返回本转换器处理的 DMN 模型元素类型。 */
  @Override
  public Class<Definitions> getType() {
    return Definitions.class;
  }

  /**
   * 以 Definitions 的 id 与名称初始化决策需求图放入上下文，并记录全局表达式语言。
   *
   * @param element 待转换的 Definitions 模型元素
   * @param context 转换上下文
   */
  @Override
  public void transform(final Definitions element, final TransformContext context) {
    final DmnDecisionRequirementsGraphImpl graph = new DmnDecisionRequirementsGraphImpl();
    graph.setKey(element.getId());
    graph.setName(element.getName());
    context.setRequirementsGraph(graph);
    context.setLanguage(element.getExpressionLanguage());
  }
}
