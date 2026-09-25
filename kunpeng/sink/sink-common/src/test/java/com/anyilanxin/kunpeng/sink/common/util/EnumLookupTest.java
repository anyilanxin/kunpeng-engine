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
package com.anyilanxin.kunpeng.sink.common.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * 枚举查找工具测试。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
final class EnumLookupTest {

  private enum Status {
    IN_PROGRESS,
    COMPLETED
  }

  @Test
  void resolvesCommonSpellings() {
    assertThat(EnumLookup.resolve(Status.class, "IN_PROGRESS")).isEqualTo(Status.IN_PROGRESS);
    assertThat(EnumLookup.resolve(Status.class, "in-progress")).isEqualTo(Status.IN_PROGRESS);
    assertThat(EnumLookup.resolve(Status.class, "in progress")).isEqualTo(Status.IN_PROGRESS);
    assertThat(EnumLookup.resolve(Status.class, "completed")).isEqualTo(Status.COMPLETED);
  }

  @Test
  void rejectsUnknownInput() {
    assertThatThrownBy(() -> EnumLookup.resolve(Status.class, "nope"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("nope");
    assertThatThrownBy(() -> EnumLookup.resolve(Status.class, null))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void fallsBackOnUnknownInput() {
    assertThat(EnumLookup.resolveOrDefault(Status.class, "nope", Status.COMPLETED))
        .isEqualTo(Status.COMPLETED);
    assertThat(EnumLookup.resolveOrDefault(Status.class, null, Status.COMPLETED))
        .isEqualTo(Status.COMPLETED);
    assertThat(EnumLookup.resolveOrDefault(Status.class, "in_progress", Status.COMPLETED))
        .isEqualTo(Status.IN_PROGRESS);
  }

  @Test
  void mapsResolvedConstant() {
    final String mapped = EnumLookup.map(Status.class, "COMPLETED", value -> value.name());
    final String missing = EnumLookup.map(Status.class, "unknown", value -> value.name());

    assertThat(mapped).isEqualTo("COMPLETED");
    assertThat(missing).isNull();
  }
}
