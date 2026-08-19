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
package com.anyilanxin.kunpeng.engine.script.impl;

import com.anyilanxin.kunpeng.engine.script.EvaluationResult;
import com.anyilanxin.kunpeng.engine.script.Language;
import com.anyilanxin.kunpeng.engine.script.ScriptAbstractExpression;
import com.anyilanxin.kunpeng.engine.script.ScriptContext;
import java.util.Set;
import org.apache.commons.lang3.Strings;

/**
 * @author zxuanhong
 * @date 2025-10-10 12:10
 * @since
 */
public class StaticExpression extends ScriptAbstractExpression {
  private final boolean isStatic;
  private boolean isValid;
  private String failureMessage;
  private String parsedText;
  private String sourceText;
  private final Language language;

  public StaticExpression(final String expression, final Language language) {
    if (!Strings.CS.startsWith(expression, "=")) {
      isStatic = true;
      isValid = false;
      failureMessage = null;
      parsedText = expression;
      sourceText = expression;
    } else {
      isStatic = false;
    }
    this.language = language;
  }

  @Override
  public boolean isStatic() {
    return isStatic;
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
    return Set.of();
  }

  @Override
  public String getSourceText() {
    return sourceText;
  }

  @Override
  public Language getLanguage() {
    return language;
  }

  @Override
  protected EvaluationResult evaluate(final ScriptContext scriptContext) {
    final EvaluationResultImpl evaluationResult = new EvaluationResultImpl(getSourceText());
    if (isStatic()) {
      evaluationResult.setResult(getSourceText());
      evaluationResult.setFailure(false);
    }
    return evaluationResult;
  }
}
