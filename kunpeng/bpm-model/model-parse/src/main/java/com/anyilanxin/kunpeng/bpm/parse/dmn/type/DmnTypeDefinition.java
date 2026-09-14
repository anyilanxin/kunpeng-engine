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

/** DMN 引擎中用于转换数据的类型定义 */
public interface DmnTypeDefinition {

  /**
   * @return 该定义的类型名称
   */
  String getTypeName();

  /**
   * 将给定的值转换为类型名称所指定的类型。
   *
   * @param value 需要转换为指定类型的值
   * @return 指定类型的值
   * @throws IllegalArgumentException 如果该值无法被转换
   */
  TypedValue transform(Object value) throws IllegalArgumentException;
}
