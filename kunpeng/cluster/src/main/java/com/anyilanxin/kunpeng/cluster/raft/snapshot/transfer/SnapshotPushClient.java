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
import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotChunkReader;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotException;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * 合并快照推送客户端：源分区 leader 把本分区的 MERGE 快照以批量分片**主动推送**到目标分区
 * leader 的接收处理器。
 *
 * <p>逐批发送（贪心装批、累计字节 ≤ maxBatchSize、单片超限独占一批），末批携带结束标志；
 * 任一批应答失败即中止整个推送。推送是幂等重发起的——目标侧按快照 id 建会话，重推会重建
 * pending 覆盖旧会话。
 */
public final class SnapshotPushClient {

  private final ClusterCommunicationService communicator;
  private final Duration batchTimeout;

  public SnapshotPushClient(
      final ClusterCommunicationService communicator, final Duration batchTimeout) {
    this.communicator = communicator;
    this.batchTimeout = batchTimeout;
  }

  /**
   * 把 {@code snapshot} 推送到 {@code targetMember} 节点上 {@code targetPartitionName} 分区
   * 的合并接收处理器；完成时目标侧已完成校验与提交。
   */
  public CompletableFuture<Void> push(
      final String targetPartitionName,
      final MemberId targetMember,
      final PersistedSnapshot snapshot,
      final int preferredChunkSize,
      final int maxBatchSize) {
    try {
      final SnapshotChunkReader reader = snapshot.newChunkReader();
      reader.setMaximumChunkSize(Math.max(1, preferredChunkSize));
      return pushNext(
              SnapshotPushServer.subjectOf(targetPartitionName),
              targetMember,
              snapshot.getId(),
              reader,
              Math.max(1, maxBatchSize))
          .whenComplete((ignored, error) -> reader.close());
    } catch (final Exception e) {
      return CompletableFuture.failedFuture(e);
    }
  }

  private CompletableFuture<Void> pushNext(
      final String subject,
      final MemberId targetMember,
      final String snapshotId,
      final SnapshotChunkReader reader,
      final int maxBatchSize) {
    if (!reader.hasNext()) {
      return CompletableFuture.failedFuture(
          new SnapshotException("Merge snapshot " + snapshotId + " has no chunk to push"));
    }
    final var batch = SnapshotTransferServer.fillBatch(reader, maxBatchSize);
    final var request =
        SnapshotPushRequest.of(
            snapshotId, batch.getLastChunkName(), batch.getChunkFrames(), batch.hasMore());
    return communicator
        .send(
            subject,
            request,
            SnapshotPushRequest::encode,
            SnapshotPushAck::decode,
            targetMember,
            batchTimeout)
        .thenCompose(
            ack -> {
              if (!ack.isOk()) {
                return CompletableFuture.failedFuture(
                    new SnapshotException(
                        "Merge push to " + subject + " rejected: " + ack.getError()));
              }
              if (batch.hasMore()) {
                return pushNext(subject, targetMember, snapshotId, reader, maxBatchSize);
              }
              return CompletableFuture.completedFuture(null);
            });
  }
}
