/*
 * Copyright © 2025 anyilanxin zxh(anyilanxin@aliyun.com)
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
package com.anyilanxin.kunpeng.engine.script.impl.python;

import com.anyilanxin.kunpeng.engine.script.EvaluationResult;
import com.anyilanxin.kunpeng.engine.script.Language;
import com.anyilanxin.kunpeng.engine.script.ScriptAbstractExpression;
import com.anyilanxin.kunpeng.engine.script.ScriptContext;
import com.anyilanxin.kunpeng.engine.script.impl.EvaluationResultImpl;
import java.util.Set;
import javax.script.SimpleScriptContext;
import org.python.jsr223.PyScriptEngine;

/**
 * @author zxuanhong
 * @date 2025-10-10 12:10
 * @since
 */
public final class PythonExpression extends ScriptAbstractExpression {
  private boolean isValid;
  private String failureMessage;
  private String parsedText;
  private String sourceText;
  private final PyScriptEngine engine;

  public PythonExpression(final PyScriptEngine engine, final String expression) {
    this.engine = engine;
    try {
      engine.compile(expression);
      parsedText = expression;
      sourceText = expression;
      isValid = false;
    } catch (final Exception e) {
      failureMessage = e.getMessage();
      isValid = true;
      parsedText = "";
      sourceText = expression;
    }
  }

  @Override
  public Language getLanguage() {
    return Language.PYTHON;
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
    return parsedText;
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
    final EvaluationResultImpl evaluationResult = new EvaluationResultImpl(getSourceText());
    if (isValid()) {
      evaluationResult.setFailure(true);
      evaluationResult.setFailureMessage(getFailureMessage());
    } else {
      final javax.script.ScriptContext context = new SimpleScriptContext();
      if (scriptContext != null
          && scriptContext.getVariable() != null
          && !scriptContext.getVariable().isEmpty()) {
        for (final String key : scriptContext.getVariable().keySet()) {
          context.setAttribute(
              key, scriptContext.getVariable().get(key), javax.script.ScriptContext.ENGINE_SCOPE);
        }
      }
      try {
        final Object result = engine.eval(parsedText, context);
        evaluationResult.setFailure(false);
        evaluationResult.setResult(result);
      } catch (final Exception e) {
        evaluationResult.setFailure(true);
        evaluationResult.setFailureMessage(e.getMessage());
      }
    }
    return evaluationResult;
  }
}
