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
package com.anyilanxin.kunpeng.cluster.raft.orchestrator;

import com.anyilanxin.kunpeng.cluster.raft.RaftCommitListener;
import com.anyilanxin.kunpeng.cluster.raft.RaftRoleChangeListener;
import com.anyilanxin.kunpeng.cluster.raft.journal.util.health.FailureListener;
import com.anyilanxin.kunpeng.cluster.raft.partition.PartitionId;
import com.anyilanxin.kunpeng.cluster.raft.partition.RaftPartitionConfig;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotHandler;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * 按分区组类型注册的工厂（编排服务构造器传入 map，key 为分区组类型）。
 *
 * <p>分区级参数与监听器均由本工厂按 {@link PartitionId} 提供；其中
 * {@link #getRaftPartitionConfig()} 与 {@link #getSnapshotHandler(PartitionId)} 为
 * 分区启动必传项，启动时校验，未提供直接抛出异常。{@link Supplier} 形式使每次取用
 * 都可获得独立实例。
 */
public interface RaftGroupFactory {

  /** 分区配置（必传） */
  RaftPartitionConfig getRaftPartitionConfig();

  /** 前置启动步骤（Raft 分区集合启动之前执行） */
  <T extends RaftGroupContext> Supplier<List<PartitionStartup<T>>> getBeforeStartupSteps(T context);

  /** 后置启动步骤（Raft 分区集合启动之后执行） */
  <T extends RaftGroupContext> Supplier<List<PartitionStartup<T>>> getAfterStartupSteps(T context);

  /** 分区快照处理器（必传） */
  Supplier<SnapshotHandler> getSnapshotHandler(final PartitionId id);

  /** 分区角色变更监听器集合 */
  Supplier<Set<RaftRoleChangeListener>> getRoleChangeListeners(final PartitionId id);

  /** 分区故障监听器集合 */
  Supplier<Set<FailureListener>> getFailureListeners(final PartitionId id);

  /** 分区提交监听器集合 */
  Supplier<Set<RaftCommitListener>> getCommitListeners(final PartitionId id);
}
