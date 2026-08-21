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
import com.anyilanxin.kunpeng.cluster.raft.snapshot.PersistedSnapshot;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.ReceivableSnapshotStore;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotChunk;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotChunkReader;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotException;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.impl.SnapshotChunkImpl;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

/**
 * 跨分区快照传输服务端：按分区订阅拉取主题，把本分区指定类型的最新快照以分片流形式应答。
 *
 * <p>每次请求返回一个分片（自描述帧）与是否还有后续的标志，客户端通过"续传分片名"逐块推进，
 * 服务端无传输会话状态，天然支持失败重拉。
 */
public final class SnapshotTransferServer {

  /** 拉取主题的统一前缀。 */
  static final String SUBJECT_PREFIX = "snapshot-transfer-";

  private final ClusterCommunicationService communicator;
  private final String subject;
  private final ReceivableSnapshotStore store;

  public SnapshotTransferServer(
      final ClusterCommunicationService communicator,
      final String partitionName,
      final ReceivableSnapshotStore store) {
    this.communicator = communicator;
    this.subject = subjectOf(partitionName);
    this.store = store;
  }

  /** 拉取主题名。 */
  public static String subjectOf(final String partitionName) {
    return SUBJECT_PREFIX + partitionName;
  }

  /** 注册本分区的快照拉取处理器。 */
  public void register() {
    communicator.replyTo(
        subject, SnapshotTransferRequest::decode, this::serve, SnapshotTransferResponse::encode);
  }

  /** 注销拉取处理器（分区停止时调用）。 */
  public void unregister() {
    communicator.unsubscribe(subject);
  }

  private CompletableFuture<SnapshotTransferResponse> serve(final SnapshotTransferRequest request) {
    return CompletableFuture.supplyAsync(() -> serveSync(request));
  }

  private SnapshotTransferResponse serveSync(final SnapshotTransferRequest request) {
    final PersistedSnapshot snapshot =
        store
            .getLatestSnapshot(request.getType())
            .orElseThrow(
                () ->
                    new SnapshotException(
                        "No snapshot of type " + request.getType() + " on this partition"));

    try (final SnapshotChunkReader reader = snapshot.newChunkReader()) {
      final int chunkSize = Math.max(1, request.getPreferredChunkSize());
      reader.setMaximumChunkSize(chunkSize);
      if (request.getAfterChunkName() != null) {
        // 续传：定位到上次的分片并跳过，返回其后的下一个分片
        reader.seek(chunkIdOf(request.getAfterChunkName()));
        reader.next();
      }
      if (!reader.hasNext()) {
        return SnapshotTransferResponse.end();
      }
      final SnapshotChunk chunk = reader.next();
      final byte[] frame = new SnapshotChunkImpl(chunk).toByteBuffer().array();
      return SnapshotTransferResponse.of(frame, reader.hasNext());
    }
  }

  private static ByteBuffer chunkIdOf(final String chunkName) {
    return ByteBuffer.wrap(chunkName.getBytes(StandardCharsets.UTF_8));
  }
}
