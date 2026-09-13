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
package com.anyilanxin.kunpeng.cluster.dispatch.distributor.fixed;

import com.anyilanxin.kunpeng.cluster.cluster.MemberId;
import java.util.Objects;

/** 固定分区分配中的成员，包含成员 ID 与优先级。 */
final class FixedDistributionMember {
  private final MemberId id;
  private final int priority;

  FixedDistributionMember(final MemberId id, final int priority) {
    this.id = id;
    this.priority = priority;
  }

  MemberId getId() {
    return id;
  }

  int getPriority() {
    return priority;
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }

    if (!(o instanceof final FixedDistributionMember member)) {
      return false;
    }

    return id.equals(member.id);
  }

  @Override
  public String toString() {
    return "FixedDistributionMember{" + "id=" + id + ", priority=" + priority + '}';
  }
}
