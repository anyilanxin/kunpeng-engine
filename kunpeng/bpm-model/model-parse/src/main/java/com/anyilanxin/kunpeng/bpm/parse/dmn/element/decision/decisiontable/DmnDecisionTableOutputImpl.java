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

package com.anyilanxin.kunpeng.bpm.parse.dmn.element.decision.decisiontable;

import com.anyilanxin.kunpeng.bpm.parse.dmn.element.DmnElement;
import com.anyilanxin.kunpeng.bpm.parse.dmn.element.ElementType;
import com.anyilanxin.kunpeng.bpm.parse.dmn.type.DmnTypeDefinition;

/** 决策表输出列（Output）的内存模型，持有输出名称与输出结果的类型定义。 */
public class DmnDecisionTableOutputImpl implements DmnElement {

  protected String id;
  protected String name;
  /** 输出结果的变量名（用于组合输出时引用） */
  protected String outputName;
  /** 输出结果的类型定义 */
  protected DmnTypeDefinition typeDefinition;

  @Override
  public String getKey() {
    return id;
  }

  /** 设置输出列唯一标识 */
  public void setKey(final String id) {
    this.id = id;
  }

  /** 获取输出列名称 */
  public String getName() {
    return name;
  }

  /** 设置输出列名称 */
  public void setName(final String name) {
    this.name = name;
  }

  /** 获取输出结果的变量名 */
  public String getOutputName() {
    return outputName;
  }

  /** 设置输出结果的变量名 */
  public void setOutputName(final String outputName) {
    this.outputName = outputName;
  }

  /** 获取输出结果的类型定义 */
  public DmnTypeDefinition getTypeDefinition() {
    return typeDefinition;
  }

  /** 设置输出结果的类型定义 */
  public void setTypeDefinition(final DmnTypeDefinition typeDefinition) {
    this.typeDefinition = typeDefinition;
  }

  /** 返回元素类型 {@link ElementType#OUTPUT} */
  @Override
  public ElementType getType() {
    return ElementType.OUTPUT;
  }

  @Override
  public String toString() {
    return "DmnDecisionTableOutputImpl{"
        + "id='"
        + id
        + '\''
        + ", name='"
        + name
        + '\''
        + ", outputName='"
        + outputName
        + '\''
        + ", typeDefinition="
        + typeDefinition
        + '}';
  }
}
