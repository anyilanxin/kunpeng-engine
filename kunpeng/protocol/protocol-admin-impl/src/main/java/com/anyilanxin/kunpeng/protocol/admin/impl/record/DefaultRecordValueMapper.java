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
package com.anyilanxin.kunpeng.protocol.admin.impl.record;

import com.anyilanxin.kunpeng.protocol.admin.AdminRecordMappingIndex;
import com.anyilanxin.kunpeng.protocol.admin.AdminValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.command.CommandRecordRegister;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.commandapi.CommandApiRecordRegister;
import com.anyilanxin.kunpeng.protocol.admin.record.RecordValueMapper;
import com.anyilanxin.kunpeng.protocol.admin.record.RecordValueMapperRegister;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import java.util.function.Supplier;
import org.agrona.concurrent.UnsafeBuffer;

/**
 * @author zxuanhong
 * @since
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class DefaultRecordValueMapper<RECORD extends UnifiedRecordValue>
    implements RecordValueMapper<RECORD>, RecordValueMapperRegister<RECORD> {
  private final Supplier<RECORD>[] valueSuppliers;
  private final RECORD[] valueCache;

  public static RecordValueMapper getInstance() {
    return new DefaultRecordValueMapper();
  }

  private DefaultRecordValueMapper() {
    valueSuppliers = new Supplier[AdminRecordMappingIndex.size()];
    valueCache = (RECORD[]) new UnifiedRecordValue[AdminRecordMappingIndex.size()];
    registerInit();
  }

  @Override
  public RecordValueMapperRegister register(
      final AdminValueLifeCycle lifeCycle, final Supplier<RECORD> recordSupplier) {
    if (valueSuppliers[lifeCycle.recordIndex()] != null) {
      throw new IllegalStateException(
          "AdminRecord value mapper has already been registered:"
              + valueSuppliers[lifeCycle.recordIndex()].getClass().getName());
    }
    valueSuppliers[lifeCycle.recordIndex()] = recordSupplier;
    valueCache[lifeCycle.recordIndex()] = recordSupplier.get();
    return this;
  }

  private void registerInit() {
    CommandApiRecordRegister.register(this);
    CommandRecordRegister.register(this);
  }

  @Override
  public RECORD getValue(final AdminValueLifeCycle lifeCycle) {
    final Supplier<RECORD> valueSupplier = valueSuppliers[lifeCycle.recordIndex()];
    if (valueSupplier == null) {
      throw new IllegalStateException(
          "No record value supplier registered for value life cycle " + lifeCycle.name());
    }
    return valueSupplier.get();
  }

  @Override
  public RECORD getCacheValue(final AdminValueLifeCycle lifeCycle) {
    final RECORD record = valueCache[lifeCycle.recordIndex()];
    record.reset();
    return record;
  }

  @Override
  public RECORD copyValue(final AdminValueLifeCycle lifeCycle, final RECORD value) {
    final RECORD copyRecord = getValue(lifeCycle);
    final var valueBuffer = new UnsafeBuffer(new byte[value.getLength()]);
    value.write(valueBuffer, 0);
    copyRecord.wrap(valueBuffer, 0, valueBuffer.capacity());
    return copyRecord;
  }

  @Override
  public RECORD copyCacheValue(final AdminValueLifeCycle lifeCycle, final RECORD value) {
    final RECORD copyRecord = getCacheValue(lifeCycle);
    final var valueBuffer = new UnsafeBuffer(new byte[value.getLength()]);
    value.write(valueBuffer, 0);
    copyRecord.wrap(valueBuffer, 0, valueBuffer.capacity());
    return copyRecord;
  }
}
