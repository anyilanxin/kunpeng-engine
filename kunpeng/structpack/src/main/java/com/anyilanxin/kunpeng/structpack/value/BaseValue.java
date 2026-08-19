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

/**
 * 值载体基类：只管数据与 structpack 编解码，不管 key/默认值/isSet（那是 {@code BaseProperty} 的职责）。
 *
 * <p>所有实现遵循：预分配复用（{@link #reset()} 后可重读）、读路径零分配（字符串/二进制为零拷贝视图）。
 */
public abstract class BaseValue {

  public abstract void reset();

  public abstract void read(final PackerReader reader);

  public abstract void write(final PackerWriter writer);

  public abstract int getEncodedLength();

  public abstract void writeJSON(final StringBuilder builder);

  /** 调试/导出用字符串形式（复用 writeJSON，非热路径） */
  @Override
  public String toString() {
    final StringBuilder builder = new StringBuilder();
    writeJSON(builder);
    return builder.toString();
  }
}
