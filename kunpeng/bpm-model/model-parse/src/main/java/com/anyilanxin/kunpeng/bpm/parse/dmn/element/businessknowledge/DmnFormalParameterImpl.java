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

package com.anyilanxin.kunpeng.bpm.parse.dmn.element.businessknowledge;

import com.anyilanxin.kunpeng.bpm.parse.dmn.type.DmnTypeDefinition;

/** 业务知识函数的形式参数（FormalParameter）模型，包含参数名与类型定义。 */
public class DmnFormalParameterImpl {

  protected String name;

  /** 参数的类型定义 */
  protected DmnTypeDefinition typeDefinition;

  /** 获取参数名 */
  public String getName() {
    return name;
  }

  /** 设置参数名 */
  public void setName(final String name) {
    this.name = name;
  }

  /** 获取参数的类型定义 */
  public DmnTypeDefinition getTypeDefinition() {
    return typeDefinition;
  }

  /** 设置参数的类型定义 */
  public void setTypeDefinition(final DmnTypeDefinition typeDefinition) {
    this.typeDefinition = typeDefinition;
  }

  @Override
  public String toString() {
    return "DmnFormalParameterImpl{"
        + "name='"
        + name
        + '\''
        + ", typeDefinition="
        + typeDefinition
        + '}';
  }
}
