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
package com.anyilanxin.kunpeng.cluster.raft.snapshot.transfer;

import com.anyilanxin.kunpeng.cluster.cluster.PartitionId;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.ClusterCommunicationService;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.PersistedSnapshot;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotChunk;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotChunkBatch;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotException;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotId;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotTransferCodec;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.receive.ReceiveSnapshotStore;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.receive.ReceivedSnapshot;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * 合并快照接收端：目标分区 leader 注册本处理器，接收源分区 leader 推来的合并镜像分片， 落地到目标分区 {@code
 * snapshots/merge} 后触发合并流，并应答源分区的合并完成等待请求。
 *
 * <p>推送协议（纯 {@link SnapshotChunkBatch}，与 {@link DefaultSnapshotTransfer#pushSnapshot} 对应）：
 *
 * <ul>
 *   <li>首条消息为信息批：单个分片，chunkName 承载镜像 id、content 承载源分区标识（{@link
 *       PartitionId#toString()} 格式，目标端据此记录"哪个分区合并进了本分区"）； 据此经 {@link
 *       ReceiveSnapshotStore#newReceivedSnapshot} 建接收 pending；
 *   <li>后续消息为内容批，逐批 {@code write}；
 *   <li>{@code hasMore=false} 的末批写完即 {@code persist} 提交，随后异步触发 {@link MergeFlowRunner}
 *       合并流（两阶段安装 → 业务合并 → 追加合并记录条目，不触发 follower 安装——由调度侧全部合并完成后统一收尾）。
 * </ul>
 *
 * <p>完成等待协议（{@code snapshot-merge-await-}{分区名}，一问一答）：payload 为镜像 id（UTF-8）， 目标端对应合并流完成后应答；
 * 未知镜像 id（重启清理后）异常应答，源分区整体重推。
 *
 * <p>合并推送一次只进行一个会话（单会话覆盖旧会话，支持整体重推）。
 *
 * @author zxuanhong
 * @since 1.0.0
 */
public final class SnapshotPushServer {

  /** 合并推送主题前缀：{@value #SUBJECT_PREFIX}{分区名}。 */
  static final String SUBJECT_PREFIX = "snapshot-merge-push-";

  /** 合并完成等待主题前缀：{@value #AWAIT_SUBJECT_PREFIX}{分区名}。 */
  static final String AWAIT_SUBJECT_PREFIX = "snapshot-merge-await-";

  private static final Function<byte[], byte[]> IDENTITY = Function.identity();

  private final ClusterCommunicationService communicator;
  private final String subject;
  private final String awaitSubject;
  private final ReceiveSnapshotStore store;
  // 合并流触发器：末批提交后调用，返回的 future 完成即整个合并流完成
  private final MergeFlowRunner mergeFlowRunner;
  private final AtomicReference<PushSession> session = new AtomicReference<>();
  // 镜像 id → 合并流完成 future（完成等待请求据此应答）
  private final Map<String, CompletableFuture<Void>> pendingMerges = new ConcurrentHashMap<>();

  /** 合并流触发器：接收完成后执行目标端合并全流程。 */
  @FunctionalInterface
  public interface MergeFlowRunner {

    /**
     * @param received 接收完成的合并镜像
     * @param sourcePartition 源分区（数据从该分区合并进本分区）
     * @return 合并流完成 future（异常完成即本次合并失败）
     */
    CompletableFuture<Void> run(PersistedSnapshot received, PartitionId sourcePartition);
  }

  private record PushSession(ReceivedSnapshot pending, PartitionId sourcePartition) {}

  public SnapshotPushServer(
      final ClusterCommunicationService communicator,
      final String partitionName,
      final ReceiveSnapshotStore store,
      final MergeFlowRunner mergeFlowRunner) {
    this.communicator = communicator;
    this.subject = subjectOf(partitionName);
    this.awaitSubject = awaitSubjectOf(partitionName);
    this.store = store;
    this.mergeFlowRunner = mergeFlowRunner;
  }

  /** 合并推送主题名。 */
  public static String subjectOf(final String partitionName) {
    return SUBJECT_PREFIX + partitionName;
  }

  /** 合并完成等待主题名。 */
  public static String awaitSubjectOf(final String partitionName) {
    return AWAIT_SUBJECT_PREFIX + partitionName;
  }

  /** 注册合并推送接收与完成等待处理器（目标分区 leader 角色时调用）。 */
  public void register() {
    communicator.replyTo(subject, IDENTITY, this::serve, IDENTITY);
    communicator.replyTo(awaitSubject, IDENTITY, this::serveAwait, IDENTITY);
  }

  /** 注销处理器（离开 leader 或分区停止时调用）。 */
  public void unregister() {
    communicator.unsubscribe(subject);
    communicator.unsubscribe(awaitSubject);
    abortSession();
    pendingMerges.clear();
  }

  private CompletableFuture<byte[]> serve(final byte[] payload) {
    final SnapshotChunkBatch batch;
    try {
      batch = SnapshotTransferCodec.decodeChunkBatch(payload);
    } catch (final Exception e) {
      return CompletableFuture.failedFuture(e);
    }
    return CompletableFuture.supplyAsync(() -> serveSync(batch));
  }

  private byte[] serveSync(final SnapshotChunkBatch batch) {
    if (batch.chunks().isEmpty()) {
      return new byte[0];
    }
    final SnapshotChunk first = batch.chunks().get(0);
    // 信息批判定：单分片 + totalLength=0 + chunkName 为合法镜像 id（内容分片名含 '@' 被排除）
    final boolean infoBatch =
        batch.chunks().size() == 1 && first.getTotalLength() == 0 && isInfoChunkName(first.getChunkName());
    try {
      if (infoBatch) {
        final String snapshotId = first.getChunkName();
        final PartitionId sourcePartition = parseSourcePartition(first);
        final ReceivedSnapshot pending = store.newReceivedSnapshot(snapshotId).join();
        // 覆盖旧会话：合并推送一次一个，整体重推时旧 pending 作废
        abortSession();
        session.set(new PushSession(pending, sourcePartition));
      } else {
        final var current = session.get();
        if (current == null) {
          throw new SnapshotException("No push session for incoming snapshot chunks");
        }
        current.pending().write(batch).join();
        if (!batch.hasMore()) {
          session.set(null);
          final var persisted = current.pending().persist().join();
          triggerMergeFlow(persisted, current.sourcePartition());
        }
      }
      return new byte[0];
    } catch (final Exception e) {
      abortSession();
      throw new SnapshotException("Merge push failed: " + e.getMessage(), e);
    }
  }

  /** 信息批 content 承载源分区标识（{@link PartitionId#toString()} 格式），缺失/非法即推送协议错误。 */
  private PartitionId parseSourcePartition(final SnapshotChunk info) {
    final var content = info.getContent();
    final byte[] bytes = new byte[content.remaining()];
    content.get(bytes);
    final String value = new String(bytes, StandardCharsets.UTF_8);
    if (value.isEmpty()) {
      throw new SnapshotException("Merge push info batch carries no source partition");
    }
    return PartitionId.parse(value);
  }

  /** 末批提交后触发合并流：登记 pending future 再异步执行，完成等待请求据此应答。 */
  private void triggerMergeFlow(final PersistedSnapshot persisted, final PartitionId sourcePartition) {
    final var mergeFuture = new CompletableFuture<Void>();
    pendingMerges.put(persisted.snapshotId().asString(), mergeFuture);
    mergeFlowRunner
        .run(persisted, sourcePartition)
        .whenComplete(
            (v, error) -> {
              pendingMerges.remove(persisted.snapshotId().asString());
              if (error != null) {
                mergeFuture.completeExceptionally(error);
              } else {
                mergeFuture.complete(null);
              }
            });
  }

  /** 完成等待应答：对应镜像的合并流完成（或已无记录时立即失败）后回复。 */
  private CompletableFuture<byte[]> serveAwait(final byte[] payload) {
    final String snapshotId = new String(payload, StandardCharsets.UTF_8);
    final var mergeFuture = pendingMerges.get(snapshotId);
    if (mergeFuture == null) {
      // 目标端重启/清理后原合并不再推进：源分区整体重推
      return CompletableFuture.failedFuture(
          new SnapshotException("No merge in progress for snapshot " + snapshotId));
    }
    return mergeFuture.thenApply(ignored -> new byte[0]);
  }

  /** info 批的 chunkName 承载镜像 id；含 '@' 的是内容分片名（文件名@偏移），直接排除后再校验 id 格式。 */
  static boolean isInfoChunkName(final String chunkName) {
    if (chunkName == null || chunkName.indexOf('@') >= 0) {
      return false;
    }
    try {
      SnapshotId.fromString(chunkName);
      return true;
    } catch (final IllegalArgumentException e) {
      return false;
    }
  }

  private void abortSession() {
    final var current = session.getAndSet(null);
    if (current != null) {
      current.pending().abort();
    }
  }
}
