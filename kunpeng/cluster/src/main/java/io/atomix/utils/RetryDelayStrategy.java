/*
 * Copyright © 2026 anyilanxin zxh (anyilanxin@aliyun.com)
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package io.atomix.utils;

import java.time.Duration;
import java.util.function.Supplier;

/** Provides the delay to wait before the next retry attempt. */
@FunctionalInterface
public interface RetryDelayStrategy extends Supplier<Duration> {

  /** @return the delay to apply before the next retry */
  Duration nextDelay();

  @Override
  default Duration get() {
    return nextDelay();
  }

  /** A strategy which always returns the given fixed delay. */
  static RetryDelayStrategy fixedDelay(final Duration delay) {
    return () -> delay;
  }

  /** A strategy which returns the given delays in order, repeating the last one forever. */
  static RetryDelayStrategy boundedBackoff(
      final Duration minBackoff, final Duration maxBackoff, final double factor) {
    return new RetryDelayStrategy() {
      private Duration current = minBackoff;

      @Override
      public Duration nextDelay() {
        final var delay = current;
        final var next = Duration.ofMillis((long) (delay.toMillis() * factor));
        current = next.compareTo(maxBackoff) > 0 ? maxBackoff : next;
        return delay;
      }
    };
  }
}
