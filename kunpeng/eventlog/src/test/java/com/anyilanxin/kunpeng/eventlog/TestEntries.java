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
package com.anyilanxin.kunpeng.eventlog;

import com.anyilanxin.kunpeng.structpack.buffer.BufferWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.agrona.concurrent.UnsafeBuffer;

/** 测试条目工厂（跨测试包共享） */
public final class TestEntries {

  private TestEntries() {}

  public static BufferWriter bytes(final String text) {
    final byte[] array = text.getBytes(StandardCharsets.UTF_8);
    return new com.anyilanxin.kunpeng.structpack.buffer.DirectBufferWriter()
        .wrap(new UnsafeBuffer(array), 0, array.length);
  }

  public static AppendEntry entry(final long key, final int sourceIndex, final boolean skip,
      final String metadata, final String value) {
    final AppendEntry base = AppendEntry.of(key, bytes(metadata), bytes(value), sourceIndex);
    return skip ? AppendEntry.skipped(base) : base;
  }

  public static AppendEntry simple() {
    return entry(-1, -1, false, "meta", "value");
  }

  public static List<AppendEntry> entriesOfSize(final int n) {
    final List<AppendEntry> entries = new ArrayList<>(n);
    for (int i = 0; i < n; i++) {
      entries.add(entry(i, -1, false, "m" + i, "v" + i));
    }
    return entries;
  }
}
