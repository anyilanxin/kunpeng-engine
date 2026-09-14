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
package com.anyilanxin.kunpeng.repository.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.anyilanxin.kunpeng.protocol.admin.AdminValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.admin.AdminValueType;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.delayed.DelayedRecord;
import com.anyilanxin.kunpeng.protocol.admin.record.RecordType;
import com.anyilanxin.kunpeng.protocol.admin.record.command.delayed.DelayedLifeCycle;
import com.anyilanxin.kunpeng.protocol.common.RecordValue;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** AdminRecordApplierMap 注册表行为测试 */
class AdminRecordApplierMapTest {

  private final AdminRecordApplierMap map = new AdminRecordApplierMap();

  private static AdminApplier<DelayedRecord> applier() {
    return new AdminApplier<>() {

      @Override
      public void applyState(final long key, final DelayedRecord recordValue) {}

      @Override
      public AdminValueType valueType() {
        return AdminValueType.DELAYED;
      }

      @Override
      public AdminValueLifeCycle lifeCycle() {
        return DelayedLifeCycle.CREATED;
      }
    };
  }

  @Test
  void shouldRegisterAndLookupApplier() {
    final var applier = applier();
    map.put(RecordType.EVENT, AdminValueType.DELAYED, DelayedLifeCycle.CREATED, applier);

    assertThat(map.get(RecordType.EVENT, AdminValueType.DELAYED, DelayedLifeCycle.CREATED))
        .isSameAs(applier);
    // 其它 record type / life cycle 组合不受影响
    assertThat(map.get(RecordType.COMMAND, AdminValueType.DELAYED, DelayedLifeCycle.CREATED))
        .isNull();
    assertThat(map.get(RecordType.EVENT, AdminValueType.DELAYED, DelayedLifeCycle.CANCELED))
        .isNull();
  }

  @Test
  void shouldRejectDuplicateRegistration() {
    map.put(RecordType.EVENT, AdminValueType.DELAYED, DelayedLifeCycle.CREATED, applier());
    assertThatThrownBy(
            () -> map.put(RecordType.EVENT, AdminValueType.DELAYED, DelayedLifeCycle.CREATED, applier()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("duplicate processor");
  }

  @Test
  void shouldRejectNonStateLifeCycle() {
    assertThatThrownBy(
            () ->
                map.put(
                    RecordType.EVENT, AdminValueType.DELAYED, AdminValueLifeCycle.UNKNOWN, applier()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("non-state");
  }

  @Test
  void valuesIteratorShouldSkipEmptySlots() {
    final var applier = applier();
    map.put(RecordType.EVENT, AdminValueType.DELAYED, DelayedLifeCycle.CREATED, applier);

    final List<AdminApplier<? extends RecordValue>> values = new ArrayList<>();
    map.values().forEachRemaining(values::add);

    assertThat(values).containsExactly(applier);
  }

  @Test
  void valuesIteratorShouldBeReusableAfterInit() {
    final var applier = applier();
    map.put(RecordType.EVENT, AdminValueType.DELAYED, DelayedLifeCycle.CREATED, applier);

    final var first = new ArrayList<>();
    map.values().forEachRemaining(first::add);
    final var second = new ArrayList<>();
    map.values().forEachRemaining(second::add);

    assertThat(first).isEqualTo(second);
  }
}
