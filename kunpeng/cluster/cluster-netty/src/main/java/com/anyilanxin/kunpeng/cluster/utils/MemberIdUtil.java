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
package com.anyilanxin.kunpeng.cluster.utils;

import org.jspecify.annotations.Nullable;

/** Helpers to validate zone-aware member identifiers of the form {@code zone@suffix}. */
public final class MemberIdUtil {

  private MemberIdUtil() {}

  /**
   * Validates a zone name: it must not be empty and must not contain an '{@code @}', as it is
   * reserved as the separator between zone and node suffix.
   *
   * @param zone the zone name to validate; may be {@code null}
   * @return the validated zone
   */
  public static @Nullable String validateZone(final @Nullable String zone) {
    if (zone == null) {
      return null;
    }
    if (zone.isEmpty() || zone.indexOf('@') >= 0) {
      throw new IllegalArgumentException(
          "Expected zone to be a non-empty string without '@', but got [" + zone + "]");
    }
    return zone;
  }
}
