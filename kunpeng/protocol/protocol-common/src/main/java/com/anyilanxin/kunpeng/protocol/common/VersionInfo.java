/*
 * Copyright © 2026 anyilanxin zxh (anyilanxin@aliyun.com)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.anyilanxin.kunpeng.protocol.common;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 版本信息：语义化版本解析与比较。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class VersionInfo {

  public static final VersionInfo UNKNOWN = new VersionInfo(0, 0, 0);

  private static final Pattern VERSION_PATTERN = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+).*");

  private final int majorVersion;
  private final int minorVersion;
  private final int patchVersion;

  public VersionInfo(final int majorVersion, final int minorVersion, final int patchVersion) {
    this.majorVersion = majorVersion;
    this.minorVersion = minorVersion;
    this.patchVersion = patchVersion;
  }

  public static VersionInfo parse(final String version) {
    final var matcher = VERSION_PATTERN.matcher(version);
    if (matcher.matches()) {
      final var major = Integer.parseInt(matcher.group(1));
      final var minor = Integer.parseInt(matcher.group(2));
      final var patch = Integer.parseInt(matcher.group(3));
      return new VersionInfo(major, minor, patch);

    } else {
      return UNKNOWN;
    }
  }

  public int getMajorVersion() {
    return majorVersion;
  }

  public int getMinorVersion() {
    return minorVersion;
  }

  public int getPatchVersion() {
    return patchVersion;
  }

  @Override
  public int hashCode() {
    return Objects.hash(majorVersion, minorVersion, patchVersion);
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final VersionInfo that = (VersionInfo) o;
    return majorVersion == that.majorVersion
        && minorVersion == that.minorVersion
        && patchVersion == that.patchVersion;
  }

  @Override
  public String toString() {
    return majorVersion + "." + minorVersion + "." + patchVersion;
  }
}
