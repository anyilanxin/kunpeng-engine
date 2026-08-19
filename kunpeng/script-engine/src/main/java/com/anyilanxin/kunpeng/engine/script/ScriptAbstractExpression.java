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

/**
 * @author zxuanhong
 * @date 2025-11-10 11:56
 * @since
 */
public abstract class ScriptAbstractExpression implements ScriptExpression {

  protected abstract EvaluationResult evaluate(final ScriptContext scriptContext);

  @Override
  public Either<String, Object> evaluateObject(final ScriptContext scriptContext) {
    final EvaluationResult evaluate = evaluate(scriptContext);
    if (evaluate.isFailure()) {
      return Either.left(evaluate.getFailureMessage());
    } else {
      return Either.right(evaluate.getObject());
    }
  }

  @Override
  public Either<String, Object> evaluateObject() {
    return evaluateObject(Map::of);
  }

  @Override
  public Either<String, String> evaluateString(final ScriptContext scriptContext) {
    final EvaluationResult evaluate = evaluate(scriptContext);
    if (evaluate.isFailure()) {
      return Either.left(evaluate.getFailureMessage());
    } else {
      return Either.right(evaluate.getString());
    }
  }

  @Override
  public Either<String, String> evaluateString() {
    return evaluateString(Map::of);
  }

  @Override
  public Either<String, Boolean> evaluateBoolean(final ScriptContext scriptContext) {
    final EvaluationResult evaluate = evaluate(scriptContext);
    if (evaluate.isFailure()) {
      return Either.left(evaluate.getFailureMessage());
    }
    final Boolean booleanValue = evaluate.getBoolean();
    if (evaluate.isFailure()) {
      return Either.left(evaluate.getFailureMessage());
    }
    return Either.right(booleanValue);
  }

  @Override
  public Either<String, Boolean> evaluateBoolean() {
    return evaluateBoolean(Map::of);
  }

  @Override
  public Either<String, Number> evaluateNumber(final ScriptContext scriptContext) {
    final EvaluationResult evaluate = evaluate(scriptContext);
    if (evaluate.isFailure()) {
      return Either.left(evaluate.getFailureMessage());
    }
    final Number numberValue = evaluate.getNumber();
    if (evaluate.isFailure()) {
      return Either.left(evaluate.getFailureMessage());
    }
    return Either.right(numberValue);
  }

  @Override
  public Either<String, Number> evaluateNumber() {
    return evaluateNumber(Map::of);
  }

  @Override
  public Either<String, Duration> evaluateDuration(final ScriptContext scriptContext) {
    final EvaluationResult evaluate = evaluate(scriptContext);
    if (evaluate.isFailure()) {
      return Either.left(evaluate.getFailureMessage());
    }
    final Duration durationValue = evaluate.getDuration();
    if (evaluate.isFailure()) {
      return Either.left(evaluate.getFailureMessage());
    }
    return Either.right(durationValue);
  }

  @Override
  public Either<String, Duration> evaluateDuration() {
    return evaluateDuration(Map::of);
  }

  @Override
  public Either<String, Period> evaluatePeriod(final ScriptContext scriptContext) {
    final EvaluationResult evaluate = evaluate(scriptContext);
    if (evaluate.isFailure()) {
      return Either.left(evaluate.getFailureMessage());
    }
    final Period periodValue = evaluate.getPeriod();
    if (evaluate.isFailure()) {
      return Either.left(evaluate.getFailureMessage());
    }
    return Either.right(periodValue);
  }

  @Override
  public Either<String, Period> evaluatePeriod() {
    return evaluatePeriod(Map::of);
  }

  @Override
  public Either<String, ZonedDateTime> evaluateDateTime(final ScriptContext scriptContext) {
    final EvaluationResult evaluate = evaluate(scriptContext);
    if (evaluate.isFailure()) {
      return Either.left(evaluate.getFailureMessage());
    }
    final ZonedDateTime zonedDateTimeValue = evaluate.getDateTime();
    if (evaluate.isFailure()) {
      return Either.left(evaluate.getFailureMessage());
    }
    return Either.right(zonedDateTimeValue);
  }

  @Override
  public Either<String, ZonedDateTime> evaluateDateTime() {
    return evaluateDateTime(Map::of);
  }

  @Override
  public Either<String, List<String>> evaluateListString(final ScriptContext scriptContext) {
    final EvaluationResult evaluate = evaluate(scriptContext);
    if (evaluate.isFailure()) {
      return Either.left(evaluate.getFailureMessage());
    }
    final List<String> listStringValue = evaluate.getListOfStrings();
    if (evaluate.isFailure()) {
      return Either.left(evaluate.getFailureMessage());
    }
    return Either.right(listStringValue);
  }

  @Override
  public Either<String, List<String>> evaluateListString() {
    return evaluateListString(Map::of);
  }

  @Override
  public Either<String, Map<String, Object>> evaluateMapObject(final ScriptContext scriptContext) {
    final EvaluationResult evaluate = evaluate(scriptContext);
    if (evaluate.isFailure()) {
      return Either.left(evaluate.getFailureMessage());
    }
    final Map<String, Object> mapObjectValue = evaluate.getMapOfObject();
    if (evaluate.isFailure()) {
      return Either.left(evaluate.getFailureMessage());
    }
    return Either.right(mapObjectValue);
  }

  @Override
  public Either<String, Map<String, Object>> evaluateMapObject() {
    return evaluateMapObject(Map::of);
  }
}
