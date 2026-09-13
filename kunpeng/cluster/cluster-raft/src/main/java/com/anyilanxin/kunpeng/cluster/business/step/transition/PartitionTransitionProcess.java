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
import java.util.Deque;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;

/** 单次分区角色切换的执行过程，按顺序执行各切换步骤。 */
final class PartitionTransitionProcess<CONTENT extends TransitionContent> {

  public static final String MSG_PREPARE_TRANSITION =
      "Prepare transition from {}[term: {}] -> {}[term: {}]";
  public static final String MSG_PREPARE_TRANSITION_STEP =
      MSG_PREPARE_TRANSITION + " - preparing {}";
  public static final String MSG_PREPARE_TRANSITION_COMPLETED =
      MSG_PREPARE_TRANSITION + " completed";
  private static final Logger LOG = ClusterRaftLoggers.CLUSTER_RAFT;
  private static final long STEP_TIMEOUT_MS = Duration.ofSeconds(60).toMillis();
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
    totalSteps = pendingSteps.size();
    pendingSteps.forEach(stepsToPrepare::push);
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
          switch (transitionType) {
            case TO_LEADER ->
                nextStep
                    .onLeader(context, term)
                    .onComplete((_, error) -> onStepCompletion(future, error));
            case TO_FOLLOWER ->
                nextStep
                    .onFollower(context, term)
                    .onComplete((_, error) -> onStepCompletion(future, error));
            case TO_INACTIVE ->
                nextStep
                    .onInactive(context, term)
                    .onComplete((_, error) -> onStepCompletion(future, error));
          }
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
