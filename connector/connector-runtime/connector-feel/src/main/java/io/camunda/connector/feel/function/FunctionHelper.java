/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * Copyright © 2026 anyilanxin zxh (anyilanxin@aliyun.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.camunda.connector.feel.function;

import com.alibaba.qlexpress4.runtime.Parameters;
import java.time.Duration;
import java.util.Map;

/** Shared type-safe argument helpers for Connector Expression functions. */
class FunctionHelper {

  private FunctionHelper() {}

  static String toString(Parameters parameters, int index, String functionName, String paramName) {
    Object value = parameters.getValue(index);
    if (value instanceof String string) {
      return string;
    }
    throw new IllegalArgumentException(
        String.format("Parameter '%s' of function '%s' must be a String", paramName, functionName));
  }

  @SuppressWarnings("unchecked")
  static Map<String, Object> toMap(
      Parameters parameters, int index, String functionName, String paramName) {
    Object value = parameters.getValue(index);
    // qlexpress evaluates an empty map literal '{}' to null, meaning "no variables"
    if (value == null) {
      return Map.of();
    }
    if (value instanceof Map<?, ?> map) {
      return (Map<String, Object>) map;
    }
    throw new IllegalArgumentException(
        String.format(
            "Parameter '%s' of function '%s' must be a Context", paramName, functionName));
  }

  static double toNumber(Parameters parameters, int index, String functionName, String paramName) {
    Object value = parameters.getValue(index);
    if (value instanceof Number number) {
      return number.doubleValue();
    }
    throw new IllegalArgumentException(
        String.format("Parameter '%s' of function '%s' must be a Number", paramName, functionName));
  }

  static Duration toDuration(
      Parameters parameters, int index, String functionName, String paramName) {
    Object value = parameters.getValue(index);
    if (value instanceof Duration duration) {
      return duration;
    }
    if (value instanceof Number millis) {
      return Duration.ofMillis(millis.longValue());
    }
    throw new IllegalArgumentException(
        String.format(
            "Parameter '%s' of function '%s' must be a duration or a number of milliseconds",
            paramName, functionName));
  }
}
