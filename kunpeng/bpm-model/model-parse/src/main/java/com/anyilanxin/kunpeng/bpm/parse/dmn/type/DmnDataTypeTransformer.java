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
 * 将值转换为特定的类型。
 *
 * @author Philipp Ossler
 */
public interface DmnDataTypeTransformer {

  /**
   * 转换给定的值。
   *
   * @param value 任意类型的值
   * @return 特定类型的值
   * @throws IllegalArgumentException 如果该值无法被转换
   */
  TypedValue transform(Object value) throws IllegalArgumentException;
}
