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
package com.anyilanxin.kunpeng.bpm.parse.dmn.element;

/**
 * DMN 解析后内存元素模型的根接口，提供元素的唯一标识与类型信息。
 *
 * @author zxuanhong
 * @since
 */
public interface DmnElement {

  /**
   * 元素的唯一标识符。
   *
   * @return 标识符，未设置时返回 null
   */
  String getKey();

  /**
   * 元素的类型。
   *
   * @return 元素类型
   */
  ElementType getType();
}
