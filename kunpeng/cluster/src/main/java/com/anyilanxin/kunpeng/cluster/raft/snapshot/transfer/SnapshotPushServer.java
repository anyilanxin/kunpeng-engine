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

import com.anyilanxin.kunpeng.cluster.cluster.messaging.ClusterCommunicationService;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.PersistableSnapshot;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.ReceivableSnapshotStore;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotChunk;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotChunkAppender;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotException;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.impl.SnapshotChunkImpl;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import org.agrona.concurrent.UnsafeBuffer;

/**
 * 合并快照推送接收端：目标分区 leader 注册本处理器，接收源分区 leader 主动推来的批量分片。
 *
 * <p>首个请求按快照 id 创建接收式 pending，逐批经 {@link SnapshotChunkAppender} 批量缓冲写入；
 * 末批触发完整性校验与提交；任一批失败即中止该会话并清理临时目录。
 */
public final class SnapshotPushServer {

  /** 合并推送主题的统一前缀。 */
  static final String SUBJECT_PREFIX = "snapshot-merge-push-";

  private final ClusterCommunicationService communicator;
  private final String subject;
  private final ReceivableSnapshotStore store;
  // 进行中的接收会话：快照 id → pending + 写入器（源侧逐批推进，同一时刻每 id 至多一个会话）
  private final Map<String, Receiving> sessions = new ConcurrentHashMap<>();

  private record Receiving(PersistableSnapshot pending, SnapshotChunkAppender appender) {}

  public SnapshotPushServer(
      final ClusterCommunicationService communicator,
      final String partitionName,
      final ReceivableSnapshotStore store) {
    this.communicator = communicator;
    this.subject = subjectOf(partitionName);
    this.store = store;
  }

  /** 合并推送主题名。 */
  public static String subjectOf(final String partitionName) {
    return SUBJECT_PREFIX + partitionName;
  }

  /** 注册本分区的合并推送接收处理器（目标分区 leader 角色时调用）。 */
  public void register() {
    communicator.replyTo(
        subject, SnapshotPushRequest::decode, this::serve, SnapshotPushAck::encode);
  }

  /** 注销接收处理器（离开 leader 或分区停止时调用）。 */
  public void unregister() {
    communicator.unsubscribe(subject);
    sessions.values().forEach(receiving -> receiving.pending().abort());
    sessions.clear();
  }

  private CompletableFuture<SnapshotPushAck> serve(final SnapshotPushRequest request) {
    return CompletableFuture.supplyAsync(() -> serveSync(request));
  }

  private SnapshotPushAck serveSync(final SnapshotPushRequest request) {
    final Receiving receiving =
        sessions.computeIfAbsent(
            request.getSnapshotId(),
            id -> {
              try {
                final var pending = store.newReceivedSnapshot(id).join();
                return new Receiving(pending, SnapshotChunkAppender.of(pending));
              } catch (final Exception e) {
                throw new SnapshotException(
                    "Failed to create receiving snapshot " + id + " for merge push", e);
              }
            });
    try {
      for (final byte[] frame : request.getChunkFrames()) {
        receiving.appender().append(parseChunk(frame)).join();
      }
      if (!request.hasMore()) {
        sessions.remove(request.getSnapshotId());
        receiving.appender().verifyComplete();
        receiving.pending().persist().toCompletableFuture().join();
      }
      return SnapshotPushAck.ok();
    } catch (final Exception e) {
      sessions.remove(request.getSnapshotId());
      receiving.pending().abort();
      return SnapshotPushAck.fail(
          "Merge push of snapshot " + request.getSnapshotId() + " failed: " + e.getMessage());
    }
  }

  private SnapshotChunkImpl parseChunk(final byte[] frame) {
    final var chunk = new SnapshotChunkImpl();
    if (!chunk.tryWrap(new UnsafeBuffer(frame))) {
      throw new SnapshotException("Received a malformed snapshot chunk frame");
    }
    return chunk;
  }
}
