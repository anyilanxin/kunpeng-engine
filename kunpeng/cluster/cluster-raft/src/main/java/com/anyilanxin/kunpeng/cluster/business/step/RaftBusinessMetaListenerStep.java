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
import com.anyilanxin.kunpeng.cluster.raft.RaftBusinessMetaListener;
import com.anyilanxin.kunpeng.cluster.raft.partition.RaftPartition;
import com.anyilanxin.kunpeng.scheduler.future.ActorFuture;
import com.anyilanxin.kunpeng.scheduler.startup.StartupStep;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 业务分区启动流程中的引导（bootstrap）步骤，负责在初始成员上创建并引导业务 Raft 分组。
 *
 * @author zxuanhong
 */
public abstract class RaftBusinessMetaListenerStep<CONTENT extends PartitionStartupContext>
    implements StartupStep<CONTENT> {
  private final List<RaftBusinessMetaListener> raftBusinessMetaListeners;

  public RaftBusinessMetaListenerStep(
      final List<RaftBusinessMetaListener> raftBusinessMetaListeners) {
    this.raftBusinessMetaListeners = new CopyOnWriteArrayList<>(raftBusinessMetaListeners);
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
              raftBusinessMetaListeners.forEach(raftPartition::addBusinessMetaListener);
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
              raftBusinessMetaListeners.forEach(raftPartition::removeBusinessMetaListener);
              result.complete(content);
            });
    return result;
  }
}
