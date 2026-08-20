/*
 * Copyright 2015-present Open Networking Foundation
 * Copyright © 2020 camunda services GmbH (info@camunda.com)
 * Copyright © 2026 anyilanxin zxh(anyilanxin@aliyun.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */
package com.anyilanxin.kunpeng.cluster.raft.storage.system;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.anyilanxin.kunpeng.cluster.raft.cluster.RaftMember;
import com.anyilanxin.kunpeng.cluster.utils.misc.TimestampPrinter;
import java.util.Collection;

/**
 * 持久化的集群配置。
 *
 * <p>联合共识（joint consensus）状态下 {@code oldMembers} 非空：此时共识提交与选举均需
 * 新旧两个成员集合各自过半（Raft 论文 §6），保证变更过程中任意时刻旧 quorum 与新 quorum
 * 始终有交集，防止脑裂。
 */
public class Configuration {

  private final long index;
  private final long term;
  private final long time;
  private final Collection<RaftMember> members;
  /** 联合共识阶段的旧成员集合；非联合态为 null */
  private final Collection<RaftMember> oldMembers;

  public Configuration(
      final long index, final long term, final long time, final Collection<RaftMember> members) {
    this(index, term, time, members, null);
  }

  public Configuration(
      final long index,
      final long term,
      final long time,
      final Collection<RaftMember> members,
      final Collection<RaftMember> oldMembers) {
    checkArgument(time > 0, "time must be positive");
    checkNotNull(members, "members cannot be null");
    this.index = index;
    this.term = term;
    this.time = time;
    this.members = members;
    this.oldMembers = oldMembers;
  }

  public long index() {
    return index;
  }

  public long term() {
    return term;
  }

  public long time() {
    return time;
  }

  /** 当前（新）成员集合 */
  public Collection<RaftMember> members() {
    return members;
  }

  /** 联合共识阶段的旧成员集合；非联合态返回 {@code null} */
    public Collection<RaftMember> oldMembers() {
    return oldMembers;
  }

  /** 是否处于联合共识阶段 */
  public boolean requiresJointConsensus() {
    return oldMembers != null;
  }

  @Override
  public String toString() {
    return toStringHelper(this)
        .add("index", index)
        .add("time", new TimestampPrinter(time))
        .add("members", members)
        .add("oldMembers", oldMembers)
        .toString();
  }
}
