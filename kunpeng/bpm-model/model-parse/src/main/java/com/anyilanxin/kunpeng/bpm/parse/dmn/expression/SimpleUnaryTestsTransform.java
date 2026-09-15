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
package com.anyilanxin.kunpeng.bpm.parse.dmn.expression;

import com.anyilanxin.kunpeng.bpm.parse.dmn.element.common.DmnExpressionImpl;
import com.anyilanxin.kunpeng.bpm.parse.dmn.element.decision.decisiontable.DmnDecisionTableInputImpl;

/** FEEL simple unary tests 表达式的转换接口，将决策表输入上的一元测试（如 "&gt; 100"、区间、"-" 任意匹配）适配为 可执行表达式。 */
public interface SimpleUnaryTestsTransform {

  /**
   * 将 FEEL simple unary tests 表达式转换为 QlExpress 可执行表达式。
   *
   * @param dmnExpression 携带一元测试表达式文本的 DMN 表达式
   * @param input 该表达式所属的决策表输入列
   * @return 转换后的 QlExpress 表达式
   */
  String transformSimpleUnaryTests(
      final DmnExpressionImpl dmnExpression, final DmnDecisionTableInputImpl input);
}
