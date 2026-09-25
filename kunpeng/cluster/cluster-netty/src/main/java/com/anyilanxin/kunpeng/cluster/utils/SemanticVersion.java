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

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Immutable semantic version as defined by <a href="https://semver.org">semver.org</a>, with
 * optional pre-release and build metadata segments.
 */
public final class SemanticVersion implements Comparable<SemanticVersion> {

  private static final Pattern VERSION_PATTERN =
      Pattern.compile(
          "^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)(?:-((?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\\.(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\\+([0-9a-zA-Z-]+(?:\\.[0-9a-zA-Z-]+)*))?$");

  private final int major;
  private final int minor;
  private final int patch;
  private final String preRelease;
  private final String buildMetadata;

  public SemanticVersion(
      final int major,
      final int minor,
      final int patch,
      final String preRelease,
      final String buildMetadata) {
    this.major = major;
    this.minor = minor;
    this.patch = patch;
    this.preRelease = preRelease;
    this.buildMetadata = buildMetadata;
  }

  /** Parses a semantic version string; build metadata is ignored for comparison. */
  public static Optional<SemanticVersion> parse(final String version) {
    final Matcher matcher = VERSION_PATTERN.matcher(version);
    if (!matcher.matches()) {
      return Optional.empty();
    }
    return Optional.of(
        new SemanticVersion(
            Integer.parseInt(matcher.group(1)),
            Integer.parseInt(matcher.group(2)),
            Integer.parseInt(matcher.group(3)),
            matcher.group(4),
            matcher.group(5)));
  }

  public int major() {
    return major;
  }

  public int minor() {
    return minor;
  }

  public int patch() {
    return patch;
  }

  public String preRelease() {
    return preRelease;
  }

  public String buildMetadata() {
    return buildMetadata;
  }

  @Override
  public int compareTo(final SemanticVersion other) {
    int result = Integer.compare(major, other.major);
    if (result != 0) {
      return result;
    }
    result = Integer.compare(minor, other.minor);
    if (result != 0) {
      return result;
    }
    result = Integer.compare(patch, other.patch);
    if (result != 0) {
      return result;
    }
    return comparePreRelease(preRelease, other.preRelease);
  }

  private static int comparePreRelease(final String left, final String right) {
    if (left == null && right == null) {
      return 0;
    }
    if (left == null) {
      return 1; // a version without pre-release outranks one with it
    }
    if (right == null) {
      return -1;
    }
    final String[] leftParts = left.split("\\.");
    final String[] rightParts = right.split("\\.");
    final int length = Math.min(leftParts.length, rightParts.length);
    for (var i = 0; i < length; i++) {
      final int partResult = compareIdentifiers(leftParts[i], rightParts[i]);
      if (partResult != 0) {
        return partResult;
      }
    }
    return Integer.compare(leftParts.length, rightParts.length);
  }

  private static int compareIdentifiers(final String left, final String right) {
    final var leftNumeric = isNumeric(left);
    final var rightNumeric = isNumeric(right);
    if (leftNumeric && rightNumeric) {
      return Long.compare(Long.parseLong(left), Long.parseLong(right));
    }
    if (leftNumeric) {
      return -1; // numeric identifiers sort below alphanumeric ones
    }
    if (rightNumeric) {
      return 1;
    }
    return left.compareTo(right);
  }

  private static boolean isNumeric(final String value) {
    return value.chars().allMatch(Character::isDigit);
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof final SemanticVersion that)) {
      return false;
    }
    return major == that.major
        && minor == that.minor
        && patch == that.patch
        && java.util.Objects.equals(preRelease, that.preRelease);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(major, minor, patch, preRelease);
  }

  @Override
  public String toString() {
    final var builder = new StringBuilder("%d.%d.%d".formatted(major, minor, patch));
    if (preRelease != null) {
      builder.append('-').append(preRelease);
    }
    if (buildMetadata != null) {
      builder.append('+').append(buildMetadata);
    }
    return builder.toString();
  }
}
