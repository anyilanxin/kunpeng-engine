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
package com.anyilanxin.kunpeng.bpm.model.xml.impl.validation;

import com.anyilanxin.kunpeng.bpm.model.xml.instance.ModelElementInstance;
import com.anyilanxin.kunpeng.bpm.model.xml.validation.ValidationResult;
import com.anyilanxin.kunpeng.bpm.model.xml.validation.ValidationResultFormatter;
import com.anyilanxin.kunpeng.bpm.model.xml.validation.ValidationResults;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * @author Daniel Meyer
 */
public class ModelValidationResultsImpl implements ValidationResults {

  protected Map<ModelElementInstance, List<ValidationResult>> collectedResults;

  protected int errorCount;
  protected int warningCount;

  public ModelValidationResultsImpl(
      final Map<ModelElementInstance, List<ValidationResult>> collectedResults,
      final int errorCount,
      final int warningCount) {
    this.collectedResults = collectedResults;
    this.errorCount = errorCount;
    this.warningCount = warningCount;
  }

  public ModelValidationResultsImpl(final ValidationResults... validationResults) {
    collectedResults = new HashMap<>();
    for (final var entry : validationResults) {
      collectedResults.putAll(entry.getResults());
      errorCount += entry.getErrorCount();
      warningCount += entry.getWarinigCount();
    }
  }

  @Override
  public boolean hasErrors() {
    return errorCount > 0;
  }

  @Override
  public int getErrorCount() {
    return errorCount;
  }

  @Override
  public int getWarinigCount() {
    return warningCount;
  }

  @Override
  public void write(final StringWriter writer, final ValidationResultFormatter formatter) {
    for (final Entry<ModelElementInstance, List<ValidationResult>> entry :
        collectedResults.entrySet()) {

      final ModelElementInstance element = entry.getKey();
      final List<ValidationResult> results = entry.getValue();

      formatter.formatElement(writer, element);

      for (final ValidationResult result : results) {
        formatter.formatResult(writer, result);
      }
    }
  }

  @Override
  public void write(
      final StringWriter writer, final ValidationResultFormatter formatter, final int maxSize) {
    int printedCount = 0;
    int previousLength = 0;
    for (final var entry : collectedResults.entrySet()) {
      final var element = entry.getKey();
      final var results = entry.getValue();

      formatter.formatElement(writer, element);

      for (final var result : results) {
        formatter.formatResult(writer, result);

        // Size and Length are not necessarily the same, depending on the encoding of the string.
        final int currentSize = writer.getBuffer().toString().getBytes().length;
        final int currentLength = writer.getBuffer().length();
        if (!canAccommodateResult(maxSize, currentSize, printedCount, formatter)) {
          writer.getBuffer().setLength(previousLength);
          final int remaining = errorCount + warningCount - printedCount;
          formatter.formatSuffixWithOmittedResultsCount(writer, remaining);
          return;
        }
        printedCount++;
        previousLength = currentLength;
      }
    }
  }

  private boolean canAccommodateResult(
      final int maxSize,
      final int currentSize,
      final int printedCount,
      final ValidationResultFormatter formatter) {
    final boolean isLastItemToPrint = printedCount == errorCount + warningCount - 1;
    if (isLastItemToPrint && currentSize <= maxSize) {
      return true;
    }
    final int remaining = errorCount + warningCount - printedCount;
    final int suffixLength = formatter.getFormattedSuffixWithOmittedResultsSize(remaining);
    return currentSize + suffixLength <= maxSize;
  }

  @Override
  public Map<ModelElementInstance, List<ValidationResult>> getResults() {
    return collectedResults;
  }
}
