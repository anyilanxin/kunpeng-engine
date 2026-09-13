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
package com.anyilanxin.kunpeng.cluster.business.step.transition;

import com.anyilanxin.kunpeng.cluster.raft.RaftRoleStateListener;
import com.anyilanxin.kunpeng.cluster.raft.partition.RaftPartition;
import com.anyilanxin.kunpeng.scheduler.Actor;
import com.anyilanxin.kunpeng.scheduler.future.ActorFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 分区角色切换入口组件：以 Actor 方式运行，监听 Raft 分区的角色状态变化并驱动角色切换服务执行相应切换。
 *
 * @author zxuanhong
 * @since
 */
public class PartitionTransition<CONTENT extends TransitionContent> extends Actor
    implements RaftRoleStateListener {
  private final PartitionTransitionService<CONTENT> transitionService;
  private final RaftPartition raftPartition;
  private final CONTENT transitionContent;
  private final AtomicBoolean started = new AtomicBoolean(false);

  public PartitionTransition(
      final PartitionTransitionService<CONTENT> transitionService,
      final RaftPartition raftPartition,
      final CONTENT transitionContent) {
    this.transitionService = transitionService;
    this.raftPartition = raftPartition;
    this.transitionContent = transitionContent;
    transitionContent.setConcurrencyControl(actor);
    transitionService.setConcurrencyControl(actor);
  }

  public ActorFuture<Void> start() {
    final ActorFuture<Void> future = actor.createFuture();
    actor.submit(
        () -> {
          raftPartition.addRoleStateListener(this);
          transitionService.updateTransitionContext(transitionContent);
          started.set(true);
          future.complete(null);
        });
    return future;
  }

  public ActorFuture<Void> stop() {
    final ActorFuture<Void> future = actor.createFuture();
    actor.submit(
        () ->
            transitionService
                .awaitTransition()
                .onComplete(
                    (_, throwable) -> {
                      if (throwable != null) {
                        future.completeExceptionally(throwable);
                      } else {
                        raftPartition.removeRoleStateListener(this);
                        started.set(false);
                        future.complete(null);
                      }
                    }));
    return future;
  }

  @Override
  public void onLeader(final long currentTerm) {
    actor.run(
        () -> {
          if (started.get()) {
            transitionService.toLeader(currentTerm);
          }
        });
  }

  @Override
  public void onFollower(final long currentTerm) {
    actor.run(
        () -> {
          if (started.get()) {
            transitionService.toFollower(currentTerm);
          }
        });
  }

  @Override
  public void onInactive(final long currentTerm) {
    actor.run(
        () -> {
          if (started.get()) {
            transitionService.toInactive(currentTerm);
          }
        });
  }
}
