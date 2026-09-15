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

import static com.anyilanxin.kunpeng.bpm.parse.dmn.type.TypeHelper.createTypeDefinition;

import com.anyilanxin.kunpeng.bpm.model.dmn.instance.Variable;
import com.anyilanxin.kunpeng.bpm.parse.dmn.element.common.DmnVariableImpl;
import com.anyilanxin.kunpeng.bpm.parse.dmn.transformation.ModelElementTransformer;
import com.anyilanxin.kunpeng.bpm.parse.dmn.transformation.TransformContext;
import com.anyilanxin.kunpeng.bpm.parse.dmn.type.DmnTypeDefinition;

/** 将 DMN Variable（信息项变量）转换为运行时 DmnVariable 元素：设置名称并按 typeRef 解析类型定义。 */
public final class VariableTransformer implements ModelElementTransformer<Variable> {
  /** 返回本转换器处理的 DMN 模型元素类型。 */
  @Override
  public Class<Variable> getType() {
    return Variable.class;
  }

  /**
   * 转换信息项变量并注册到上下文。
   *
   * @param element 待转换的 Variable 模型元素
   * @param context 转换上下文
   */
  @Override
  public void transform(final Variable element, final TransformContext context) {
    final DmnVariableImpl dmnVariable = new DmnVariableImpl();
    dmnVariable.setKey(element.getId());
    dmnVariable.setName(element.getName());
    final DmnTypeDefinition typeDefinition = createTypeDefinition(element.getTypeRef());
    dmnVariable.setTypeDefinition(typeDefinition);
    context.addElement(dmnVariable);
  }
}
