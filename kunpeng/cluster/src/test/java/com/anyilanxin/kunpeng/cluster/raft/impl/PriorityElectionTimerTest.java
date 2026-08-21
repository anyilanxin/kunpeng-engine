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
package com.anyilanxin.kunpeng.cluster.raft.impl;

import static org.assertj.core.api.Assertions.assertThat;

import com.anyilanxin.kunpeng.cluster.cluster.MemberId;
import com.anyilanxin.kunpeng.cluster.raft.DeterministicSingleThreadContext;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.jmock.lib.concurrent.DeterministicScheduler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class PriorityElectionTimerTest {

  private final Logger log = LoggerFactory.getLogger(PriorityElectionTimerTest.class);
  private final DeterministicSingleThreadContext threadContext =
      new DeterministicSingleThreadContext(new DeterministicScheduler(), MemberId.from(""));

  @AfterEach
  void afterEach() {
    threadContext.close();
  }

  @Test
  void shouldLowerPriorityNodeEventuallyStartsAnElection() {
    // given
    final AtomicInteger triggerCount = new AtomicInteger();

    final Duration electionTimeout = Duration.ofMillis(100);
    final int targetPriority = 4;
    final PriorityElectionTimer timer =
        new PriorityElectionTimer(
            electionTimeout, threadContext, triggerCount::getAndIncrement, log, targetPriority, 1);

    // when
    timer.reset();
    for (int i = 0; i < targetPriority; i++) {
      threadContext
          .getDeterministicScheduler()
          .tick(electionTimeout.toMillis(), TimeUnit.MILLISECONDS);
    }

    // then
    assertThat(triggerCount.get()).describedAs("Time is triggered once").isOne();
  }

  @Test
  void shouldHighPriorityNodeStartElectionFirst() {
    // given
    final String highPrioId = "highPrioTimer";
    final String lowPrioId = "lowPrioTimer";
    final List<String> electionOrder = new CopyOnWriteArrayList<>();

    final int targetPriority = 4;
    final Duration electionTimeout = Duration.ofMillis(100);
    final PriorityElectionTimer timerHighPrio =
        new PriorityElectionTimer(
            electionTimeout,
            threadContext,
            () -> electionOrder.add(highPrioId),
            log,
            targetPriority,
            targetPriority);

    final PriorityElectionTimer timerLowPrio =
        new PriorityElectionTimer(
            electionTimeout,
            threadContext,
            () -> electionOrder.add(lowPrioId),
            log,
            targetPriority,
            1);

    // when
    timerLowPrio.reset();
    timerHighPrio.reset();

    for (int i = 0; i < targetPriority; i++) {
      threadContext
          .getDeterministicScheduler()
          .tick(electionTimeout.toMillis(), TimeUnit.MILLISECONDS);
    }

    // then
    assertThat(electionOrder)
        .as("both elections should have been triggered eventually")
        .contains(highPrioId, lowPrioId);
    assertThat(electionOrder.get(0))
        .as("the first election triggered should have been the high priority election")
        .isEqualTo(highPrioId);
  }

  @Test
  void canChangePriorityDynamically() {
    final String highPrioId = "highPrioTimer";
    final String lowPrioId = "lowPrioTimer";
    final List<String> electionOrder = new CopyOnWriteArrayList<>();

    final int targetPriority = 4;
    final Duration electionTimeout = Duration.ofMillis(100);
    final PriorityElectionTimer timerLowPrio =
        new PriorityElectionTimer(
            electionTimeout,
            threadContext,
            () -> electionOrder.add(lowPrioId),
            log,
            targetPriority, // set higher priority first
            2);

    final PriorityElectionTimer timerHighPrio =
        new PriorityElectionTimer(
            electionTimeout,
            threadContext,
            () -> electionOrder.add(highPrioId),
            log,
            targetPriority,
            1); // set lower priority first

    // when
    timerLowPrio.reset();
    timerHighPrio.reset();

    threadContext
        .getDeterministicScheduler()
        .tick(electionTimeout.toMillis(), TimeUnit.MILLISECONDS);

    timerLowPrio.setNodePriority(1);
    timerHighPrio.setNodePriority(targetPriority);

    for (int i = 0; i < targetPriority - 1; i++) {
      threadContext
          .getDeterministicScheduler()
          .tick(electionTimeout.toMillis(), TimeUnit.MILLISECONDS);
    }

    // then
    assertThat(electionOrder)
        .as("both elections should have been triggered eventually")
        .contains(highPrioId, lowPrioId);
    assertThat(electionOrder.get(0))
        .as("the first election triggered should have been the high priority election")
        .isEqualTo(highPrioId);
  }
}
