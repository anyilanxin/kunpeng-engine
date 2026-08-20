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
 * limitations under the License.
 */
package com.anyilanxin.kunpeng.cluster.raft.storage.log.entry;

import static com.google.common.base.MoreObjects.toStringHelper;

import com.anyilanxin.kunpeng.cluster.raft.cluster.RaftMember;
import com.anyilanxin.kunpeng.cluster.utils.misc.TimestampPrinter;
import java.util.Collection;
import java.util.Objects;

/**
 * 集群配置日志条目。
 *
 * <p>联合共识（joint consensus）条目同时携带新成员集合（{@code members}）与旧成员集合
 * （{@code oldMembers}），提交需两集合各自过半；第二阶段条目仅含新集合（{@code oldMembers}
 * 为 null），提交仅需新集合过半。
 */
public class ConfigurationEntry implements RaftEntry {

  protected final Collection<RaftMember> members;
  private final Collection<RaftMember> oldMembers;
  private final long timestamp;

  public ConfigurationEntry(final long timestamp, final Collection<RaftMember> members) {
    this(timestamp, members, null);
  }

  public ConfigurationEntry(
      final long timestamp,
      final Collection<RaftMember> members,
      final Collection<RaftMember> oldMembers) {
    this.members = members;
    this.oldMembers = oldMembers;
    this.timestamp = timestamp;
  }

  public long timestamp() {
    return timestamp;
  }

  /** 新成员集合 */
  public Collection<RaftMember> members() {
    return members;
  }

  /** 联合共识阶段的旧成员集合；非联合态返回 {@code null} */
    public Collection<RaftMember> oldMembers() {
    return oldMembers;
  }

  /** 是否为联合共识条目（第一阶段） */
  public boolean isJointConsensus() {
    return oldMembers != null;
  }

  @Override
  public String toString() {
    return toStringHelper(this)
        .add("timestamp", new TimestampPrinter(timestamp))
        .add("members", members)
        .add("oldMembers", oldMembers)
        .toString();
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final ConfigurationEntry that = (ConfigurationEntry) o;
    return timestamp == that.timestamp
        && Objects.equals(members, that.members)
        && Objects.equals(oldMembers, that.oldMembers);
  }

  @Override
  public int hashCode() {
    return Objects.hash(members, oldMembers, timestamp);
  }
}
