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
package com.anyilanxin.kunpeng.engine.dmn.test.asserts;

import com.anyilanxin.kunpeng.bpm.parse.dmn.type.TypedValue;
import com.anyilanxin.kunpeng.engine.dmn.DmnDecisionResultEntries;
import org.assertj.core.api.AbstractMapAssert;

public class DmnDecisionResultEntriesAssert
    extends AbstractMapAssert<
        DmnDecisionResultEntriesAssert, DmnDecisionResultEntries, String, Object> {

  public DmnDecisionResultEntriesAssert(final DmnDecisionResultEntries actual) {
    super(actual, DmnDecisionResultEntriesAssert.class);
  }

  public DmnDecisionResultEntriesAssert hasSingleEntry(final Object value) {
    hasSize(1);
    containsValue(value);

    return this;
  }

  public DmnDecisionResultEntriesAssert hasSingleEntryTyped(final TypedValue value) {
    hasSize(1);

    final TypedValue actualValue = actual.getSingleEntryTyped();
    failIfTypedValuesAreNotEqual(value, actualValue);

    return this;
  }

  protected void failIfTypedValuesAreNotEqual(
      final TypedValue expectedValue, final TypedValue actualValue) {
    if (actualValue == null && expectedValue != null) {
      failWithMessage("Expected value to be '%s' but was null", expectedValue);
    } else if (actualValue != null && !actualValue.equals(expectedValue)) {
      failWithMessage("Expected typed value to be '%s' but was '%s'", expectedValue, actualValue);
    }
  }
}
