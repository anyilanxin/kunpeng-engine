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
package com.anyilanxin.kunpeng.cluster.raft.utils;

import com.anyilanxin.kunpeng.cluster.cluster.MemberId;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

/**
 * 联合共识计票器：新旧成员集合<b>各自</b>过半才算成功（Raft 论文 §6）。
 *
 * <p>仅当成功票与新集合的交集达到新 quorum <b>且</b>与旧集合的交集达到旧 quorum 时
 * 回调 {@code succeeded=true}；任一集合多数失败即整体失败。
 */
public final class JointConsensusQuorum {

  private final Set<MemberId> newMembers;
  private final Set<MemberId> oldMembers;
  private final int newQuorum;
  private final int oldQuorum;
  private final Set<MemberId> succeeded = new HashSet<>();
  private final Consumer<Boolean> callback;
  private boolean complete;

  public JointConsensusQuorum(
      final Collection<MemberId> newMembers,
      final Collection<MemberId> oldMembers,
      final Consumer<Boolean> callback) {
    this.newMembers = new HashSet<>(newMembers);
    this.oldMembers = new HashSet<>(oldMembers);
    this.newQuorum = this.newMembers.size() / 2 + 1;
    this.oldQuorum = this.oldMembers.size() / 2 + 1;
    this.callback = callback;
  }

  /** 记录一票成功 */
  public JointConsensusQuorum succeed(final MemberId memberId) {
    succeeded.add(memberId);
    checkComplete();
    return this;
  }

  /** 记录一票失败（任一成员不可达即可导致失败判定，保守策略） */
  public JointConsensusQuorum fail() {
    if (!complete) {
      complete = true;
      callback.accept(false);
    }
    return this;
  }

  private void checkComplete() {
    if (complete) {
      return;
    }
    final int newCount = countIntersection(succeeded, newMembers);
    final int oldCount = countIntersection(succeeded, oldMembers);
    if (newCount >= newQuorum && oldCount >= oldQuorum) {
      complete = true;
      callback.accept(true);
    }
  }

  private static int countIntersection(final Set<MemberId> succeeded, final Set<MemberId> config) {
    int count = 0;
    for (final MemberId member : succeeded) {
      if (config.contains(member)) {
        count++;
      }
    }
    return count;
  }

  /** 是否已完成 */
  public boolean isComplete() {
    return complete;
  }
}
