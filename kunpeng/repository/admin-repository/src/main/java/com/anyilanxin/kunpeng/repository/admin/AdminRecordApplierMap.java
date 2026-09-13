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
package com.anyilanxin.kunpeng.repository.admin;

import com.anyilanxin.kunpeng.protocol.admin.AdminValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.admin.AdminValueType;
import com.anyilanxin.kunpeng.protocol.admin.record.RecordType;
import java.util.Iterator;

@SuppressWarnings({"rawtypes"})
public final class AdminRecordApplierMap {
  private final AdminApplier[] elements;

  private final int valueTypeCardinality;
  private final int intentCardinality;

  private final ValueIterator valueIt = new ValueIterator();

  public <R extends Enum<R>, S extends Enum<S>> AdminRecordApplierMap() {
    final int recordTypeCardinality = RecordType.class.getEnumConstants().length;
    valueTypeCardinality = AdminValueType.class.getEnumConstants().length;
    intentCardinality = AdminValueLifeCycle.maxCardinality();

    final int cardinality = recordTypeCardinality * valueTypeCardinality * intentCardinality;
    elements = new AdminApplier[cardinality];
  }

  public AdminApplier get(
      final RecordType recordType,
      final AdminValueType valueType,
      final AdminValueLifeCycle valueState) {
    final int index = mapToIndex(recordType, valueType, valueState);

    if (index >= 0) {
      return elements[index];
    } else {
      return null;
    }
  }

  public void put(
      final RecordType recordType,
      final AdminValueType valueType,
      final AdminValueLifeCycle valueState,
      final AdminApplier value) {
    if (!valueState.isState()) {
      throw new IllegalStateException(
          "Expected to register state life cycles only, but got non-state: " + valueState);
    }

    final int index = mapToIndex(recordType, valueType, valueState);

    if (index < 0) {
      throw new RuntimeException("Invalid intent value " + valueState.value());
    }

    final AdminApplier oldElement = elements[index];
    if (oldElement != null) {
      final String exceptionMsg =
          String.format(
              "Expected to have a single processor per intent,"
                  + " got for intent %s duplicate processor %s have already %s",
              AdminValueLifeCycle.fromProtocolValue(valueType, valueState.value()),
              value.getClass().getName(),
              oldElement.getClass().getName());
      throw new IllegalStateException(exceptionMsg);
    }

    elements[index] = value;
  }

  private int mapToIndex(
      final RecordType recordType,
      final AdminValueType valueType,
      final AdminValueLifeCycle valueState) {
    if (valueState.value() >= intentCardinality) {
      return -1;
    }

    return (recordType.ordinal() * valueTypeCardinality * intentCardinality)
        + (valueType.ordinal() * intentCardinality)
        + valueState.value();
  }

  /** BEWARE: does not detect concurrent modifications and behaves incorrectly in this case */
  public Iterator<AdminApplier> values() {
    valueIt.init();
    return valueIt;
  }

  private final class ValueIterator implements Iterator<AdminApplier> {
    private int next;

    private void scanToNext() {
      do {
        next++;
      } while (next < elements.length && elements[next] == null);
    }

    public void init() {
      next = -1;
      scanToNext();
    }

    @Override
    public boolean hasNext() {
      return next < elements.length;
    }

    @Override
    public AdminApplier next() {
      final AdminApplier element = elements[next];
      scanToNext();
      return element;
    }
  }
}
