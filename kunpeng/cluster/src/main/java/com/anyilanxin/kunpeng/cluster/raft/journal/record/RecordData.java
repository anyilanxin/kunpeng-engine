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
import org.agrona.DirectBuffer;

/** 记录体（数据）部分，承载索引、应用层序号与负载数据。 */
public final class RecordData {

  /** 记录在日志中的全局索引。 */
  private final long index;

  /** 记录的应用层序号（application sequence number）。 */
  private final long asqn;

  /** 记录负载数据。 */
  private final DirectBuffer data;

  public RecordData(final long index, final long asqn, final DirectBuffer data) {
    this.index = index;
    this.asqn = asqn;
    this.data = data;
  }

  /** 记录在日志中的全局索引。 */
  public long index() {
    return index;
  }

  /** 记录的应用层序号（application sequence number）。 */
  public long asqn() {
    return asqn;
  }

  /** 记录负载数据。 */
  public DirectBuffer data() {
    return data;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof RecordData)) {
      return false;
    }
    final RecordData that = (RecordData) o;
    return index == that.index && asqn == that.asqn && Objects.equals(data, that.data);
  }

  @Override
  public int hashCode() {
    return Objects.hash(index, asqn, data);
  }

  @Override
  public String toString() {
    return "RecordData{index=" + index + ", asqn=" + asqn + ", data=" + data + '}';
  }
}
