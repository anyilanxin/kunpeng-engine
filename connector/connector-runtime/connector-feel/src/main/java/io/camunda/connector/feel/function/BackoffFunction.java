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
import com.alibaba.qlexpress4.runtime.QContext;
import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Expression function {@code backoff(attempt[, minDelay[, factor[, maxDelay[, jitterFactor]]]])}.
 * Delays may be given as a duration or as a number of milliseconds.
 */
public class BackoffFunction implements QLFunction {

  public static final String NAME = "backoff";

  private static final long DEFAULT_MIN_DELAY_MS = 50L;
  private static final long DEFAULT_MAX_DELAY_MS = 5_000L;
  private static final double DEFAULT_FACTOR = 1.6;
  private static final double DEFAULT_JITTER_FACTOR = 0.1;

  @Override
  public Object call(final QContext qContext, final Parameters parameters) throws Throwable {
    final int attempt = (int) FunctionHelper.toNumber(parameters, 0, NAME, "attempt");
    final Duration minDelay =
        parameters.size() > 1
            ? FunctionHelper.toDuration(parameters, 1, NAME, "minDelay")
            : Duration.ofMillis(DEFAULT_MIN_DELAY_MS);
    final double factor =
        parameters.size() > 2
            ? FunctionHelper.toNumber(parameters, 2, NAME, "factor")
            : DEFAULT_FACTOR;
    final Duration maxDelay =
        parameters.size() > 3
            ? FunctionHelper.toDuration(parameters, 3, NAME, "maxDelay")
            : Duration.ofMillis(DEFAULT_MAX_DELAY_MS);
    final double jitterFactor =
        parameters.size() > 4
            ? FunctionHelper.toNumber(parameters, 4, NAME, "jitterFactor")
            : DEFAULT_JITTER_FACTOR;
    return compute(attempt, minDelay, factor, maxDelay, jitterFactor);
  }

  @Override
  public String getSignature() {
    return NAME;
  }

  private static Duration compute(
      final int attempt,
      final Duration minDelay,
      final double factor,
      final Duration maxDelay,
      final double jitterFactor) {

    if (attempt < 1) {
      throw new IllegalArgumentException("backoff(): 'attempt' must be >= 1, but was " + attempt);
    }

    final long minMs = minDelay.toMillis();
    final long maxMs = maxDelay.toMillis();

    if (factor <= 0) {
      throw new IllegalArgumentException("backoff(): 'factor' must be > 0, but was " + factor);
    }
    if (minMs > maxMs) {
      throw new IllegalArgumentException(
          "backoff(): 'minDelay' must be <= 'maxDelay', but " + minMs + "ms > " + maxMs + "ms");
    }
    if (jitterFactor < 0) {
      throw new IllegalArgumentException(
          "backoff(): 'jitterFactor' must be >= 0, but was " + jitterFactor);
    }

    final double rawMs = minMs * Math.pow(factor, attempt - 1);
    final double clampedMs = Math.max(minMs, Math.min(maxMs, rawMs));

    double jitter = 0.0;
    if (jitterFactor > 0) {
      jitter =
          ThreadLocalRandom.current()
              .nextDouble(-jitterFactor * clampedMs, jitterFactor * clampedMs);
    }

    return Duration.ofMillis(Math.max(0, Math.round(clampedMs + jitter)));
  }
}
