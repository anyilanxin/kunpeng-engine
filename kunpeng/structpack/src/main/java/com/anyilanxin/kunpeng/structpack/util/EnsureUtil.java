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
package com.anyilanxin.kunpeng.structpack.util;

import java.time.Duration;
import org.agrona.DirectBuffer;

public class EnsureUtil {

  public static void ensureNotNull(final String property, final Object o) {
    if (o == null) {
      throw new RuntimeException(property + " must not be null");
    }
  }

  public static void ensureNotEmpty(final String property, final String value) {
    if (value.isEmpty()) {
      throw new RuntimeException(property + " must not be empty");
    }
  }

  public static void ensureGreaterThan(
      final String property, final long testValue, final long comparisonValue) {
    if (testValue <= comparisonValue) {
      throw new RuntimeException(property + " must be greater than " + comparisonValue);
    }
  }

  public static void ensureGreaterThanOrEqual(
      final String property, final long testValue, final long comparisonValue) {
    if (testValue < comparisonValue) {
      throw new RuntimeException(property + " must be greater than or equal to " + comparisonValue);
    }
  }

  public static void ensureLessThan(
      final String property, final long testValue, final long comparisonValue) {
    if (testValue >= comparisonValue) {
      throw new RuntimeException(property + " must be less than " + comparisonValue);
    }
  }

  public static void ensureLessThanOrEqual(
      final String property, final long testValue, final long comparisonValue) {
    if (testValue > comparisonValue) {
      throw new RuntimeException(property + " must be less than or equal to " + comparisonValue);
    }
  }

  public static void ensureGreaterThan(
      final String property, final double testValue, final double comparisonValue) {
    if (testValue <= comparisonValue) {
      throw new RuntimeException(property + " must be greater than " + comparisonValue);
    }
  }

  public static void ensureGreaterThanOrEqual(
      final String property, final double testValue, final double comparisonValue) {
    if (testValue < comparisonValue) {
      throw new RuntimeException(property + " must be greater than or equal to " + comparisonValue);
    }
  }

  public static void ensureLessThan(
      final String property, final double testValue, final double comparisonValue) {
    if (testValue >= comparisonValue) {
      throw new RuntimeException(property + " must be less than " + comparisonValue);
    }
  }

  public static void ensureLessThanOrEqual(
      final String property, final double testValue, final double comparisonValue) {
    if (testValue > comparisonValue) {
      throw new RuntimeException(property + " must be less than or equal to " + comparisonValue);
    }
  }

  public static void ensureNotNullOrGreaterThan(
      final String property, final Duration testValue, final Duration comparisonValue) {
    ensureNotNull(property, testValue);

    if (testValue.compareTo(comparisonValue) <= 0) {
      throw new RuntimeException(property + " must be greater than " + comparisonValue);
    }
  }

  public static void ensureNotNullOrGreaterThanOrEqual(
      final String property, final Duration testValue, final Duration comparisonValue) {
    ensureNotNull(property, testValue);

    if (testValue.compareTo(comparisonValue) < 0) {
      throw new RuntimeException(property + " must be greater than or equal to " + comparisonValue);
    }
  }

  public static void ensureNotNullOrEmpty(final String property, final String testValue) {
    ensureNotNull(property, testValue);

    if (testValue.isEmpty()) {
      throw new RuntimeException(property + " must not be empty");
    }
  }

  public static void ensureNotNullOrEmpty(final String property, final byte[] testValue) {
    ensureNotNull(property, testValue);

    if (testValue.length == 0) {
      throw new RuntimeException(property + " must not be empty");
    }
  }

  public static void ensureNotNullOrEmpty(final String property, final DirectBuffer testValue) {
    ensureNotNull(property, testValue);

    if (testValue.capacity() == 0) {
      throw new RuntimeException(property + " must not be empty");
    }
  }

  public static void ensureAtLeastOneNotNull(final String property, final Object... values) {
    for (final Object value : values) {
      if (value != null) {
        return;
      }
    }

    throw new RuntimeException(property + " must have at least one non-null value");
  }

  public static void ensureFalse(final String property, final boolean value) {
    if (value) {
      throw new RuntimeException(property + " must be false");
    }
  }

  public static void ensureArrayBacked(final DirectBuffer buffer) {
    ensureNotNull("bytes array", buffer.byteArray());
  }

  public static void ensureArrayBacked(final DirectBuffer... buffers) {
    for (final DirectBuffer buffer : buffers) {
      ensureArrayBacked(buffer);
    }
  }
}
