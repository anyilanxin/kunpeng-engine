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
package com.anyilanxin.kunpeng.cluster.raft.snapshot.transfer;

import com.anyilanxin.kunpeng.cluster.cluster.MemberId;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.ClusterCommunicationService;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.PersistedSnapshot;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.ReceivableSnapshotStore;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.ReceivedSnapshot;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotException;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotType;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.impl.SnapshotChunkImpl;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import org.agrona.concurrent.UnsafeBuffer;

/**
 * 跨分区快照传输客户端：向指定源节点逐块拉取指定类型的最新快照，写入本地接收式快照存储并提交。
 *
 * <p>逐块请求-应答（每块带超时），以"续传分片名"推进；任一块失败即中止本次接收，可整体重拉。
 */
public final class SnapshotTransferClient {

  private final ClusterCommunicationService communicator;
  private final Duration chunkTimeout;

  public SnapshotTransferClient(
      final ClusterCommunicationService communicator, final Duration chunkTimeout) {
    this.communicator = communicator;
    this.chunkTimeout = chunkTimeout;
  }

  /**
   * 从 {@code sourceMember} 节点上的 {@code sourcePartitionName} 分区拉取指定类型的最新快照，
   * 接收并提交到 {@code localStore}。
   */
  public CompletableFuture<PersistedSnapshot> pull(
      final String sourcePartitionName,
      final MemberId sourceMember,
      final SnapshotType type,
      final int preferredChunkSize,
      final ReceivableSnapshotStore localStore) {
    return transferNext(
        SnapshotTransferServer.subjectOf(sourcePartitionName),
        sourceMember,
        type,
        Math.max(1, preferredChunkSize),
        null,
        null,
        localStore);
  }

  private CompletableFuture<PersistedSnapshot> transferNext(
      final String subject,
      final MemberId sourceMember,
      final SnapshotType type,
      final int chunkSize,
      final String afterChunkName,
      final ReceivedSnapshot receiving,
      final ReceivableSnapshotStore localStore) {
    final var request = new SnapshotTransferRequest(type, chunkSize, afterChunkName);
    return communicator
        .send(
            subject,
            request,
            SnapshotTransferRequest::encode,
            SnapshotTransferResponse::decode,
            sourceMember,
            chunkTimeout)
        .thenCompose(
            response -> {
              if (response.getChunkFrame().length == 0) {
                return finishEmpty(receiving);
              }
              final var chunk = parseChunk(response.getChunkFrame());
              final ReceivedSnapshot target =
                  receiving != null
                      ? receiving
                      : localStore.newReceivedSnapshot(chunk.getSnapshotId()).join();
              return target
                  .apply(chunk)
                  .thenCompose(
                      ignored -> {
                        if (response.hasMore()) {
                          return transferNext(
                              subject,
                              sourceMember,
                              type,
                              chunkSize,
                              chunk.getChunkName(),
                              target,
                              localStore);
                        }
                        return target.persist();
                      });
            });
  }

  private CompletableFuture<PersistedSnapshot> finishEmpty(final ReceivedSnapshot receiving) {
    if (receiving != null) {
      receiving.abort();
    }
    return CompletableFuture.failedFuture(
        new SnapshotException("Source partition has no more chunks of the requested snapshot"));
  }

  private SnapshotChunkImpl parseChunk(final byte[] frame) {
    final var chunk = new SnapshotChunkImpl();
    if (!chunk.tryWrap(new UnsafeBuffer(frame))) {
      throw new SnapshotException("Received a malformed snapshot chunk frame");
    }
    return chunk;
  }
}
