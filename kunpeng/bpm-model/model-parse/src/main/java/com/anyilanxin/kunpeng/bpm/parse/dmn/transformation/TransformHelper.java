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
package com.anyilanxin.kunpeng.bpm.parse.dmn.transformation;

import com.anyilanxin.kunpeng.bpm.model.dmn.impl.DmnModelConstants;
import com.anyilanxin.kunpeng.bpm.model.dmn.instance.LiteralExpression;
import com.anyilanxin.kunpeng.bpm.model.dmn.instance.Text;

/**
 * DMN 转换辅助工具：解析表达式语言（元素局部声明优先，缺失时回退全局声明并剔除 FEEL 命名空间）并提取字面表达式的文本内容。
 */
public final class TransformHelper {

  /**
   * 解析表达式语言：元素自身声明的语言优先，未声明时回退到 Definitions 上的全局表达式语言。
   *
   * @param context 转换上下文
   * @param expressionLanguage 元素自身声明的表达式语言，可为 null
   * @return 解析出的表达式语言；全局语言为 FEEL 命名空间时返回 null
   */
  public static String getExpressionLanguage(
      final TransformContext context, final String expressionLanguage) {
    if (expressionLanguage != null) {
      return expressionLanguage;
    } else {
      return getGlobalExpressionLanguage(context);
    }
  }

  /** 获取全局表达式语言；若为 FEEL 命名空间则视为未显式声明，返回 null。 */
  private static String getGlobalExpressionLanguage(final TransformContext context) {
    final String expressionLanguage = context.getLanguage();
    if (!DmnModelConstants.FEEL_NS.equals(expressionLanguage)
        && !DmnModelConstants.FEEL12_NS.equals(expressionLanguage)
        && !DmnModelConstants.FEEL13_NS.equals(expressionLanguage)
        && !DmnModelConstants.FEEL14_NS.equals(expressionLanguage)
        && !DmnModelConstants.FEEL15_NS.equals(expressionLanguage)) {
      return expressionLanguage;
    } else {
      return null;
    }
  }

  /**
   * 提取字面表达式的文本内容。
   *
   * @param expression 字面表达式
   * @return 文本内容，无有效文本时返回 null
   */
  public static String getExpression(final LiteralExpression expression) {
    return getExpression(expression.getText());
  }

  /** 提取 Text 元素的文本内容，为空时返回 null。 */
  private static String getExpression(final Text text) {
    if (text != null) {
      final String textContent = text.getTextContent();
      if (textContent != null && !textContent.isEmpty()) {
        return textContent;
      }
    }
    return null;
  }
}
