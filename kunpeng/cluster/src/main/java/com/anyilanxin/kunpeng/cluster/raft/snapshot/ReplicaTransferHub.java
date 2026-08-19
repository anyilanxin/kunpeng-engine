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
package com.anyilanxin.kunpeng.cluster.raft.snapshot;

import com.anyilanxin.kunpeng.cluster.raft.snapshot.ReplicaSenderService.SnapshotTaker;
import com.anyilanxin.kunpeng.utils.scheduler.future.ActorFuture;
import com.anyilanxin.kunpeng.utils.scheduler.future.CompletableActorFuture;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 副本传输中枢（服务端会话编排）：会话按 (分区, transferId) 持有源快照租约与块读取器；
 * 副本缺失或过旧时经补拍回调重建；块顺序由"上一块游标"校验保证。
 */
public class ReplicaTransferHub implements ReplicaSenderService {

  private static final Logger LOG = LoggerFactory.getLogger(ReplicaTransferHub.class);

  private final SnapshotVault vault;
  private final SnapshotTaker taker;
  private final int maxBlockBytes;
  private final Map<SessionKey, Session> sessions = new HashMap<>();
  private volatile boolean closed;

  public ReplicaTransferHub(
      final SnapshotVault vault, final SnapshotTaker taker, final int maxBlockBytes) {
    this.vault = vault;
    this.taker = taker;
    this.maxBlockBytes = maxBlockBytes;
  }

  protected SnapshotVault vault() {
    return vault;
  }

  @Override
  public synchronized ActorFuture<SnapshotBlock> getLatestSnapshot(
      final int partition, final long lastProcessedPosition, final UUID transferId) {
    if (closed) {
      return refused("传输服务已关闭");
    }
    final var future = new CompletableActorFuture<SnapshotBlock>();
    resolveSnapshot(lastProcessedPosition)
        .onComplete(
            (snapshot, error) -> {
              if (error != null) {
                future.completeExceptionally(error);
                return;
              }
              try {
                final var lease = snapshot.reserve();
                final var reader = snapshot.blockReader(maxBlockBytes);
                final var first = reader.next();
                sessions.put(
                    new SessionKey(partition, transferId),
                    new Session(snapshot.ref().toString(), lease, reader, first.blockName()));
                future.complete(first);
              } catch (final Exception e) {
                future.completeExceptionally(e);
              }
            });
    return future;
  }

  @Override
  public synchronized ActorFuture<SnapshotBlock> getNextChunk(
      final int partition,
      final String snapshotId,
      final String previousChunkName,
      final UUID transferId) {
    if (closed) {
      return refused("传输服务已关闭");
    }
    final Session session = sessions.get(new SessionKey(partition, transferId));
    if (session == null) {
      return refused("会话不存在: partition=" + partition + " transferId=" + transferId);
    }
    if (!session.snapshotId.equals(snapshotId)) {
      return refused("会话快照不符: 期望 " + session.snapshotId + " 实际 " + snapshotId);
    }
    if (!session.lastBlockName.equals(previousChunkName)) {
      return refused("续读游标不符: 期望 " + session.lastBlockName + " 实际 " + previousChunkName);
    }
    try {
      if (!session.reader.hasNext()) {
        sessions.remove(new SessionKey(partition, transferId));
        return CompletableActorFuture.completed(null);
      }
      final var next = session.reader.next();
      sessions.put(
          new SessionKey(partition, transferId),
          new Session(session.snapshotId, session.lease, session.reader, next.blockName()));
      return CompletableActorFuture.completed(next);
    } catch (final Exception e) {
      return CompletableActorFuture.completedExceptionally(e);
    }
  }

  @Override
  public synchronized ActorFuture<Void> deleteSnapshots(final int partitionId) {
    if (closed) {
      return refusedVoid("传输服务已关闭");
    }
    sessions.clear();
    return deleteCached();
  }

  @Override
  public synchronized ActorFuture<Void> closeAsync() {
    closed = true;
    sessions.clear();
    return CompletableActorFuture.completed(null);
  }

  private ActorFuture<ArchivedSnapshot> resolveSnapshot(final long lastProcessedPosition) {
    final Optional<ArchivedSnapshot> current = currentCached();
    if (current.isPresent() && current.orElseThrow().ref().index() >= lastProcessedPosition) {
      return CompletableActorFuture.completed(current.orElseThrow());
    }
    final var future = new CompletableActorFuture<ArchivedSnapshot>();
    taker
        .takeSnapshot(lastProcessedPosition)
        .onComplete(
            (ignored, error) -> {
              if (error != null) {
                future.completeExceptionally(error);
                return;
              }
              final Optional<ArchivedSnapshot> after = currentCached();
              if (after.isEmpty()) {
                future.completeExceptionally(new SnapshotStoreException.NotFound("补拍后副本仍缺失"));
                return;
              }
              future.complete(after.orElseThrow());
            });
    return future;
  }

  /** 当前缓存副本（子类定向 bootstrap/merge 区） */
  protected Optional<ArchivedSnapshot> currentCached() {
    return vault.getBootstrapSnapshot();
  }

  /** 清空缓存副本区 */
  protected ActorFuture<Void> deleteCached() {
    return new CompletableActorFuture<>(vault.deleteBootstrapSnapshots());
  }

  private static ActorFuture<Void> wrapVoid(final java.util.concurrent.CompletableFuture<Void> f) {
    return new CompletableActorFuture<>(f);
  }

  private static ActorFuture<SnapshotBlock> refused(final String reason) {
    LOG.warn("副本传输请求被拒: {}", reason);
    return CompletableActorFuture.completedExceptionally(
        new SnapshotStoreException.NotFound(reason));
  }

  private static ActorFuture<Void> refusedVoid(final String reason) {
    LOG.warn("副本传输请求被拒: {}", reason);
    return CompletableActorFuture.completedExceptionally(
        new SnapshotStoreException.NotFound(reason));
  }

  private record SessionKey(int partition, UUID transferId) {}

  private record Session(
      String snapshotId,
      ArchivedSnapshot.Lease lease,
      BlockStreamReader reader,
      String lastBlockName) {}
}
