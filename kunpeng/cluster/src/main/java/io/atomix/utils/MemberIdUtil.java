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

import org.jspecify.annotations.Nullable;

/** Helpers to build and validate zone-aware member identifiers of the form {@code zone_nodeIdx}. */
public final class MemberIdUtil {

  private MemberIdUtil() {}

  /**
   * Builds the string representation of a member id.
   *
   * @param zone optional zone name; may be {@code null} for the bare form
   * @param nodeIdx the node index, must be >= 0
   * @return {@code "zone_nodeIdx"} or {@code "nodeIdx"} when the zone is {@code null}
   */
  public static String memberIdString(final @Nullable String zone, final int nodeIdx) {
    validateZone(zone);
    if (nodeIdx < 0) {
      throw new IllegalArgumentException("Expected nodeIdx to be >= 0, but got " + nodeIdx);
    }
    return zone == null ? Integer.toString(nodeIdx) : zone + "_" + nodeIdx;
  }

  /**
   * Validates a zone name: it must not be empty and must not contain an underscore, as the
   * underscore is reserved as the separator between zone and node index.
   *
   * @param zone the zone name to validate; may be {@code null}
   * @return the validated zone
   */
  public static @Nullable String validateZone(final @Nullable String zone) {
    if (zone == null) {
      return null;
    }
    if (zone.isEmpty() || zone.indexOf('_') >= 0) {
      throw new IllegalArgumentException(
          "Expected zone to be a non-empty string without underscores, but got [" + zone + "]");
    }
    return zone;
  }
}
