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
package com.anyilanxin.kunpeng.cluster.raft.snapshot.transfer;

import com.anyilanxin.kunpeng.cluster.cluster.messaging.ClusterCommunicationService;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.*;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.impl.SnapshotChunkImpl;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * 逐批拉取服务端：按分区订阅拉取主题（{@code snapshot-transfer-}{分区名}），一问一答。
 *
 * <p>协议（与 {@link DefaultSnapshotTransfer} 对应）：
 *
 * <ul>
 *   <li>首个 PULL：为 transferId 建会话（读取器），应答信息分片——chunkName 承载镜像 id、 content 为空；
 *   <li>后续 PULL：应答下一个传输单元（{@link SnapshotChunkBatch}）；
 *   <li>COMPLETE：清理 transferId 对应的会话。
 * </ul>
 *
 * 会话级断点由读取器位置承载；客户端整体失败即以新 transferId 重拉。
 *
 * @author zxuanhong
 * @since 1.0.0
 */
public final class SnapshotTransferServer {

  /** 传输单元累计字节上限（缺省 4MiB）。 */
  private static final int DEFAULT_MAX_BATCH_SIZE = 4 * 1024 * 1024;

  private static final Function<byte[], byte[]> IDENTITY = Function.identity();

  private final ClusterCommunicationService communicator;
  private final String subject;
  private final FileSnapshotStore store;
  private final int maxBatchSize;
  // transferId → 拉取会话
  private final Map<String, PullSession> sessions = new ConcurrentHashMap<>();

  public SnapshotTransferServer(
      final ClusterCommunicationService communicator,
      final String partitionName,
      final FileSnapshotStore store) {
    this(communicator, partitionName, store, DEFAULT_MAX_BATCH_SIZE);
  }

  public SnapshotTransferServer(
      final ClusterCommunicationService communicator,
      final String partitionName,
      final FileSnapshotStore store,
      final int maxBatchSize) {
    this.communicator = communicator;
    subject = DefaultSnapshotTransfer.subjectOf(partitionName);
    this.store = store;
    this.maxBatchSize = maxBatchSize;
  }

  /** 注册本分区的分片拉取处理器（分区 leader 角色时调用）。 */
  public void register() {
    communicator.replyTo(subject, IDENTITY, this::serve, IDENTITY);
  }

  /** 注销拉取处理器（分区停止或离开 leader 时调用）。 */
  public void unregister() {
    communicator.unsubscribe(subject);
    sessions.values().forEach(session -> session.batcher.close());
    sessions.clear();
  }

  private CompletableFuture<byte[]> serve(final byte[] payload) {
    final var request = SnapshotTransferCodec.decodeRequest(payload);
    return switch (request.command()) {
      case PULL -> CompletableFuture.supplyAsync(() -> servePull(request.requestId()));
      case COMPLETE -> {
        final var session = sessions.remove(request.requestId());
        if (session != null) {
          session.batcher.close();
        }
        yield CompletableFuture.completedFuture(new byte[0]);
      }
      default -> CompletableFuture.completedFuture(new byte[0]);
    };
  }

  private byte[] servePull(final String transferId) {
    final var session = sessions.computeIfAbsent(transferId, id -> newSession());
    if (!session.infoSent) {
      // 首个 PULL：应答信息分片（chunkName 承载镜像 id，content 为空）
      session.infoSent = true;
      return SnapshotTransferCodec.encodeChunk(
          new SnapshotChunkImpl(session.snapshotId, 0, 0, 0, ByteBuffer.allocate(0), 0));
    }
    return SnapshotTransferCodec.encodeChunkBatch(session.batcher.nextBatch(maxBatchSize));
  }

  private PullSession newSession() {
    final PersistedSnapshot snapshot =
        store
            .getLatestSnapshot()
            .orElseThrow(() -> new SnapshotException("No snapshot on this partition"));
    return new PullSession(
        snapshot.snapshotId().asString(), snapshot.newChunkReader(UUID.randomUUID()));
  }

  /** 拉取会话：镜像 id + 装批器 + 信息分片是否已发。 */
  private static final class PullSession {
    private final String snapshotId;
    private final SnapshotChunkBatcher batcher;
    private boolean infoSent;

    private PullSession(final String snapshotId, final SnapshotChunkReader reader) {
      this.snapshotId = snapshotId;
      batcher = new SnapshotChunkBatcher(reader);
    }
  }
}
