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
package com.anyilanxin.kunpeng.cluster.raft.snapshot.impl;

import com.anyilanxin.kunpeng.utils.scheduler.Actor;
import com.anyilanxin.kunpeng.utils.scheduler.future.ActorFuture;
import com.anyilanxin.kunpeng.utils.scheduler.future.CompletableActorFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * 副本拉取编排（客户端侧）：首块建立会话 → vault.receive 建立接收副本 → 逐块 apply →
 * 全部到齐后 commitReplica 落档（成为最新快照）；失败自动 abort 接收目录。
 */
public final class ReplicaPullTransfer extends Actor {

  private static final Logger LOG = LoggerFactory.getLogger(ReplicaPullTransfer.class);
  private static final int MAX_TRANSIENT_RETRIES = 120;
  private static final Duration TRANSIENT_RETRY_DELAY = Duration.ofMillis(500);

  private final SnapshotVault vault;
  private final ReplicaTransferClient transferClient;
  private final ReplicaChannel channel;
  private final String actorName;

  public ReplicaPullTransfer(
      final int partitionId,
      final SnapshotVault vault,
      final ReplicaTransferClient transferClient,
      final ReplicaChannel channel) {
    this.vault = vault;
    this.transferClient = transferClient;
    this.channel = channel;
      actorName = "ReplicaPullTransfer-" + channel.name().toLowerCase() + '-' + partitionId;
  }

  @Override
  public String getName() {
    return actorName;
  }

  /** 拉取分区最新快照副本；远端无可用快照时完成于 null */
  public ActorFuture<ArchivedSnapshot> getLatestSnapshot(final int partitionId) {
    final var result = new CompletableActorFuture<ArchivedSnapshot>();
    pullWithRetry(partitionId, 0, result);
    return result;
  }

  private void pullWithRetry(
      final int partitionId,
      final int attempt,
      final CompletableActorFuture<ArchivedSnapshot> result) {
    attemptPull(partitionId)
        .onComplete(
            (snapshot, error) -> {
              if (error != null
                  && isTransientServerError(error)
                  && attempt < MAX_TRANSIENT_RETRIES) {
                LOG.warn(
                    "分区 {} 的 {} 副本拉取遇瞬态错误, 500ms 后第 {} 次重试",
                    partitionId, channel, attempt + 1, error);
                schedule(TRANSIENT_RETRY_DELAY, () -> pullWithRetry(partitionId, attempt + 1, result));
                return;
              }
              if (error != null) {
                result.completeExceptionally(error);
              } else {
                result.complete(snapshot);
              }
            });
  }

  private static boolean isTransientServerError(final Throwable error) {
    Throwable current = error;
    while (current != null) {
      final String message = current.getMessage();
      if (message != null && (message.contains("未登记") || message.contains("会话不存在"))) {
        return true;
      }
      current = current.getCause();
    }
    return false;
  }

  private ActorFuture<ArchivedSnapshot> attemptPull(final int partitionId) {
    final var transferId = UUID.randomUUID();
    LOG.debug("开始拉取分区 {} 的 {} 副本 transferId={}", partitionId, channel, transferId);

    return transferClient
        .getLatestSnapshot(partitionId, -1, transferId, channel)
        .andThen(
            firstBlock -> {
              if (firstBlock == null) {
                return CompletableActorFuture.<PullContext>completed(null);
              }
              return new CompletableActorFuture<>(vault
                  .receive(firstBlock.snapshotId())
                  .thenApply(
                      replica -> {
                        applyBlock(replica, firstBlock);
                        return new PullContext(replica, firstBlock);
                      }));
            })
        .andThen(
            context -> {
              if (context == null) {
                return CompletableActorFuture.<ArchivedSnapshot>completed(null);
              }
              final var pull = pullRemaining(partitionId, context, transferId);
              pull.onError(error -> context.replica().abort());
              return pull;
            });
  }

  private ActorFuture<ArchivedSnapshot> pullRemaining(
      final int partitionId, final PullContext context, final UUID transferId) {
    return transferClient
        .getNextChunk(
            partitionId,
            context.replica().ref().toString(),
            context.lastBlock().blockName(),
            transferId,
            channel)
        .andThen(
            block -> {
              if (block != null) {
                applyBlock(context.replica(), block);
                return pullRemaining(partitionId, new PullContext(context.replica(), block), transferId);
              }
              return new CompletableActorFuture<>(vault
                  .commitReplica(context.replica())
                  .thenApply(
                      ignored -> {
                        final Optional<ArchivedSnapshot> latest = vault.getLatestSnapshot();
                        if (latest.isEmpty()
                            || latest.get().ref().compareTo(context.replica().ref()) != 0) {
                          throw new SnapshotStoreException.WriteFailure(
                              "接收副本提交后不可见: " + context.replica().ref(), null);
                        }
                        return latest.get();
                      }));
            });
  }

  private void applyBlock(final IncomingReplica replica, final SnapshotBlock block) {
    try {
      replica.apply(block);
    } catch (final Exception e) {
      throw new SnapshotStoreException.WriteFailure("接收块应用失败: " + block.blockName(), e);
    }
  }

  @Override
  public ActorFuture<Void> closeAsync() {
    return transferClient.closeAsync().andThen(ignored -> super.closeAsync());
  }

  private record PullContext(IncomingReplica replica, SnapshotBlock lastBlock) {}
}
