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
package com.anyilanxin.kunpeng.bpm.parse.dmn.qladapter;

import com.anyilanxin.kunpeng.bpm.parse.dmn.element.common.DmnExpressionImpl;
import com.anyilanxin.kunpeng.engine.script.ScriptContext;
import com.anyilanxin.kunpeng.engine.script.ScriptEngine;
import com.anyilanxin.kunpeng.engine.script.ScriptExpression;
import com.anyilanxin.kunpeng.utils.Either;
import java.util.Map;

/**
 * 基于 QlExpress 脚本引擎的 DMN 表达式求值处理器，在给定 {@link ScriptContext} 中解析并执行表达式。
 */
public class QlExpressionHandler {

  protected final ScriptEngine scriptEngine;

  /**
   * 构造处理器。
   *
   * @param scriptEngine 用于解析和执行表达式的脚本引擎
   */
  public QlExpressionHandler(final ScriptEngine scriptEngine) {
    this.scriptEngine = scriptEngine;
  }

  /**
   * 对 DMN 表达式元素求值，表达式文本为 null 时直接返回 null。
   *
   * @param expression DMN 表达式元素
   * @param scriptContext 脚本执行上下文
   * @return 求值结果；表达式文本为 null 时返回 null
   */
  public Object evaluateExpression(
      final DmnExpressionImpl expression, final ScriptContext scriptContext) {
    final String expressionText = expression.getExpression();
    if (expressionText != null) {
      return evaluate(expressionText, scriptContext, null);
    } else {
      return null;
    }
  }

  /**
   * 直接用项目的 {@link ScriptEngine} 求值。inputVariableName 非 null 时，把输入变量的值以 {@code <name>Value} 为 key
   * 再绑一份，模拟 FEEL 的隐式输入绑定。
   */
  public Object evaluate(
      final String expression, final ScriptContext scriptContext, final String inputVariableName) {
    if (expression == null) {
      return null;
    }
    final Map<String, Object> variables = scriptContext.getVariable();
    if (inputVariableName != null && variables.containsKey(inputVariableName)) {
      variables.put(inputVariableName + "Value", variables.get(inputVariableName));
    }
    final ScriptExpression scriptExpression = scriptEngine.parse("=" + expression.trim());
    final Either<String, Object> result = scriptExpression.evaluateObject(scriptContext);
    if (result.isLeft()) {
      throw new RuntimeException(
          "Unable to evaluate expression '" + expression + "': " + result.getLeft());
    }
    return result.get();
  }
}
