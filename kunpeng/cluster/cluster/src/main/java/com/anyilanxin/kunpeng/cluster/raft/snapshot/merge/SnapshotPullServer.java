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
package com.anyilanxin.kunpeng.cluster.raft.snapshot.merge;

import com.anyilanxin.kunpeng.cluster.cluster.messaging.ClusterCommunicationService;
import com.anyilanxin.kunpeng.cluster.raft.partition.RaftPartition;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.PersistedSnapshot;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotChunkReader;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotException;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotId;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotTransferCodec;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.constructable.TransferSnapshotProvider;
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
 * 传输镜像拉取服务端（合并迁移的拉模式）：目标分区经 {@code snapshot-pull-<分区名>} 主题请求本分区（源分区）拍摄合并镜像， 本服务在 merge 存储拍摄镜像并登记
 * transferId 引用，随后应答信息分片、逐批回传； 请求方 COMPLETE 结束读取、RELEASE 释放引用，引用归零时删除镜像。
 *
 * <p>拍摄统一经 {@link TransferSnapshotProvider#takeMergeSnapshot} 生成。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class SnapshotPullServer {

  /** 拉取主题前缀：{@value #SUBJECT_PREFIX}{分区名}。 */
  static final String SUBJECT_PREFIX = "snapshot-pull-";

  /** 合并结果通知主题前缀：{@value #RESULT_SUBJECT_PREFIX}{分区名}——获取方合并终止后回告提供方。 */
  static final String RESULT_SUBJECT_PREFIX = "snapshot-pull-result-";

  private static final int DEFAULT_MAX_BATCH_SIZE = 1024 * 1024;
  private static final Function<byte[], byte[]> IDENTITY = Function.identity();
  private static final Logger LOGGER = LoggerFactory.getLogger(SnapshotPullServer.class);

  private final ClusterCommunicationService communicator;
  private final String subject;
  private final String resultSubject;
  private final MergeSnapshotStore mergeSnapshotStore;
  private final RaftPartition partition;
  private final LongSupplier commitIndexSupplier;
  private final LongSupplier termSupplier;
  private final int maxBatchSize;
  // transferId → 拉取会话（引用计数的最小单元：一个 transferId 即一个请求方）
  private final Map<String, PullSession> sessions = new ConcurrentHashMap<>();

  public SnapshotPullServer(
      final ClusterCommunicationService communicator,
      final String partitionName,
      final MergeSnapshotStore mergeSnapshotStore,
      final RaftPartition partition,
      final LongSupplier commitIndexSupplier,
      final LongSupplier termSupplier,
      final int maxBatchSize) {
    this.communicator = communicator;
    this.subject = subjectOf(partitionName);
    this.resultSubject = resultSubjectOf(partitionName);
    this.mergeSnapshotStore = mergeSnapshotStore;
    this.partition = partition;
    this.commitIndexSupplier = commitIndexSupplier;
    this.termSupplier = termSupplier;
    this.maxBatchSize = maxBatchSize;
  }

  /** 拉取主题名。 */
  public static String subjectOf(final String partitionName) {
    return SUBJECT_PREFIX + partitionName;
  }

  /** 合并结果通知主题名（获取方合并流终止后回告提供方）。 */
  public static String resultSubjectOf(final String partitionName) {
    return RESULT_SUBJECT_PREFIX + partitionName;
  }

  /** 注册拉取请求处理器（分区 leader 角色时调用）。 */
  public void register() {
    communicator.replyTo(subject, IDENTITY, this::serve, IDENTITY);
    communicator.replyTo(resultSubject, IDENTITY, this::serveResult, IDENTITY);
  }

  /** 注销处理器并关闭全部会话读取器（离开 leader 或分区停止时调用；镜像删除由 RELEASE/关闭流程负责）。 */
  public void unregister() {
    communicator.unsubscribe(subject);
    communicator.unsubscribe(resultSubject);
    sessions.values().forEach(session -> session.batcher.close());
    sessions.clear();
  }

  private CompletableFuture<byte[]> serve(final byte[] payload) {
    final var request = SnapshotTransferCodec.decodeRequest(payload);
    return switch (request.command()) {
      case BOOTSTRAP -> serveTransferBegin(request.requestId(), request.decodeParameters());
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

  /**
   * 拍摄合并镜像并登记 transferId 引用，应答信息分片。 信息分片随本应答已发出（{@code infoSent} 置位），请求方随后的 PULL 从首个批开始——否则首个 PULL
   * 会再收到一遍信息分片， 与请求方的批解码错位（Unexpected template id）。
   */
  private CompletableFuture<byte[]> serveTransferBegin(
      final String transferId, final Map<String, String> parameters) {
    final var existing = sessions.get(transferId);
    if (existing != null) {
      // 同 transferId 重试：直接重发信息分片，不重拍
      existing.infoSent = true;
      return CompletableFuture.completedFuture(infoChunk(existing.snapshotId.asString()));
    }
    final long commitIndex = commitIndexSupplier.getAsLong();
    final long term = termSupplier.getAsLong();
    if (commitIndex <= 0) {
      return CompletableFuture.failedFuture(
          new SnapshotException("No committed data on partition; cannot take merge snapshot"));
    }
    // 拍摄合并镜像前先拍一次 raft 正式镜像（一致视图来源）
    final CompletableFuture<Void> preTake =
        partition == null
            ? CompletableFuture.completedFuture(null)
            : partition.takeSnapshot().toCompletableFuture();
    return preTake
        .thenCompose(
            ignored ->
                mergeSnapshotStore
                    .takeMergeSnapshot(commitIndex, term, parameters)
                    .toCompletableFuture())
        .thenApply(
            snapshot -> {
              final PullSession session = newSession(snapshot);
              session.infoSent = true;
              sessions.put(transferId, session);
              LOGGER.info(
                  "Merge snapshot {} taken for pull transfer {}",
                  snapshot.snapshotId(),
                  transferId);
              return infoChunk(snapshot.snapshotId().asString());
            });
  }

  private byte[] servePull(final String transferId) {
    final var session = sessions.get(transferId);
    if (session == null) {
      // 拍摄节点重启/清理后原会话丢失：请求方以新 transferId 重新发起
      throw new SnapshotException(
          "No pull session for transfer " + transferId + "; retry with a new transfer id");
    }
    if (!session.infoSent) {
      session.infoSent = true;
      return infoChunk(session.snapshotId.asString());
    }
    return SnapshotTransferCodec.encodeChunkBatch(session.batcher.nextBatch(maxBatchSize));
  }

  /**
   * 合并结果通知处理（获取方合并流终止后回告）：成功时先按本次拉取会话（transferId）释放引用—— 仅当该拍摄镜像已无其他引用时才真正删除；
   * 失败保留供获取方重拉复用。释放完成后回调提供方业务（{@code TransferSnapshotProvider#mergeSnapshotResult}，成功失败都回调）。
   */
  private CompletableFuture<byte[]> serveResult(final byte[] payload) {
    final SnapshotTransferCodec.MergeResult result;
    try {
      result = SnapshotTransferCodec.decodeMergeResult(payload);
    } catch (final Exception e) {
      return CompletableFuture.failedFuture(e);
    }
    if (result.result()) {
      release(result.transferId());
    }
    final var provider = partition.getTransferSnapshotProvider();
    provider
        .mergeSnapshotResult(result.result(), result.message(), null, result.parameters())
        .onComplete(
            (ignored, error) -> {
              if (error != null) {
                LOGGER.warn(
                    "Merge result callback failed on provider side for transfer {}",
                    result.transferId(),
                    error);
              }
            });
    return CompletableFuture.completedFuture(new byte[0]);
  }

  /** 引用释放：只剩当前一个请求方时真正删除镜像，否则仅移除该请求标识。 */
  private void release(final String transferId) {
    final var session = sessions.remove(transferId);
    if (session == null) {
      return;
    }
    session.batcher.close();
    final boolean lastReference =
        sessions.values().stream().noneMatch(other -> other.snapshotId.equals(session.snapshotId));
    if (lastReference) {
      LOGGER.info("Last reference of merge snapshot {} released, deleting it", session.snapshotId);
      mergeSnapshotStore
          .deleteSnapshot(session.snapshotId)
          .onComplete(
              (ignored, error) -> {
                if (error != null) {
                  LOGGER.warn("Failed to delete merge snapshot {}", session.snapshotId, error);
                }
              });
    } else {
      LOGGER.debug(
          "Transfer snapshot {} still has other pullers, kept after releasing {}",
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
