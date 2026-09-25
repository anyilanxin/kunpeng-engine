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
package com.anyilanxin.kunpeng.cluster.raft.primitive;

import com.anyilanxin.kunpeng.cluster.cluster.MemberId;
import com.anyilanxin.kunpeng.cluster.raft.cluster.RaftMember;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/** Test member. */
public class TestMember implements RaftMember {

  private final MemberId memberId;
  private final Type type;

  public TestMember(final MemberId memberId, final Type type) {
    this.memberId = memberId;
    this.type = type;
  }

  @Override
  public MemberId memberId() {
    return memberId;
  }

  @Override
  public int hash() {
    return 0;
  }

  @Override
  public void addTypeChangeListener(final Consumer<Type> listener) {}

  @Override
  public CompletableFuture<Void> promote() {
    return null;
  }

  @Override
  public CompletableFuture<Void> promote(final Type type) {
    return null;
  }

  @Override
  public CompletableFuture<Void> demote() {
    return null;
  }

  @Override
  public CompletableFuture<Void> demote(final Type type) {
    return null;
  }

  @Override
  public CompletableFuture<Void> remove() {
    return null;
  }

  @Override
  public Instant getLastUpdated() {
    return null;
  }

  @Override
  public Type getType() {
    return type;
  }
}
