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
package com.anyilanxin.kunpeng.cluster.raft.journal.record;

import java.util.Objects;

/** 记录头（元数据）部分，承载校验和与记录体长度。 */
public final class RecordMetadata {

  /** 记录体数据的 CRC32C 校验和。 */
  private final long checksum;

  /** 记录体数据的字节长度。 */
  private final int length;

  public RecordMetadata(final long checksum, final int length) {
    this.checksum = checksum;
    this.length = length;
  }

  /** 记录体数据的 CRC32C 校验和。 */
  public long checksum() {
    return checksum;
  }

  /** 记录体数据的字节长度。 */
  public int length() {
    return length;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof RecordMetadata)) {
      return false;
    }
    final RecordMetadata that = (RecordMetadata) o;
    return checksum == that.checksum && length == that.length;
  }

  @Override
  public int hashCode() {
    return Objects.hash(checksum, length);
  }

  @Override
  public String toString() {
    return "RecordMetadata{checksum=" + checksum + ", length=" + length + '}';
  }
}
