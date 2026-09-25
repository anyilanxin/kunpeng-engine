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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.kvstore;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * 预定义列族的编号、迁移语义与名称反查测试。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
class PredefinedColumnFamilyTest {

  @Test
  void shouldDefineOrderedFamiliesWithTransferFlags() {
    assertThat(PredefinedColumnFamily.DEFAULT_COLUMN_FAMILY.getFamily()).isZero();
    assertThat(PredefinedColumnFamily.DEFAULT_COLUMN_FAMILY.isEnableTransfer()).isFalse();

    assertThat(PredefinedColumnFamily.LOCAL_COLUMN_FAMILY.getFamily()).isEqualTo(1);
    assertThat(PredefinedColumnFamily.LOCAL_COLUMN_FAMILY.isEnableTransfer()).isTrue();

    assertThat(PredefinedColumnFamily.GLOBAL_COLUMN_FAMILY.getFamily()).isEqualTo(2);
    assertThat(PredefinedColumnFamily.GLOBAL_COLUMN_FAMILY.isEnableTransfer()).isTrue();
  }

  @Test
  void shouldResolveFamilyFromItsName() {
    for (final var family : PredefinedColumnFamily.values()) {
      assertThat(PredefinedColumnFamily.fromColumnFamilyName(family.getColumnFamilyName()))
          .isEqualTo(family);
    }
  }

  @Test
  void shouldRejectUnknownFamilyName() {
    assertThatThrownBy(
            () -> PredefinedColumnFamily.fromColumnFamilyName("unknown".getBytes(UTF_8)))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void copyTypeShouldCoverFamilyAndVirtualFamily() {
    assertThat(ColumnCopyType.values()).containsExactlyInAnyOrder(ColumnCopyType.FAMILY, ColumnCopyType.VIRTUAL_FAMILY);
  }
}
