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

package com.anyilanxin.kunpeng.bpm.parse.dmn.type;

import com.anyilanxin.kunpeng.bpm.model.dmn.instance.LiteralExpression;
import com.anyilanxin.kunpeng.bpm.model.dmn.instance.Text;
import com.anyilanxin.kunpeng.bpm.model.dmn.instance.UnaryTests;
import com.anyilanxin.kunpeng.bpm.parse.dmn.type.impl.DefaultDataTypeTransformerRegistry;
import com.anyilanxin.kunpeng.bpm.parse.dmn.type.impl.DefaultTypeDefinition;
import com.anyilanxin.kunpeng.bpm.parse.dmn.type.impl.DmnTypeDefinitionImpl;

/**
 * DMN 类型系统的工具类：根据类型引用（typeRef）创建类型定义，并从 DMN 表达式中提取文本内容。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class TypeHelper {
  /** 内置数据类型转换器注册表（静态共享实例）。 */
  private static final DefaultDataTypeTransformerRegistry typeTransformerRegistry =
      new DefaultDataTypeTransformerRegistry();

  /**
   * 根据类型引用（typeRef）创建类型定义，typeRef 为 null 时返回无类型（untyped）的定义。
   *
   * @param typeRef DMN 类型引用，如 {@code string}、{@code integer}
   * @return 对应的类型定义
   */
  public static DmnTypeDefinition createTypeDefinition(final String typeRef) {
    if (typeRef != null) {
      final DmnDataTypeTransformer transformer = typeTransformerRegistry.getTransformer(typeRef);
      return new DmnTypeDefinitionImpl(typeRef, transformer);
    } else {
      return new DefaultTypeDefinition();
    }
  }

  /**
   * 提取字面量表达式（LiteralExpression）中的表达式文本。
   *
   * @param expression 字面量表达式
   * @return 表达式文本，文本不存在或为空时返回 null
   */
  public static String getExpression(final LiteralExpression expression) {
    return getExpression(expression.getText());
  }

  /**
   * 提取一元测试表达式（UnaryTests）中的表达式文本。
   *
   * @param expression 一元测试表达式
   * @return 表达式文本，文本不存在或为空时返回 null
   */
  public static String getExpression(final UnaryTests expression) {
    return getExpression(expression.getText());
  }

  /**
   * 提取 {@link Text} 元素中的非空文本内容。
   *
   * @param text 表达式文本元素，可为 null
   * @return 非空的文本内容，否则返回 null
   */
  protected static String getExpression(final Text text) {
    if (text != null) {
      final String textContent = text.getTextContent();
      if (textContent != null && !textContent.isEmpty()) {
        return textContent;
      }
    }
    return null;
  }
}
