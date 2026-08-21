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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.Test;

/** Version test. */
public class VersionTest {

  @Test
  public void shouldAllowAlphaReleaseCandidates() {
    // given
    final var version = "8.6.0-alpha1-rc1";

    // when
    final Version from = Version.from(version);

    // then
    assertThat(from.major()).isEqualTo(8);
    assertThat(from.minor()).isEqualTo(6);
    assertThat(from.patch()).isEqualTo(0);
    assertThat(from.preRelease()).isEqualTo("alpha1-rc1");
    assertThat(from.buildMetadata()).isNull();
  }

  @Test
  public void testVersionComparison() {
    assertThat(Version.from("1.0.0")).isLessThan(Version.from("2.0.0"));
    assertThat(Version.from("2.0.0")).isGreaterThan(Version.from("1.0.0"));
    assertThat(Version.from("1.0.0")).isGreaterThan(Version.from("0.1.0"));
    assertThat(Version.from("0.1.0"))
        .isLessThan(Version.from("1.0.0"))
        .isLessThan(Version.from("0.1.1"));
    assertThat(Version.from("1.0.0")).isGreaterThan(Version.from("0.0.1"));
    assertThat(Version.from("1.1.1")).isGreaterThan(Version.from("1.0.3"));
    assertThat(Version.from("1.0.0")).isGreaterThan(Version.from("1.0.0-beta1"));
    assertThat(Version.from("1.0.0-rc2")).isGreaterThan(Version.from("1.0.0-rc1"));
    assertThat(Version.from("1.0.0-rc2.1")).isGreaterThan(Version.from("1.0.0-rc2"));
    assertThat(Version.from("1.0.0-rc2.1.1")).isGreaterThan(Version.from("1.0.0-rc2.1"));
    assertThat(Version.from("1.0.0-rc2")).isLessThan(Version.from("1.0.0-rc2.1"));
    assertThat(Version.from("1.0.0-rc1")).isGreaterThan(Version.from("1.0.0-beta1"));
    assertThat(Version.from("2.0.0-beta1")).isGreaterThan(Version.from("1.0.0"));
    assertThat(Version.from("1.0.0-alpha1")).isGreaterThan(Version.from("1.0.0-SNAPSHOT"));
    assertThat(Version.from("1.0.0-alpha1-rc1")).isLessThan(Version.from("1.0.0-alpha1-rc2"));
  }

  @Test
  public void testVersionToString() {
    assertThat(Version.from("1.0.0")).hasToString("1.0.0");
    assertThat(Version.from("1.0.0-alpha1")).hasToString("1.0.0-alpha1");
    assertThat(Version.from("1.0.0-beta1")).hasToString("1.0.0-beta1");
    assertThat(Version.from("1.0.0-rc1")).hasToString("1.0.0-rc1");
    assertThat(Version.from("1.0.0-rc1.2")).hasToString("1.0.0-rc1.2");
    assertThat(Version.from("1.0.0-SNAPSHOT")).hasToString("1.0.0-SNAPSHOT");
  }

  @Test
  public void testInvalidVersions() {
    assertThatThrownBy(() -> Version.from("1")).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> Version.from("1.0")).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> Version.from("1.0-beta1"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> Version.from("1.0.0.0")).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> Version.from("1.0.0.0-beta1"))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
