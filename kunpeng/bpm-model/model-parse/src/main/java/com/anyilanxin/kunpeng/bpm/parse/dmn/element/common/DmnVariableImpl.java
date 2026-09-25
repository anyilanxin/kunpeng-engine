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

package com.anyilanxin.kunpeng.bpm.parse.dmn.element.common;

import com.anyilanxin.kunpeng.bpm.parse.dmn.element.DmnElement;
import com.anyilanxin.kunpeng.bpm.parse.dmn.element.ElementType;
import com.anyilanxin.kunpeng.bpm.parse.dmn.type.DmnTypeDefinition;

/**
 * DMN 变量（InformationItem/Variable）的内存模型，包含变量名与类型定义。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class DmnVariableImpl implements DmnElement {

  protected String key;
  protected String name;

  /** 变量的类型定义 */
  protected DmnTypeDefinition typeDefinition;

  @Override
  public String getKey() {
    return key;
  }

  /** 设置变量唯一标识 */
  public void setKey(final String key) {
    this.key = key;
  }

  /** 获取变量名称 */
  public String getName() {
    return name;
  }

  /** 设置变量名称 */
  public void setName(final String name) {
    this.name = name;
  }

  /** 获取变量的类型定义 */
  public DmnTypeDefinition getTypeDefinition() {
    return typeDefinition;
  }

  /** 设置变量的类型定义 */
  public void setTypeDefinition(final DmnTypeDefinition typeDefinition) {
    this.typeDefinition = typeDefinition;
  }

  /** 返回元素类型 {@link ElementType#VARIABLE} */
  @Override
  public ElementType getType() {
    return ElementType.VARIABLE;
  }

  @Override
  public String toString() {
    return "DmnVariableImpl{"
        + "key='"
        + key
        + '\''
        + ", name='"
        + name
        + '\''
        + ", typeDefinition="
        + typeDefinition
        + '}';
  }
}
