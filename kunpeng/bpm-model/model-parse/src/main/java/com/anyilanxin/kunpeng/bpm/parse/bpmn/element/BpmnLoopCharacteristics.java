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
package com.anyilanxin.kunpeng.bpm.parse.bpmn.element;

import com.anyilanxin.kunpeng.engine.script.ScriptExpression;

/**
 * 多实例循环特征的运行时模型：以不可变值对象承载（解析期一次性确定，运行期只读）。
 *
 * @param isSequential 是否串行执行
 * @param loopCardinality 循环次数表达式，未声明为 null
 * @param completionCondition 完成条件表达式，未声明为 null
 * @param collection 迭代集合表达式，未声明为 null
 * @param elementVariable 元素变量名，未声明为 null
 * @author zxuanhong
 * @since 2026.9.0
 */
public record BpmnLoopCharacteristics(
    boolean isSequential,
    ScriptExpression loopCardinality,
    ScriptExpression completionCondition,
    ScriptExpression collection,
    String elementVariable) {

  /** 是否串行执行。 */
  public boolean isSequential() {
    return isSequential;
  }

  /** 获取循环次数表达式，未声明时返回 null。 */
  public ScriptExpression getLoopCardinality() {
    return loopCardinality;
  }

  /** 获取完成条件表达式，未声明时返回 null。 */
  public ScriptExpression getCompletionCondition() {
    return completionCondition;
  }

  /** 获取迭代集合表达式，未声明时返回 null。 */
  public ScriptExpression getCollection() {
    return collection;
  }

  /** 获取元素变量名，未声明时返回 null。 */
  public String getElementVariable() {
    return elementVariable;
  }
}
