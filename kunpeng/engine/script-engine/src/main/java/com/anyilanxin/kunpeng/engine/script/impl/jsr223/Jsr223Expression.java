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
package com.anyilanxin.kunpeng.engine.script.impl.jsr223;

import com.anyilanxin.kunpeng.engine.script.EvaluationResult;
import com.anyilanxin.kunpeng.engine.script.ScriptAbstractExpression;
import com.anyilanxin.kunpeng.engine.script.ScriptContext;
import com.anyilanxin.kunpeng.engine.script.impl.EvaluationResultImpl;
import java.util.Set;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.SimpleScriptContext;

/**
 * JSR-223 编译型表达式基类：构造期编译校验并保留 {@link CompiledScript}，求值复用编译产物—— 每次求值重新走源码编译是纯浪费（JavaScript/Python
 * 共用）。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public abstract class Jsr223Expression extends ScriptAbstractExpression {
  private final CompiledScript compiled;
  private final String sourceText;
  private final String failureMessage;
  private final boolean isValid;

  protected Jsr223Expression(final Compilable engine, final String expression) {
    sourceText = expression;
    CompiledScript compiledResult = null;
    String failure = null;
    try {
      compiledResult = engine.compile(expression);
    } catch (final Exception e) {
      failure = e.getMessage();
    }
    compiled = compiledResult;
    failureMessage = failure;
    isValid = compiledResult != null;
  }

  @Override
  public boolean isStatic() {
    return false;
  }

  @Override
  public boolean isValid() {
    return isValid;
  }

  @Override
  public String getFailureMessage() {
    return failureMessage;
  }

  @Override
  public String getParsedText() {
    return isValid ? sourceText : "";
  }

  @Override
  public Set<String> outVarNames() {
    throw new RuntimeException("not implemented");
  }

  @Override
  public String getSourceText() {
    return sourceText;
  }

  @Override
  protected EvaluationResult evaluate(final ScriptContext scriptContext) {
    final EvaluationResultImpl evaluationResult = new EvaluationResultImpl(sourceText);
    if (!isValid) {
      evaluationResult.setFailure(true);
      evaluationResult.setFailureMessage(failureMessage);
      return evaluationResult;
    }
    final javax.script.ScriptContext context = new SimpleScriptContext();
    if (scriptContext != null
        && scriptContext.getVariable() != null
        && !scriptContext.getVariable().isEmpty()) {
      scriptContext
          .getVariable()
          .forEach(
              (key, value) ->
                  context.setAttribute(key, value, javax.script.ScriptContext.ENGINE_SCOPE));
    }
    try {
      final Object result = compiled.eval(context);
      evaluationResult.setFailure(false);
      evaluationResult.setResult(result);
    } catch (final Exception e) {
      evaluationResult.setFailure(true);
      evaluationResult.setFailureMessage(e.getMessage());
    }
    return evaluationResult;
  }
}
