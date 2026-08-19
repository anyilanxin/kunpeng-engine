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
package com.anyilanxin.kunpeng.cluster.raft.journal.file;

import com.anyilanxin.kunpeng.cluster.raft.journal.JournalRecord;
import org.agrona.DirectBuffer;

import java.util.Objects;

public class TestJournalRecord implements JournalRecord {

  private final long index;
  private final long asqn;
  private final long checksum;
  private final DirectBuffer data;

  public TestJournalRecord(
    final long index, final long asqn, final long checksum, final DirectBuffer data) {
    this.index = index;
    this.asqn = asqn;
    this.checksum = checksum;
    this.data = data;
  }

  @Override
  public long index() {
    return index;
  }

  @Override
  public long asqn() {
    return asqn;
  }

  @Override
  public long checksum() {
    return checksum;
  }

  @Override
  public DirectBuffer data() {
    return data;
  }

  @Override
  public int hashCode() {
    return Objects.hash(index, asqn, checksum, data);
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final JournalRecord that = (JournalRecord) o;
    return index == that.index()
      && asqn == that.asqn()
      && checksum == that.checksum()
      && Objects.equals(data, that.data());
  }
}
