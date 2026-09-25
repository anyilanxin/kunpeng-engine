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

/**
 * 为特定的类型名称提供 {@link DmnDataTypeTransformer}。
 *
 * @author Philipp Ossler
 * @since 2026.9.0
 */
public interface DmnDataTypeTransformerRegistry {

  /**
   * 返回给定类型所匹配的转换器。
   *
   * @param typeName 类型名称
   * @return 匹配的转换器
   */
  DmnDataTypeTransformer getTransformer(String typeName);

  /**
   * 为指定的类型名称注册转换器。
   *
   * @param typeName 类型名称
   * @param transformer 对应的类型转换器
   */
  void addTransformer(String typeName, DmnDataTypeTransformer transformer);
}
