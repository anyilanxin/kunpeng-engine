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
package com.anyilanxin.kunpeng.cluster.manager.admin.raft.step.transition.logstorage;

import static com.anyilanxin.kunpeng.utils.Either.left;
import static com.anyilanxin.kunpeng.utils.Either.right;

import com.anyilanxin.kunpeng.cluster.business.step.transition.TransitionStep;
import com.anyilanxin.kunpeng.cluster.manager.admin.raft.step.transition.AdminTransitionContent;
import com.anyilanxin.kunpeng.cluster.raft.logentry.LogAppender;
import com.anyilanxin.kunpeng.cluster.raft.partition.impl.RaftPartitionServer;
import com.anyilanxin.kunpeng.cluster.raft.storage.log.entry.ApplicationEntry;
import com.anyilanxin.kunpeng.scheduler.future.ActorFuture;
import com.anyilanxin.kunpeng.utils.Either;

public final class AdminLogStoragePartitionTransitionStep
    implements TransitionStep<AdminTransitionContent> {
  private static final String WRONG_TERM_ERROR_MSG =
      "Expected that current term '%d' is same as raft term '%d', but was not. Failing installation of 'AdminLogStoragePartitionTransitionStep' on partition %d.";

  @Override
  public String getName() {
    return "Admin Log Storage Partition Transition";
  }

  @Override
  public ActorFuture<Void> onLeader(final AdminTransitionContent context, final long currentTerm) {
    return buildLogStorage(context, currentTerm, true);
  }

  @Override
  public ActorFuture<Void> onFollower(
      final AdminTransitionContent context, final long currentTerm) {
    return buildLogStorage(context, currentTerm, false);
  }

  @Override
  public ActorFuture<Void> onInactive(
      final AdminTransitionContent context, final long currentTerm) {
    final ActorFuture<Void> future = context.getConcurrencyControl().createFuture();
    context
        .getConcurrencyControl()
        .run(
            () -> {
              final var logStorage = context.getEventStore();
              if (logStorage != null) {
                context.getRaftPartition().getServer().removeCommitListener(logStorage);
                context.setEventStore(null);
              }
              future.complete(null);
            });
    return future;
  }

  private ActorFuture<Void> buildLogStorage(
      final AdminTransitionContent context, final long currentTerm, final boolean isLeader) {
    final ActorFuture<Void> future = context.getConcurrencyControl().createFuture();
    context
        .getConcurrencyControl()
        .run(
            () -> {
              if (context.getEventStore() == null) {
                final var logStorageOrException = buildEventStore(context, currentTerm, isLeader);
                if (logStorageOrException.isRight()) {
                  final var logStorage = logStorageOrException.get();
                  context.setEventStore(logStorage);
                  context.getRaftPartition().getServer().addCommitListener(logStorage);
                  future.complete(null);
                } else {
                  future.completeExceptionally(logStorageOrException.getLeft());
                }
              } else {
                future.complete(null);
              }
            });
    return future;
  }

  private Either<Exception, AdminRaftEventStore> buildEventStore(
      final AdminTransitionContent context, final long targetTerm, final boolean isLeader) {
    final var server = context.getRaftPartition().getServer();
    if (isLeader) {
      return createWritableLogStorage(context, server, targetTerm);
    } else {
      return createReadOnlyStorage(server);
    }
  }

  private Either<Exception, AdminRaftEventStore> createReadOnlyStorage(
      final RaftPartitionServer server) {

    return right(new AdminRaftEventStore(server::openReader, new LogAppenderForReadOnlyStorage()));
  }

  private Either<Exception, AdminRaftEventStore> createWritableLogStorage(
      final AdminTransitionContent context,
      final RaftPartitionServer server,
      final long targetTerm) {
    final var appenderOptional = server.getAppender();
    return appenderOptional
        .map(logAppender -> checkAndCreateEventStore(context, server, logAppender, targetTerm))
        .orElseGet(
            () ->
                left(
                    new NotLeaderException(
                        "Expected to get writable log storage, but the node is not the leader for the partition anymore. Failing installation of 'LogStoragePartitionStep'.")));
  }

  private Either<Exception, AdminRaftEventStore> checkAndCreateEventStore(
      final AdminTransitionContent context,
      final RaftPartitionServer server,
      final LogAppender logAppender,
      final long targetTerm) {
    final var raftTerm = server.getTerm();

    if (raftTerm != targetTerm) {
      return left(
          new NotLeaderException(
              String.format(
                  WRONG_TERM_ERROR_MSG, targetTerm, raftTerm, context.getRaftPartitionId().id())));
    } else {
      final var logStorage = AdminRaftEventStore.ofPartition(server::openReader, logAppender);
      return right(logStorage);
    }
  }

  public static final class NotLeaderException extends RuntimeException {

    private NotLeaderException(final String message) {
      super(message);
    }
  }

  private static final class LogAppenderForReadOnlyStorage implements LogAppender {

    @Override
    public void appendEntry(final ApplicationEntry entry, final AppendListener appendListener) {
      throw new UnsupportedOperationException(
          String.format(
              "Expect to append entry (positions %d - %d), but was in Follower role. Followers must not append entries to the log storage",
              entry.lowestPosition(), entry.highestPosition()));
    }
  }
}
