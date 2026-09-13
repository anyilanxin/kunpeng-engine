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

import static java.util.Objects.requireNonNull;

import com.anyilanxin.kunpeng.cluster.business.ClusterRaftLoggers;
import com.anyilanxin.kunpeng.cluster.business.exception.FailedPartitionTransitionPreparation;
import com.anyilanxin.kunpeng.cluster.raft.RaftServer;
import com.anyilanxin.kunpeng.scheduler.ConcurrencyControl;
import com.anyilanxin.kunpeng.scheduler.future.ActorFuture;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;

/**
 * 分区角色切换服务的默认实现，负责按步骤执行 leader/follower/inactive 之间的切换并保证切换串行执行。
 *
 * @author zxuanhong
 * @since
 */
public class DefaultPartitionTransitionService<CONTENT extends TransitionContent>
    implements PartitionTransitionService<CONTENT> {
  private static final Logger LOG = ClusterRaftLoggers.CLUSTER_RAFT;
  private final List<TransitionStep<CONTENT>> steps;
  private CONTENT context;
  private ConcurrencyControl concurrencyControl;

  // 瞬态状态——用于跟踪当前进行中的角色切换
  private PartitionTransitionProcess currentTransition;
  private ActorFuture<Void> currentTransitionFuture;

  public DefaultPartitionTransitionService(final List<TransitionStep<CONTENT>> steps) {
    this.steps = new ArrayList<>(requireNonNull(steps));
  }

  @Override
  public ActorFuture<Void> toFollower(final long term) {
    return transitionTo(term, RaftServer.Role.FOLLOWER, TransitionType.TO_FOLLOWER);
  }

  @Override
  public ActorFuture<Void> toLeader(final long term) {
    return transitionTo(term, RaftServer.Role.LEADER, TransitionType.TO_LEADER);
  }

  @Override
  public ActorFuture<Void> toInactive(final long term) {
    return transitionTo(term, RaftServer.Role.INACTIVE, TransitionType.TO_INACTIVE);
  }

  @Override
  public ActorFuture<Void> awaitTransition() {
    final ActorFuture<Void> result = concurrencyControl.createFuture();
    concurrencyControl.run(
        () -> {
          if (currentTransitionFuture == null) {
            // 尚无任何切换记录，立即完成
            result.complete(null);
            return;
          }
          // 等待进行中（或排队中）的切换结束，成功/失败原样透传
          currentTransitionFuture.onComplete(
              (nothing, error) -> {
                if (error != null) {
                  result.completeExceptionally(error);
                } else {
                  result.complete(null);
                }
              });
        });
    return result;
  }

  @Override
  public void setConcurrencyControl(final ConcurrencyControl concurrencyControl) {
    this.concurrencyControl = requireNonNull(concurrencyControl);
  }

  @Override
  public void updateTransitionContext(final CONTENT transitionContext) {
    context = transitionContext;
  }

  public ActorFuture<Void> transitionTo(
      final long term, final RaftServer.Role role, final TransitionType transitionType) {
    LOG.info("Transition to {} on term {} requested.", role, term);
    final ActorFuture<Void> nextTransitionFuture = concurrencyControl.createFuture();
    concurrencyControl.run(
        () -> {
          final var nextTransition =
              new PartitionTransitionProcess<>(
                  steps, concurrencyControl, context, transitionType, term, role);
          nextTransitionFuture.onComplete(
              (v, error) -> {
                if (!(error instanceof FailedPartitionTransitionPreparation)) {
                  // 准备阶段已成功，本次切换要么成功完成、要么失败。无论哪种情况，
                  // 都更新 term 和角色，以确保下一次切换仍会执行必要的准备阶段。
                  // 若实际抛出的是 `FailedPartitionTransitionPreparation`，则下一次切换必须
                  // 重新执行相同的准备流程，因此此时还不能更新 term 和角色。
                  context.setCurrentTerm(term);
                  context.setCurrentRole(role);
                }
              });

          enqueueNextTransition(nextTransitionFuture, nextTransition);
        });

    return nextTransitionFuture;
  }

  /** 将下一次切换排到当前正在执行的切换完成之后，保证所有切换严格串行执行、互不并行。 */
  private void enqueueNextTransition(
      final ActorFuture<Void> nextTransitionFuture,
      final PartitionTransitionProcess nextTransition) {
    if (currentTransition == null) {
      // 当前没有进行中的切换，直接开始
      currentTransitionFuture = nextTransitionFuture;
      currentTransition = nextTransition;
      nextTransition.start(nextTransitionFuture);
      return;
    }
    // 所有切换过程共享同一批 TransitionStep 实例，并行执行会导致步骤实例状态错乱
    // （例如 RocksdbPartitionTransitionStep 中 getRocksdb() == null 判断失效、重复 recover）。
    // 因此下一次切换必须等上一次切换完全结束后才能开始，任何情况下都不并行、不抢占。
    currentTransitionFuture.onComplete(
        (nothing, error) -> nextTransition.start(nextTransitionFuture));
    currentTransitionFuture = nextTransitionFuture;
    currentTransition = nextTransition;
  }
}
