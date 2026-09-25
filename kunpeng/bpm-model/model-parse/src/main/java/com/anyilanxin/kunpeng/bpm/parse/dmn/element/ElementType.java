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
 * DMN 内存元素类型枚举，标识解析后的元素对应 DMN 规范中的哪一类 DRG 元素。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public enum ElementType {
  /** Decision（决策） */
  DECISION,
  /** BusinessKnowledge（业务知识） */
  BUSINESS_KNOWLEDGE,
  /** DecisionTable（决策表） */
  DECISION_TABLE,
  /** DecisionLiteralExpression（字面量表达式决策逻辑） */
  DECISION_LITERAL,
  /** EncapsulatedLogic（封装的业务知识逻辑） */
  ENCAPSULATED_LOGIC,
  /** InputEntry（决策表输入条目） */
  INPUT_ENTRY,
  /** Expression（表达式） */
  EXPRESSION,
  /** 决策表输入列（Input） */
  INPUT,
  /** 决策表输出列（Output） */
  OUTPUT,
  /** 决策表规则（Rule） */
  RULE,
  /** Variable（信息项变量） */
  VARIABLE,
  /** FormalParameter（业务知识函数的形式参数） */
  FORMAL_PARAMETER
}
