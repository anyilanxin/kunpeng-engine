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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.cluster.raft.snapshot.bootstrap;

import com.anyilanxin.kunpeng.cluster.cluster.messaging.ClusterCommunicationService;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.PersistedSnapshot;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.RequestCommand;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotChunkReader;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotException;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotId;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotTransferCodec;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.impl.SnapshotChunkImpl;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.transfer.SnapshotChunkBatcher;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.LongSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 引导镜像拍摄端：源分区 leader 注册本处理器（主题 {@code snapshot-bootstrap-}{分区名}）， 接收新分区引导节点的跨分区引导请求。
 *
 * <p>协议（请求复用 {@link SnapshotTransferCodec}，命令扩展见 {@link RequestCommand}）：
 *
 * <ul>
 *   <li>BOOTSTRAP（首个请求，携带 transferId）：拍摄引导镜像（已有则复用）并登记 transferId 引用， 应答信息分片——chunkName
 *       承载镜像 id、content 为空；同 transferId 重试直接重发信息分片；
 *   <li>PULL：与常规拉取一致，应答下一个传输单元（会话级断点由读取器位置承载）；
 *   <li>COMPLETE：清理本次传输的读取器（引用保留）；
 *   <li>RELEASE：引导结束/放弃，移除 transferId 引用；当镜像已无任何引用（这是最后一个请求方）时 真正删除引导镜像，否则只减少一个请求方。
 * </ul>
 *
 * <p>引用计数只在内存：拍摄节点重启后 {@link BootstrapSnapshotStore#start()} 清空残留镜像， 引导节点以新 transferId
 * 重新走 BOOTSTRAP 全流程。
 *
 * @author zxuanhong
 * @since 1.0.0
 */
public final class BootstrapSnapshotServer {

  /** 引导镜像主题前缀：{@value #SUBJECT_PREFIX}{分区名}。 */
  static final String SUBJECT_PREFIX = "snapshot-bootstrap-";

  /** 传输单元累计字节上限（缺省 4MiB）。 */
  private static final int DEFAULT_MAX_BATCH_SIZE = 4 * 1024 * 1024;

  private static final Function<byte[], byte[]> IDENTITY = Function.identity();

  private static final Logger LOGGER = LoggerFactory.getLogger(BootstrapSnapshotServer.class);

  private final ClusterCommunicationService communicator;
  private final String subject;
  private final BootstrapSnapshotStore bootstrapSnapshotStore;
  private final LongSupplier commitIndexSupplier;
  private final LongSupplier termSupplier;
  private final int maxBatchSize;
  // transferId → 拉取会话（引用计数的最小单元：一个 transferId 即一个请求方）
  private final Map<String, PullSession> sessions = new ConcurrentHashMap<>();

  public BootstrapSnapshotServer(
      final ClusterCommunicationService communicator,
      final String partitionName,
      final BootstrapSnapshotStore bootstrapSnapshotStore,
      final LongSupplier commitIndexSupplier,
      final LongSupplier termSupplier) {
    this(
        communicator,
        partitionName,
        bootstrapSnapshotStore,
        commitIndexSupplier,
        termSupplier,
        DEFAULT_MAX_BATCH_SIZE);
  }

  public BootstrapSnapshotServer(
      final ClusterCommunicationService communicator,
      final String partitionName,
      final BootstrapSnapshotStore bootstrapSnapshotStore,
      final LongSupplier commitIndexSupplier,
      final LongSupplier termSupplier,
      final int maxBatchSize) {
    this.communicator = communicator;
    this.subject = subjectOf(partitionName);
    this.bootstrapSnapshotStore = bootstrapSnapshotStore;
    this.commitIndexSupplier = commitIndexSupplier;
    this.termSupplier = termSupplier;
    this.maxBatchSize = maxBatchSize;
  }

  /** 引导镜像主题名。 */
  public static String subjectOf(final String partitionName) {
    return SUBJECT_PREFIX + partitionName;
  }

  /** 注册引导请求处理器（分区 leader 角色时调用）。 */
  public void register() {
    communicator.replyTo(subject, IDENTITY, this::serve, IDENTITY);
  }

  /** 注销处理器并关闭全部会话读取器（离开 leader 或分区停止时调用；镜像删除由 RELEASE/关闭流程负责）。 */
  public void unregister() {
    communicator.unsubscribe(subject);
    sessions.values().forEach(session -> session.batcher.close());
    sessions.clear();
  }

  private CompletableFuture<byte[]> serve(final byte[] payload) {
    final var request = SnapshotTransferCodec.decodeRequest(payload);
    return switch (request.command()) {
      case BOOTSTRAP -> serveBootstrap(request.requestId());
      case PULL -> CompletableFuture.supplyAsync(() -> servePull(request.requestId()));
      case COMPLETE -> {
        closeSession(request.requestId());
        yield CompletableFuture.completedFuture(new byte[0]);
      }
      case RELEASE -> {
        release(request.requestId());
        yield CompletableFuture.completedFuture(new byte[0]);
      }
      default -> CompletableFuture.completedFuture(new byte[0]);
    };
  }

  /** 拍摄（或复用）引导镜像并登记 transferId 引用，应答信息分片。 */
  private CompletableFuture<byte[]> serveBootstrap(final String transferId) {
    final var existing = sessions.get(transferId);
    if (existing != null) {
      // 同 transferId 重试：直接重发信息分片，不重拍
      return CompletableFuture.completedFuture(infoChunk(existing.snapshotId.asString()));
    }
    final long commitIndex = commitIndexSupplier.getAsLong();
    final long term = termSupplier.getAsLong();
    if (commitIndex <= 0) {
      return CompletableFuture.failedFuture(
          new SnapshotException(
              "No committed data on partition; cannot take bootstrap snapshot"));
    }
    return bootstrapSnapshotStore
        .takeBootstrapSnapshot(commitIndex, term)
        .thenApply(
            snapshot -> {
              sessions.put(transferId, newSession(snapshot));
              LOGGER.info(
                  "Bootstrap snapshot {} taken for transfer {}",
                  snapshot.snapshotId(),
                  transferId);
              return infoChunk(snapshot.snapshotId().asString());
            })
        .toCompletableFuture();
  }

  private byte[] servePull(final String transferId) {
    final var session = sessions.get(transferId);
    if (session == null) {
      // 拍摄节点重启/清理后原会话丢失：引导节点以新 transferId 重新 BOOTSTRAP
      throw new SnapshotException(
          "No bootstrap session for transfer " + transferId + "; retry with a new transfer id");
    }
    if (!session.infoSent) {
      session.infoSent = true;
      return infoChunk(session.snapshotId.asString());
    }
    return SnapshotTransferCodec.encodeChunkBatch(session.batcher.nextBatch(maxBatchSize));
  }

  /** 引用释放：只剩当前一个请求方时真正删除镜像，否则仅移除该请求标识。 */
  private void release(final String transferId) {
    final var session = sessions.remove(transferId);
    if (session == null) {
      return;
    }
    session.batcher.close();
    final boolean lastReference =
        sessions.values().stream()
            .noneMatch(other -> other.snapshotId.equals(session.snapshotId));
    if (lastReference) {
      LOGGER.info(
          "Last reference of bootstrap snapshot {} released, deleting it", session.snapshotId);
      bootstrapSnapshotStore
          .deleteBootstrapSnapshot(session.snapshotId)
          .onComplete(
              (ignored, error) -> {
                if (error != null) {
                  LOGGER.warn(
                      "Failed to delete bootstrap snapshot {}", session.snapshotId, error);
                }
              });
    } else {
      LOGGER.debug(
          "Bootstrap snapshot {} still has other pullers, kept after releasing {}",
          session.snapshotId,
          transferId);
    }
  }

  private void closeSession(final String transferId) {
    final var session = sessions.remove(transferId);
    if (session != null) {
      session.batcher.close();
    }
  }

  private PullSession newSession(final PersistedSnapshot snapshot) {
    return new PullSession(snapshot.snapshotId(), snapshot.newChunkReader(UUID.randomUUID()));
  }

  private static byte[] infoChunk(final String snapshotId) {
    return SnapshotTransferCodec.encodeChunk(
        new SnapshotChunkImpl(snapshotId, 0, 0, 0, ByteBuffer.allocate(0), 0));
  }

  /** 拉取会话：镜像 id + 装批器 + 信息分片是否已发；一个 transferId 即镜像的一个请求方（引用）。 */
  private static final class PullSession {
    private final SnapshotId snapshotId;
    private final SnapshotChunkBatcher batcher;
    private volatile boolean infoSent;

    private PullSession(final SnapshotId snapshotId, final SnapshotChunkReader reader) {
      this.snapshotId = snapshotId;
      batcher = new SnapshotChunkBatcher(reader);
    }
  }
}
