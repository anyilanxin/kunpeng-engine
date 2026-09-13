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
package com.anyilanxin.kunpeng.cluster.business.step;

import com.anyilanxin.kunpeng.cluster.business.PartitionStartupContext;
import com.anyilanxin.kunpeng.cluster.cluster.PartitionId;
import com.anyilanxin.kunpeng.cluster.raft.PartitionTopologyListener;
import com.anyilanxin.kunpeng.cluster.raft.RaftRoleChangeListener;
import com.anyilanxin.kunpeng.cluster.raft.partition.RaftPartition;
import com.anyilanxin.kunpeng.scheduler.future.ActorFuture;
import com.anyilanxin.kunpeng.scheduler.startup.StartupStep;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 业务分区启动流程中的引导（bootstrap）步骤，负责在初始成员上创建并引导业务 Raft 分组。
 *
 * @author zxuanhong
 */
public abstract class RaftRoleChangeListenerStep<CONTENT extends PartitionStartupContext>
    implements StartupStep<CONTENT> {
  private final List<RaftRoleChangeListener> raftRoleChangeListeners;
  // 分区感知监听器注册时按分区包装，shutdown 需移除同一包装实例
  private final Map<RaftRoleChangeListener, RaftRoleChangeListener> roleAdapters = new HashMap<>();

  public RaftRoleChangeListenerStep(final List<RaftRoleChangeListener> raftRoleChangeListeners) {
    this.raftRoleChangeListeners = new CopyOnWriteArrayList<>(raftRoleChangeListeners);
  }

  @Override
  public ActorFuture<CONTENT> startup(final CONTENT content) {
    final var result = content.getConcurrencyControl().<CONTENT>createFuture();
    final RaftPartition raftPartition = content.getRaftPartition();
    if (raftPartition == null) {
      result.completeExceptionally(
          new RuntimeException("当前组件比如在RaftBootstrapStep或RaftJoinStep之后执行"));
      return result;
    }
    final PartitionId partitionId = raftPartition.id();
    content
        .getConcurrencyControl()
        .run(
            () -> {
              raftRoleChangeListeners.forEach(
                  roleChangeListener ->
                      raftPartition.addRoleChangeListener(
                          adaptRoleChangeListener(partitionId, roleChangeListener)));
              result.complete(content);
            });
    return result;
  }

  @Override
  public ActorFuture<CONTENT> shutdown(final CONTENT content) {
    final var result = content.getConcurrencyControl().<CONTENT>createFuture();
    final RaftPartition raftPartition = content.getRaftPartition();
    content
        .getConcurrencyControl()
        .run(
            () -> {
              raftRoleChangeListeners.forEach(
                  roleChangeListener ->
                      raftPartition.removeRoleChangeListener(
                          roleAdapters.getOrDefault(roleChangeListener, roleChangeListener)));
              result.complete(content);
            });
    return result;
  }

  /** 分区感知的角色监听器适配：补充回调缺失的分区标识 */
  private RaftRoleChangeListener adaptRoleChangeListener(
      final PartitionId partitionId, final RaftRoleChangeListener listener) {
    if (!(listener instanceof final PartitionTopologyListener topologyListener)) {
      return listener;
    }
    return roleAdapters.computeIfAbsent(
        listener,
        ignored ->
            (RaftRoleChangeListener)
                (newRole, newTerm) ->
                    topologyListener.onPartitionRoleChanged(partitionId, newRole, newTerm));
  }
}
