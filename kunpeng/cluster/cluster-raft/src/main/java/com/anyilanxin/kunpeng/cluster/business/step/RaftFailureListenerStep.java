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
import com.anyilanxin.kunpeng.cluster.raft.partition.RaftPartition;
import com.anyilanxin.kunpeng.cluster.utils.health.FailureListener;
import com.anyilanxin.kunpeng.cluster.utils.health.HealthReport;
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
public abstract class RaftFailureListenerStep<CONTENT extends PartitionStartupContext>
    implements StartupStep<CONTENT> {
  private final List<FailureListener> failureListeners;
  // 分区感知监听器注册时按分区包装，shutdown 需移除同一包装实例
  private final Map<FailureListener, FailureListener> failureAdapters = new HashMap<>();

  public RaftFailureListenerStep(final List<FailureListener> failureListeners) {
    this.failureListeners = new CopyOnWriteArrayList<>(failureListeners);
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
              failureListeners.forEach(
                  failureListener ->
                      raftPartition.addFailureListener(
                          adaptFailureListener(partitionId, failureListener)));
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
              failureListeners.forEach(
                  failureListener ->
                      raftPartition.removeFailureListener(
                          failureAdapters.getOrDefault(failureListener, failureListener)));
              result.complete(content);
            });
    return result;
  }

  /** 分区感知的健康监听器适配：补充回调缺失的分区标识 */
  private FailureListener adaptFailureListener(
      final PartitionId partitionId, final FailureListener listener) {
    if (!(listener instanceof final PartitionTopologyListener topologyListener)) {
      return listener;
    }
    return failureAdapters.computeIfAbsent(
        listener,
        ignored ->
            new FailureListener() {
              @Override
              public void onFailure(final HealthReport healthReport) {
                topologyListener.onPartitionHealthChanged(partitionId, healthReport);
              }

              @Override
              public void onRecovered(final HealthReport healthReport) {
                topologyListener.onPartitionHealthChanged(partitionId, healthReport);
              }

              @Override
              public void onUnrecoverableFailure(final HealthReport healthReport) {
                topologyListener.onPartitionHealthChanged(partitionId, healthReport);
              }
            });
  }
}
