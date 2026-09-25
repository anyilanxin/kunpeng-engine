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

package com.anyilanxin.kunpeng.bpm.parse.dmn.type.impl;

import com.anyilanxin.kunpeng.bpm.parse.dmn.type.DmnDataTypeTransformer;
import com.anyilanxin.kunpeng.bpm.parse.dmn.type.DmnTypeDefinition;
import com.anyilanxin.kunpeng.bpm.parse.dmn.type.TypedValue;
import com.anyilanxin.kunpeng.bpm.parse.dmn.type.Variables;
import com.anyilanxin.kunpeng.bpm.parse.exception.DmnParseException;

/**
 * {@link DmnTypeDefinition} 的默认实现：持有类型名称，并委托 {@link DmnDataTypeTransformer} 完成值的类型转换。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class DmnTypeDefinitionImpl implements DmnTypeDefinition {

  protected String typeName;
  protected DmnDataTypeTransformer transformer;

  /**
   * 使用指定的类型名称和转换器创建实例。
   *
   * @param typeName 类型名称
   * @param transformer 类型转换器
   */
  public DmnTypeDefinitionImpl(final String typeName, final DmnDataTypeTransformer transformer) {
    this.typeName = typeName;
    this.transformer = transformer;
  }

  /**
   * 将值转换为目标类型，null 值直接返回无类型空值。
   *
   * @param value 待转换的值
   * @return 转换后的类型值
   */
  @Override
  public TypedValue transform(final Object value) {
    if (value == null) {
      return Variables.untypedNullValue();
    } else {
      return transformNotNullValue(value);
    }
  }

  /**
   * 转换非 null 值；转换器缺失时抛出 {@link IllegalArgumentException}，转换失败时将 {@link IllegalArgumentException}
   * 包装为 {@link DmnParseException} 抛出。
   *
   * @param value 待转换的非 null 值
   * @return 转换后的类型值
   */
  protected TypedValue transformNotNullValue(final Object value) {
    if (transformer == null) {
      throw new IllegalArgumentException("Transformer for type '" + typeName + "' is null");
    }

    try {

      return transformer.transform(value);

    } catch (final IllegalArgumentException e) {
      throw new DmnParseException(
          "Unable to transform value '" + value + "' to type '" + typeName + "'", e);
    }
  }

  @Override
  public String getTypeName() {
    return typeName;
  }

  /**
   * 设置类型名称。
   *
   * @param typeName 类型名称
   */
  public void setTypeName(final String typeName) {
    this.typeName = typeName;
  }

  /**
   * 设置类型转换器。
   *
   * @param transformer 类型转换器
   */
  public void setTransformer(final DmnDataTypeTransformer transformer) {
    this.transformer = transformer;
  }

  @Override
  public String toString() {
    return "DmnTypeDefinitionImpl{" + "typeName='" + typeName + '\'' + '}';
  }
}
