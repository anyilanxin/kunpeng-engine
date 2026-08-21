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
package io.atomix.test.util;

import java.time.Duration;
import org.awaitility.Awaitility;

/** Small collection of test helpers. */
public final class TestUtil {

  private TestUtil() {}

  /**
   * Waits until the given condition is met, or fails if it is not met within a reasonable timeout.
   *
   * @param condition condition to wait for
   */
  public static void waitUntil(final BooleanSupplier condition) {
    Awaitility.await()
        .atMost(Duration.ofSeconds(30))
        .pollInterval(Duration.ofMillis(25))
        .untilAsserted(
            () -> {
              if (!condition.getAsBoolean()) {
                throw new AssertionError("Condition was not met in time");
              }
            });
  }

  /** Supplier of a primitive boolean, used by {@link #waitUntil(BooleanSupplier)}. */
  @FunctionalInterface
  public interface BooleanSupplier {
    boolean getAsBoolean();
  }
}
