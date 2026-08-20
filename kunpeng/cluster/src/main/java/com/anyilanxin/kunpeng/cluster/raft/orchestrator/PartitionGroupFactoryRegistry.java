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
package com.anyilanxin.kunpeng.cluster.raft.orchestrator;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** 分区组类型工厂注册中心（按分区组类型字符串标识注册） */
public final class PartitionGroupFactoryRegistry {

  private final Map<String, PartitionGroupTypeFactory<? extends RaftGroupContext>> registry =
      new ConcurrentHashMap<>();

  /** 注册某个分区组类型的工厂集合 */
  public <T extends RaftGroupContext> void register(
      final String groupType, final PartitionGroupTypeFactory<T> factory) {
    registry.put(groupType, factory);
  }

  /** 获取某个分区组类型的工厂集合 */
  @SuppressWarnings("unchecked")
  public <T extends RaftGroupContext> Optional<PartitionGroupTypeFactory<T>> get(
      final String groupType) {
    return Optional.ofNullable((PartitionGroupTypeFactory<T>) registry.get(groupType));
  }

  /** 已注册的分区组类型 */
  public java.util.Set<String> registeredTypes() {
    return java.util.Set.copyOf(registry.keySet());
  }
}
