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

/** double 值：8 字节小端定宽 */
public class DoubleValue extends BaseValue {

  private double value;

  public DoubleValue() {}

  public DoubleValue(final double initialValue) {
    this.value = initialValue;
  }

  public void setValue(final double val) {
    this.value = val;
  }

  public double getValue() {
    return value;
  }

  @Override
  public void reset() {
    value = 0;
  }

  @Override
  public void read(final PackerReader reader) {
    value = reader.readDouble();
  }

  @Override
  public void write(final PackerWriter writer) {
    writer.writeDouble(value);
  }

  @Override
  public int getEncodedLength() {
    return Double.BYTES;
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
    if (!(o instanceof final DoubleValue that)) {
      return false;
    }
    return Double.doubleToLongBits(value) == Double.doubleToLongBits(that.value);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(value);
  }
}
