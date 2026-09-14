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

import static com.anyilanxin.kunpeng.bpm.parse.dmn.transformation.TransformHelper.getExpressionLanguage;
import static com.anyilanxin.kunpeng.bpm.parse.dmn.type.TypeHelper.getExpression;

import com.anyilanxin.kunpeng.bpm.parse.dmn.element.common.DmnExpressionImpl;
import com.anyilanxin.kunpeng.bpm.parse.dmn.transformation.ModelElementTransformer;
import com.anyilanxin.kunpeng.bpm.parse.dmn.transformation.TransformContext;
import com.anyilanxin.kunpeng.bpm.model.dmn.instance.InputEntry;

/**
 * 将 DMN InputEntry（决策表条件单元格）转换为运行时 DmnExpression 条件表达式：提取标签、表达式语言与 FEEL 文本；
 * 脚本编译统一由 DecisionTableTransformer 完成。
 */
public final class InputEntryTransformer implements ModelElementTransformer<InputEntry> {
  /** 返回本转换器处理的 DMN 模型元素类型。 */
  @Override
  public Class<InputEntry> getType() {
    return InputEntry.class;
  }

  /**
   * 转换条件单元格为条件表达式元素并注册到上下文。
   *
   * @param element 待转换的 InputEntry 模型元素
   * @param context 转换上下文
   */
  @Override
  public void transform(final InputEntry element, final TransformContext context) {
    final DmnExpressionImpl condition = new DmnExpressionImpl();
    condition.setKey(element.getId());
    condition.setName(element.getLabel());
    condition.setExpressionLanguage(
        getExpressionLanguage(context, element.getExpressionLanguage()));
    condition.setExpression(getExpression(element));
    context.addElement(condition);
  }

  private void attachToActivity() {}
}
