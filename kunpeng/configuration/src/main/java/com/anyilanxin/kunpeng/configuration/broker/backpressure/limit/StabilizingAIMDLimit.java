/*
 * Copyright © 2026 anyilanxin zxh(anyilanxin@aliyun.com)
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
package com.anyilanxin.kunpeng.configuration.broker.backpressure.limit;

import com.google.common.base.Preconditions;
import com.netflix.concurrency.limits.limit.AbstractLimit;
import java.util.concurrent.TimeUnit;

/**
 * The limit is calculated purely based on the configured expectedRTT and observed rtt (round trip
 * time). The algorithm tries to keep the rtts around expectedRTT. It is not guaranteed to be always
 * less than that. But on an average, rtts will be less than the configured expectedRTT.
 *
 * <p>The implementation is based on {@link com.netflix.concurrency.limits.limit.AIMDLimit}.
 * AIMDLimit has a limitation that the limit fluctuates between 1 and 2*X, where X is the optimal
 * limit for the system. It rarely stabilizes at X. This results in always fluctuating throughput
 * and latency. {@link StabilizingAIMDLimit} fixes this issue, by not reducing the limit if the
 * inflight is greater than the current limit. As a result it attempts to keep the limit around X.
 */
public final class StabilizingAIMDLimit extends AbstractLimit {

  private static final long DEFAULT_MAX_RTT = TimeUnit.SECONDS.toNanos(2);
  private final int minLimit;
  private final int maxLimit;
  private final double backoffRatio;
  private final long expectedRTT;

  private StabilizingAIMDLimit(
      final int initialLimit,
      final int maxLimit,
      final int minLimit,
      final double backoffRatio,
      final long expectedRTT) {
    super(initialLimit);
    this.maxLimit = maxLimit;
    this.minLimit = minLimit;
    this.backoffRatio = backoffRatio;
    this.expectedRTT = expectedRTT;
  }

  @Override
  protected int _update(
      final long startTime, final long rtt, final int inflight, final boolean didDrop) {
    int currentLimit = getLimit();

    if ((didDrop || rtt > expectedRTT)) {
      if (inflight <= currentLimit) {
        currentLimit = (int) (currentLimit * backoffRatio);
      }
    } else if (inflight * 2 >= currentLimit) {
      currentLimit = currentLimit + 1;
    }

    return Math.min(maxLimit, Math.max(minLimit, currentLimit));
  }

  @Override
  public String toString() {
    return "StabilizingAIMDLimit [limit=" + getLimit() + "]";
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  public static final class Builder {
    private int minLimit = 10;
    private int initialLimit = 100;
    private int maxLimit = 1000;
    private double backoffRatio = 0.9;
    private long expectedRtt = DEFAULT_MAX_RTT;

    public Builder initialLimit(final int initialLimit) {
      this.initialLimit = initialLimit;
      return this;
    }

    public Builder minLimit(final int minLimit) {
      this.minLimit = minLimit;
      return this;
    }

    public Builder maxLimit(final int maxLimit) {
      this.maxLimit = maxLimit;
      return this;
    }

    /**
     * When the limit has to be reduced, the new limit is calculated as current limit *
     * backoffRatio.
     */
    public Builder backoffRatio(final double backoffRatio) {
      Preconditions.checkArgument(
          backoffRatio < 1.0 && backoffRatio >= 0.5,
          "Backoff ratio must be in the range [0.5, 1.0)");
      this.backoffRatio = backoffRatio;
      return this;
    }

    /** When observed RTT exceeds this value, the limit will be reduced. */
    public Builder expectedRTT(final long timeout, final TimeUnit units) {
      Preconditions.checkArgument(timeout > 0, "Timeout must be positive");
      expectedRtt = units.toNanos(timeout);
      return this;
    }

    public StabilizingAIMDLimit build() {
      return new StabilizingAIMDLimit(initialLimit, maxLimit, minLimit, backoffRatio, expectedRtt);
    }
  }
}
