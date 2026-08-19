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
package com.anyilanxin.kunpeng.structpack.value;

import com.anyilanxin.kunpeng.structpack.PackerReader;
import com.anyilanxin.kunpeng.structpack.PackerWriter;

/** boolean 值：单字节 */
public class BooleanValue extends BaseValue {

  private boolean value;

  public BooleanValue() {}

  public BooleanValue(final boolean initialValue) {
    this.value = initialValue;
  }

  public boolean getValue() {
    return value;
  }

  public void setValue(final boolean value) {
    this.value = value;
  }

  @Override
  public void reset() {
    value = false;
  }

  @Override
  public void read(final PackerReader reader) {
    value = reader.readByte() != 0;
  }

  @Override
  public void write(final PackerWriter writer) {
    writer.writeByte(value ? 1 : 0);
  }

  @Override
  public int getEncodedLength() {
    return 1;
  }

  @Override
  public void writeJSON(final StringBuilder builder) {
    builder.append(value);
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof final BooleanValue that)) {
      return false;
    }
    return value == that.value;
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(value);
  }
}
