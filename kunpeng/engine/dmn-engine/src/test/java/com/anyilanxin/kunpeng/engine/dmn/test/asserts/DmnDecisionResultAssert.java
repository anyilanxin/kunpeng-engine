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

import com.anyilanxin.kunpeng.engine.dmn.DmnDecisionResult;
import com.anyilanxin.kunpeng.engine.dmn.DmnDecisionResultEntries;
import org.assertj.core.api.AbstractListAssert;

public class DmnDecisionResultAssert
    extends AbstractListAssert<
        DmnDecisionResultAssert, DmnDecisionResult, DmnDecisionResultEntries,
        DmnDecisionResultEntriesAssert> {

  public DmnDecisionResultAssert(final DmnDecisionResult actual) {
    super(actual, DmnDecisionResultAssert.class);
  }

  public DmnDecisionResultEntriesAssert hasSingleResult() {
    hasSize(1);

    final DmnDecisionResultEntries singleResult = actual.getSingleResult();

    return new DmnDecisionResultEntriesAssert(singleResult);
  }

  @Override
  protected DmnDecisionResultEntriesAssert toAssert(
      final DmnDecisionResultEntries value, final String description) {
    info.description(description, "");

    return new DmnDecisionResultEntriesAssert(value);
  }

  @Override
  protected DmnDecisionResultAssert newAbstractIterableAssert(
      final Iterable<? extends DmnDecisionResultEntries> iterable) {
    if (iterable instanceof final DmnDecisionResult result) {
      return new DmnDecisionResultAssert(result);
    }
    throw new UnsupportedOperationException(
        "DmnDecisionResultAssert only supports DmnDecisionResult instances");
  }
}
