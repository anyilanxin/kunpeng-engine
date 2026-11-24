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
import com.anyilanxin.kunpeng.cluster.raft.RaftServer.Role;
import com.anyilanxin.kunpeng.scheduler.ConcurrencyControl;
import com.anyilanxin.kunpeng.scheduler.clock.ActorClock;
import com.anyilanxin.kunpeng.scheduler.future.ActorFuture;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;

/**
 * 单次分区角色切换的执行过程，按顺序执行各切换步骤。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
final class PartitionTransitionProcess<CONTENT extends TransitionContent> {

  public static final String MSG_PREPARE_TRANSITION =
      "Prepare transition from {}[term: {}] -> {}[term: {}]";
  public static final String MSG_PREPARE_TRANSITION_STEP =
      MSG_PREPARE_TRANSITION + " - preparing {}";
  public static final String MSG_PREPARE_TRANSITION_COMPLETED =
      MSG_PREPARE_TRANSITION + " completed";
  private static final Logger LOG = ClusterRaftLoggers.CLUSTER_RAFT;

  /**
   * 单个切换步骤的慢步骤告警阈值：超过后仍未见完成回调即记 WARN。只是可见性阈值，不是失败条件——同一分区的切换严格串行， 一个不完成的步骤会让后续切换（含关机的
   * TO_INACTIVE）一直排在它后面。
   */
  private static final Duration STEP_TIMEOUT = Duration.ofSeconds(60);

  private TransitionStep<CONTENT> currentStep;
  private final List<TransitionStep<CONTENT>> pendingSteps;
  private final int totalSteps;
  private int currentStepIndex;
  private final Deque<TransitionStep<CONTENT>> stepsToPrepare = new ArrayDeque<>();
  private final ConcurrencyControl concurrencyControl;
  private final CONTENT context;
  private final long term;
  private final Role role;
  private final TransitionType transitionType;
  private long stepStartedAtMs = -1;

  PartitionTransitionProcess(
      final List<TransitionStep<CONTENT>> pendingSteps,
      final ConcurrencyControl concurrencyControl,
      final CONTENT context,
      final TransitionType transitionType,
      final long term,
      final Role role) {
    this.transitionType = transitionType;
    this.role = requireNonNull(role);
    this.term = term;
    this.pendingSteps = new ArrayList<>(requireNonNull(pendingSteps));
    // 关闭类切换按逆序执行：启动按列表正序逐层构建（存储先于服务），关闭必须反方向逐层释放
    // （服务先于存储），否则引擎定时任务等上层使用者会在存储关闭之后仍被调度执行
    if (transitionType == TransitionType.TO_INACTIVE) {
      Collections.reverse(this.pendingSteps);
    }
    totalSteps = this.pendingSteps.size();
    this.pendingSteps.forEach(stepsToPrepare::add);
    this.concurrencyControl = requireNonNull(concurrencyControl);
    this.context = requireNonNull(context);
  }

  void start(final ActorFuture<Void> future) {
    LOG.info("Transition to {} on term {} starting", role, term);

    if (pendingSteps.isEmpty()) {
      LOG.info("No steps defined for transition");
      future.complete(null);
      return;
    }
    proceedWithTransition(future);
  }

  private void proceedWithTransition(final ActorFuture<Void> future) {
    concurrencyControl.run(
        () -> {
          final var nextStep = pendingSteps.removeFirst();
          currentStep = nextStep;
          currentStepIndex = totalSteps - pendingSteps.size();
          stepStartedAtMs = ActorClock.currentTimeMillis();
          LOG.info(
              "Transition to {} on term {} - step {}: {}",
              role,
              term,
              stepLabel(),
              nextStep.getName());
          // 慢步骤看门狗（只告警不失败）：引导快照、大库恢复、日志回放等步骤可以合法地超过 STEP_TIMEOUT，
          // 超时即失败会误杀它们；但超过阈值的步骤必须可见——卡死的步骤与慢步骤在日志里要能区分，
          // 否则后续切换（含关机）只是在队列里无声地等。回调与定时器都在 concurrencyControl 线程上执行
          final boolean[] stepFinished = {false};
          concurrencyControl.schedule(
              STEP_TIMEOUT,
              () -> {
                if (!stepFinished[0]) {
                  LOG.warn(
                      "Transition to {} on term {} - step {}: {} is still running after {} ms;"
                          + " later transitions for this partition are queued behind it",
                      role,
                      term,
                      stepLabel(),
                      nextStep.getName(),
                      ActorClock.currentTimeMillis() - stepStartedAtMs);
                }
              });
          final ActorFuture<Void> stepFuture;
          switch (transitionType) {
            case TO_LEADER -> stepFuture = nextStep.onLeader(context, term);
            case TO_FOLLOWER -> stepFuture = nextStep.onFollower(context, term);
            case TO_INACTIVE -> stepFuture = nextStep.onInactive(context, term);
            default ->
                throw new IllegalStateException("Unexpected transition type: " + transitionType);
          }
          stepFuture.onComplete(
              (_, error) -> {
                stepFinished[0] = true;
                onStepCompletion(future, error);
              });
        });
  }

  private void onStepCompletion(final ActorFuture<Void> future, final Throwable error) {
    if (error != null) {
      LOG.info(
          "Transition to {} on term {} - step {}: {} failed",
          role,
          term,
          stepLabel(),
          currentStep.getName(),
          error);
      LOG.warn(
          "Transition to {} on term {} aborted due to exception during step {}: {}",
          role,
          term,
          stepLabel(),
          currentStep.getName(),
          error);
      future.completeExceptionally(error);
      return;
    }
    LOG.info(
        "Transition to {} on term {} - step {}: {} completed",
        role,
        term,
        stepLabel(),
        currentStep.getName());
    if (pendingSteps.isEmpty()) {
      LOG.info("Transition to {} on term {} completed", role, term);
      future.complete(null);
      currentStep = null;
      stepStartedAtMs = -1;
      return;
    }
    proceedWithTransition(future);
  }

  /** 步骤序号标签，形如 {@code 3/6}：当前序号/总步骤数 */
  private String stepLabel() {
    return currentStepIndex + "/" + totalSteps;
  }

  @Override
  public String toString() {
    return "PartitionTransitionProcess{"
        + "term="
        + term
        + ", role="
        + role
        + ", stepsToPrepare=["
        + stepsToPrepare.stream().map(TransitionStep::getName).collect(Collectors.joining(", "))
        + "], pendingSteps=["
        + pendingSteps.stream().map(TransitionStep::getName).collect(Collectors.joining(", "))
        + "]}";
  }
}
