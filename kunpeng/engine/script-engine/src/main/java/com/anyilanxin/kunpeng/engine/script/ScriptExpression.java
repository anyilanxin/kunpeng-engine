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
package com.anyilanxin.kunpeng.engine.script;

import com.anyilanxin.kunpeng.utils.Either;
import java.time.Duration;
import java.time.Period;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author zxuanhong
 * @date 2025-11-10 11:56
 * @since
 */
public interface ScriptExpression {

  Language getLanguage();

  /**
   * @return {@code true} if it is a static expression that does not require additional context
   *     variables
   */
  boolean isStatic();

  /**
   * @return {@code true} if the expression is valid and can be evaluated
   */
  boolean isValid();

  /**
   * Returns the reason why the expression is not valid. Use {@link #isValid()} to check if the
   * expression is valid or not.
   *
   * @return the failure message if the expression is not valid, otherwise {@code null}
   */
  String getFailureMessage();

  String getParsedText();

  Set<String> outVarNames();

  /**
   * Returns the source text of this expression.
   *
   * @return the source text
   */
  String getSourceText();

  Either<String, Object> evaluateObject(ScriptContext scriptContext);

  Either<String, Object> evaluateObject();

  Either<String, String> evaluateString(ScriptContext scriptContext);

  Either<String, String> evaluateString();

  Either<String, Boolean> evaluateBoolean(ScriptContext scriptContext);

  Either<String, Boolean> evaluateBoolean();

  Either<String, Number> evaluateNumber(ScriptContext scriptContext);

  Either<String, Number> evaluateNumber();

  Either<String, Duration> evaluateDuration(ScriptContext scriptContext);

  Either<String, Duration> evaluateDuration();

  Either<String, Period> evaluatePeriod(ScriptContext scriptContext);

  Either<String, Period> evaluatePeriod();

  Either<String, ZonedDateTime> evaluateDateTime(ScriptContext scriptContext);

  Either<String, ZonedDateTime> evaluateDateTime();

  Either<String, List<String>> evaluateListString(ScriptContext scriptContext);

  Either<String, List<String>> evaluateListString();

  Either<String, Map<String, Object>> evaluateMapObject(ScriptContext scriptContext);

  Either<String, Map<String, Object>> evaluateMapObject();
}
