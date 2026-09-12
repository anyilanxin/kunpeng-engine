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
import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotChunk;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotChunkBatch;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotException;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotId;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotTransferCodec;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.receive.ReceiveSnapshotStore;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.receive.ReceivedSnapshot;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * 合并推送接收端：目标分区 leader 注册本处理器，接收源分区 leader 推来的传输单元。
 *
 * <p>协议（纯 {@link SnapshotChunkBatch}，与 {@link DefaultSnapshotTransfer#pushSnapshot} 对应）：
 *
 * <ul>
 *   <li>首条消息为信息批：单个分片，chunkName 承载镜像 id、content 为空（totalLength=0）； 据此经 {@link
 *       ReceiveSnapshotStore#newReceivedSnapshot} 建接收 pending；
 *   <li>后续消息为内容批，逐批 {@code write}；
 *   <li>{@code hasMore=false} 的末批写完即 {@code persist} 提交。
 * </ul>
 *
 * 合并推送一次只进行一个会话（单会话覆盖旧会话，支持整体重推）。
 *
 * @author zxuanhong
 * @since 1.0.0
 */
public final class SnapshotPushServer {

  /** 合并推送主题前缀：{@value #SUBJECT_PREFIX}{分区名}。 */
  static final String SUBJECT_PREFIX = "snapshot-merge-push-";

  private static final Function<byte[], byte[]> IDENTITY = Function.identity();

  private final ClusterCommunicationService communicator;
  private final String subject;
  private final ReceiveSnapshotStore store;
  private final AtomicReference<PushSession> session = new AtomicReference<>();

  private record PushSession(ReceivedSnapshot pending) {}

  public SnapshotPushServer(
      final ClusterCommunicationService communicator,
      final String partitionName,
      final ReceiveSnapshotStore store) {
    this.communicator = communicator;
    subject = subjectOf(partitionName);
    this.store = store;
  }

  /** 合并推送主题名。 */
  public static String subjectOf(final String partitionName) {
    return SUBJECT_PREFIX + partitionName;
  }

  /** 注册本分区的合并推送接收处理器（目标分区 leader 角色时调用）。 */
  public void register() {
    communicator.replyTo(subject, IDENTITY, this::serve, IDENTITY);
  }

  /** 注销接收处理器（离开 leader 或分区停止时调用）。 */
  public void unregister() {
    communicator.unsubscribe(subject);
    abortSession();
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
    final boolean infoBatch =
        batch.chunks().size() == 1
            && first.getTotalLength() == 0
            && first.getLength() == 0
            && isInfoChunkName(first.getChunkName());
    try {
      if (infoBatch) {
        final String snapshotId = first.getChunkName();
        final ReceivedSnapshot pending = store.newReceivedSnapshot(snapshotId).join();
        // 覆盖旧会话：合并推送一次一个，整体重推时旧 pending 作废
        abortSession();
        session.set(new PushSession(pending));
      } else {
        final var current = session.get();
        if (current == null) {
          throw new SnapshotException("No push session for incoming snapshot chunks");
        }
        current.pending().write(batch).join();
        if (!batch.hasMore()) {
          session.set(null);
          current.pending().persist().join();
        }
      }
      return new byte[0];
    } catch (final Exception e) {
      abortSession();
      throw new SnapshotException("Merge push failed: " + e.getMessage(), e);
    }
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
