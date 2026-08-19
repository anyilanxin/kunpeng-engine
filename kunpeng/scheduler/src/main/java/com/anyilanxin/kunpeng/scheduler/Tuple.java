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
package com.anyilanxin.kunpeng.scheduler;

/** 可变二元组 */
public class Tuple<L, R> {
  private L left;
  private R right;

  public Tuple(final L left, final R right) {
    this.right = right;
    this.left = left;
  }

  public R getRight() {
    return right;
  }

  public L getLeft() {
    return left;
  }

  public void setRight(final R right) {
    this.right = right;
  }

  public void setLeft(final L left) {
    this.left = left;
  }

  @Override
  public String toString() {
    return "<" + left + ", " + right + ">";
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof final Tuple<?, ?> other)) {
      return false;
    }
    return java.util.Objects.equals(left, other.left)
        && java.util.Objects.equals(right, other.right);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(left, right);
  }
}
