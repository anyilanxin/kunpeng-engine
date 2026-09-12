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
package com.anyilanxin.kunpeng.cluster.raft;

import com.anyilanxin.kunpeng.cluster.cluster.MemberId;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/** An operation that can be executed on a raft member */
public final class RaftOperation {

  private final BiConsumer<ControllableRaftContexts, MemberId> operation;
  private final String name;

  private RaftOperation(
      final String name, final BiConsumer<ControllableRaftContexts, MemberId> operation) {
    this.name = name;
    this.operation = operation;
  }

  public static RaftOperation of(
      final String name, final BiConsumer<ControllableRaftContexts, MemberId> operation) {
    return new RaftOperation(name, operation);
  }

  public void run(final ControllableRaftContexts raftContext, final MemberId memberId) {
    operation.accept(raftContext, memberId);
  }

  @Override
  public String toString() {
    return name;
  }

  /** Returns a list of RaftOperation */
  public static List<RaftOperation> getRaftOperationsWithSnapshot() {
    final List<RaftOperation> defaultRaftOperation = getDefaultRaftOperations();
    defaultRaftOperation.add(
        RaftOperation.of("Take snapshot", ControllableRaftContexts::snapshotAndCompact));
    return defaultRaftOperation;
  }

  public static List<RaftOperation> getRaftOperationsWithRestarts() {
    final List<RaftOperation> defaultRaftOperation = getDefaultRaftOperations();
    defaultRaftOperation.add(RaftOperation.of("Restart member", ControllableRaftContexts::restart));
    return defaultRaftOperation;
  }

  public static List<RaftOperation> getRaftOperationsWithSnapshotsAndRestarts() {
    final List<RaftOperation> operationsWithSnapshot = getRaftOperationsWithSnapshot();
    operationsWithSnapshot.add(
        RaftOperation.of("Restart member", ControllableRaftContexts::restart));
    return operationsWithSnapshot;
  }

  public static List<RaftOperation> getRaftOperationsWithSnapshotsAndRestartsWithDataLoss() {
    final List<RaftOperation> operations = getRaftOperationsWithSnapshotsAndRestarts();
    operations.add(
        RaftOperation.of(
            "Restart member with full data loss", ControllableRaftContexts::restartWithDataLoss));
    return operations;
  }

  public static List<RaftOperation> getDefaultRaftOperations() {
    final List<RaftOperation> defaultRaftOperation = new ArrayList<>();
    defaultRaftOperation.add(
        RaftOperation.of("Run next task", ControllableRaftContexts::runNextTask));
    defaultRaftOperation.add(
        RaftOperation.of("Receive next message", ControllableRaftContexts::processNextMessage));
    defaultRaftOperation.add(
        RaftOperation.of("Tick electionTimeout", ControllableRaftContexts::tickElectionTimeout));
    defaultRaftOperation.add(
        RaftOperation.of("Tick heartbeatTimeout", ControllableRaftContexts::tickHeartbeatTimeout));
    defaultRaftOperation.add(
        RaftOperation.of(
            "Tick 50ms", (raftContexts, m) -> raftContexts.tick(m, Duration.ofMillis(50))));
    defaultRaftOperation.add(
        RaftOperation.of(
            "Append on leader", (raftContexts, m) -> raftContexts.clientAppendOnLeader()));
    defaultRaftOperation.add(
        RaftOperation.of(
            "Drop next message",
            (raftContexts, m) -> raftContexts.getServerProtocol(m).dropNextMessage()));
    defaultRaftOperation.add(
        RaftOperation.of("Transfer leadership", ControllableRaftContexts::transferLeadershipTo));
    return defaultRaftOperation;
  }
}
