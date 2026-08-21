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
package com.anyilanxin.kunpeng.utils;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 不可变的语义化版本号(Semantic Versioning)实现。
 *
 * <p>版本号格式: {@code 主版本.次版本.修订号[-先行版本][+构建元数据]}，例如 {@code 8.0.4-rc.1+build.20260819}。 完整规范参见 <a
 * href="https://semver.org/">Semantic Versioning</a>。
 *
 * <p>比较与相等性遵循 SemVer 优先级规则：
 *
 * <ul>
 *   <li>按主版本、次版本、修订号依次数值比较；
 *   <li>不含先行版本的版本优先级高于含先行版本的版本(如 {@code 1.0.0} 高于 {@code 1.0.0-rc.1})；
 *   <li>先行版本按点分隔的标识符逐个比较：纯数字标识符按数值比较且低于字母数字标识符， 字母数字标识符按 ASCII 字典序比较；标识符数量多者优先级高；
 *   <li>构建元数据不参与优先级比较，{@link #equals(Object)} 与 {@link #hashCode()} 同样忽略它。
 * </ul>
 */
public final class Version implements Comparable<Version> {

  private static final Pattern VERSION_PATTERN =
      Pattern.compile(
          "^(\\d+)\\.(\\d+)\\.(\\d+)"
              + "(?:-([0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*))?"
              + "(?:\\+([0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*))?$");

  private final int major;
  private final int minor;
  private final int patch;
  private final String preRelease;
  private final String buildMetadata;

  private Version(
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

  /** 主版本号。 */
  public int major() {
    return major;
  }

  /** 次版本号。 */
  public int minor() {
    return minor;
  }

  /** 修订号。 */
  public int patch() {
    return patch;
  }

  /** 先行版本号，如 {@code 8.0.4-rc.1} 中的 {@code rc.1}，可能为 {@code null}。 */
  public String preRelease() {
    return preRelease;
  }

  /** 构建元数据，如 {@code 8.0.4+build.42} 中的 {@code build.42}，可能为 {@code null}。 */
  public String buildMetadata() {
    return buildMetadata;
  }

  /**
   * 解析版本号字符串。
   *
   * @param version 版本号字符串，如 {@code 1.2.3}、{@code 1.2.3-rc.1}、{@code 1.2.3-rc.1+build.5}
   * @return 版本对象
   * @throws IllegalArgumentException 版本号不符合 SemVer 格式时抛出
   */
  public static Version from(final String version) {
    final Matcher matcher = VERSION_PATTERN.matcher(version);
    if (!matcher.matches()) {
      throw new IllegalArgumentException(
          "版本号格式不合法，期望格式为 major.minor.patch[-先行版本][+构建元数据]，实际为 [%s]".formatted(version));
    }
    return new Version(
        Integer.parseInt(matcher.group(1)),
        Integer.parseInt(matcher.group(2)),
        Integer.parseInt(matcher.group(3)),
        matcher.group(4),
        matcher.group(5));
  }

  /** 按 SemVer 优先级规则比较，构建元数据不参与比较。 */
  @Override
  public int compareTo(final Version that) {
    int result = Integer.compare(major, that.major);
    if (result != 0) {
      return result;
    }
    result = Integer.compare(minor, that.minor);
    if (result != 0) {
      return result;
    }
    result = Integer.compare(patch, that.patch);
    if (result != 0) {
      return result;
    }
    if (preRelease == null && that.preRelease == null) {
      return 0;
    }
    if (preRelease == null) {
      return 1;
    }
    if (that.preRelease == null) {
      return -1;
    }
    return comparePreRelease(preRelease, that.preRelease);
  }

  private static int comparePreRelease(final String left, final String right) {
    final String[] leftIdentifiers = left.split("\\.");
    final String[] rightIdentifiers = right.split("\\.");
    final int length = Math.min(leftIdentifiers.length, rightIdentifiers.length);
    for (int i = 0; i < length; i++) {
      final int result = compareIdentifier(leftIdentifiers[i], rightIdentifiers[i]);
      if (result != 0) {
        return result;
      }
    }
    return Integer.compare(leftIdentifiers.length, rightIdentifiers.length);
  }

  private static int compareIdentifier(final String left, final String right) {
    final boolean leftNumeric = isNumeric(left);
    final boolean rightNumeric = isNumeric(right);
    if (leftNumeric && rightNumeric) {
      final int result = Integer.compare(left.length(), right.length());
      return result != 0 ? result : left.compareTo(right);
    }
    if (leftNumeric) {
      return -1;
    }
    if (rightNumeric) {
      return 1;
    }
    return left.compareTo(right);
  }

  private static boolean isNumeric(final String identifier) {
    for (int i = 0; i < identifier.length(); i++) {
      final char c = identifier.charAt(i);
      if (c < '0' || c > '9') {
        return false;
      }
    }
    return true;
  }

  /** 构建元数据不参与相等性判断，与 {@link #compareTo(Version)} 语义保持一致。 */
  @Override
  public boolean equals(final Object object) {
    if (this == object) {
      return true;
    }
    if (!(object instanceof Version)) {
      return false;
    }
    final Version that = (Version) object;
    return major == that.major
        && minor == that.minor
        && patch == that.patch
        && Objects.equals(preRelease, that.preRelease);
  }

  @Override
  public int hashCode() {
    return Objects.hash(major, minor, patch, preRelease);
  }

  @Override
  public String toString() {
    final StringBuilder builder =
        new StringBuilder().append(major).append('.').append(minor).append('.').append(patch);
    if (preRelease != null) {
      builder.append('-').append(preRelease);
    }
    if (buildMetadata != null) {
      builder.append('+').append(buildMetadata);
    }
    return builder.toString();
  }
}
