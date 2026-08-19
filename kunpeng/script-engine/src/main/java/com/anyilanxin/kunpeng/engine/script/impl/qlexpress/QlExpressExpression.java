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
package com.anyilanxin.kunpeng.engine.script.impl.qlexpress;

import com.alibaba.qlexpress4.Express4Runner;
import com.alibaba.qlexpress4.QLOptions;
import com.alibaba.qlexpress4.QLResult;
import com.alibaba.qlexpress4.exception.QLRuntimeException;
import com.alibaba.qlexpress4.exception.lsp.Diagnostic;
import com.anyilanxin.kunpeng.engine.script.EvaluationResult;
import com.anyilanxin.kunpeng.engine.script.Language;
import com.anyilanxin.kunpeng.engine.script.ScriptAbstractExpression;
import com.anyilanxin.kunpeng.engine.script.ScriptContext;
import com.anyilanxin.kunpeng.engine.script.exception.CustomBusinessException;
import com.anyilanxin.kunpeng.engine.script.impl.EvaluationResultImpl;
import java.util.HashSet;
import java.util.Set;
import org.springframework.beans.factory.BeanFactory;

/**
 * @author zxuanhong
 * @date 2025-10-10 12:10
 * @since
 */
public final class QlExpressExpression extends ScriptAbstractExpression {
  private boolean isValid;
  private String failureMessage;
  private String parsedText;
  private String sourceText;
  private final BeanFactory beanFactory;
  final Express4Runner express4Runner;
  private Set<String> outVarNames;

  public QlExpressExpression(
      final Express4Runner express4Runner, final String expression, final BeanFactory beanFactory) {
    this.beanFactory = beanFactory;
    this.express4Runner = express4Runner;
    try {
      express4Runner.check(expression);
      parsedText = expression;
      sourceText = expression;
      outVarNames = express4Runner.getOutVarNames(parsedText);
      isValid = false;
    } catch (final Exception e) {
      failureMessage = e.getMessage();
      isValid = true;
      parsedText = "";
      sourceText = expression;
      outVarNames = new HashSet<>();
    }
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
    return outVarNames;
  }

  @Override
  public String getSourceText() {
    return sourceText;
  }

  @Override
  public Language getLanguage() {
    return Language.QL_EXPRESS;
  }

  @Override
  protected EvaluationResult evaluate(final ScriptContext scriptContext) {
    final EvaluationResultImpl evaluationResult = new EvaluationResultImpl(getSourceText());
    if (isValid()) {
      evaluationResult.setFailure(true);
      evaluationResult.setFailureMessage(getFailureMessage());
    } else {
      final QLSpringContext context =
          new QLSpringContext(outVarNames, scriptContext.getVariable(), beanFactory);
      try {
        final QLResult execute =
            express4Runner.execute(parsedText, context, QLOptions.DEFAULT_OPTIONS);
        final Object result = execute.getResult();
        evaluationResult.setFailure(false);
        evaluationResult.setResult(result);
      } catch (final Exception e) {
        evaluationResult.setFailure(true);
        if (e.getCause() instanceof final CustomBusinessException businessException) {
          evaluationResult.setFailureMessage(businessException.getMessage());
          return evaluationResult;
        }
        if (e instanceof final QLRuntimeException qlRuntimeException) {
          final Diagnostic diagnostic = qlRuntimeException.getDiagnostic();
          final String message =
              diagnostic.getMessage().replace("com.alibaba.qlexpress4.runtime.", "");
          evaluationResult.setFailureMessage("[" + diagnostic.getSnippet() + "]:" + message);
        } else {
          evaluationResult.setFailureMessage(e.getMessage());
        }
      }
    }
    return evaluationResult;
  }
}
