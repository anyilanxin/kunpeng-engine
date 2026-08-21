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
import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotType;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.impl.SnapshotChunkImpl;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 跨分区快照传输服务端：按分区订阅拉取主题，把本分区指定类型的最新快照以批量分片流应答。
 *
 * <p>每次请求返回一批分片（按文件字典序、文件内偏移序贪心装批，累计字节 ≤ maxBatchSize，单文件
 * 不限大小可跨批）与是否还有后续的标志；客户端通过"上批末片名"逐批推进，服务端无传输会话状态，
 * 天然支持失败重拉。
 */
public final class SnapshotTransferServer {

  /** 拉取主题的统一前缀。 */
  static final String SUBJECT_PREFIX = "snapshot-transfer-";
  /** 缺省批量分片累计字节上限（4 MiB）。 */
  static final int DEFAULT_MAX_BATCH_SIZE = 4 * 1024 * 1024;

  private final ClusterCommunicationService communicator;
  private final String subject;
  private final ReceivableSnapshotStore store;
  private final int maxBatchSize;

  public SnapshotTransferServer(
      final ClusterCommunicationService communicator,
      final String partitionName,
      final ReceivableSnapshotStore store) {
    this(communicator, partitionName, store, DEFAULT_MAX_BATCH_SIZE);
  }

  public SnapshotTransferServer(
      final ClusterCommunicationService communicator,
      final String partitionName,
      final ReceivableSnapshotStore store,
      final int maxBatchSize) {
    this.communicator = communicator;
    this.subject = subjectOf(partitionName);
    this.store = store;
    this.maxBatchSize = Math.max(1, maxBatchSize);
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
    final PersistedSnapshot snapshot = latestOf(request.getType());

    try (final SnapshotChunkReader reader = snapshot.newChunkReader()) {
      final int chunkSize = Math.max(1, request.getPreferredChunkSize());
      reader.setMaximumChunkSize(chunkSize);
      if (request.getAfterChunkName() != null) {
        // 续传：定位到上批末片并跳过，从其后的分片开始装批
        reader.seek(chunkIdOf(request.getAfterChunkName()));
        reader.next();
      }
      if (!reader.hasNext()) {
        return SnapshotTransferResponse.end();
      }
      return fillBatch(reader, maxBatchSize);
    }
  }

  /** 按请求类型查询本分区最新的对应类型快照。 */
  private PersistedSnapshot latestOf(final SnapshotType type) {
    return switch (type) {
      case REGULAR -> store.getLatestSnapshot()
          .map(PersistedSnapshot.class::cast)
          .orElseThrow(() -> noSnapshotOf(type));
      case BOOTSTRAP -> store.getBootstrapSnapshot()
          .map(PersistedSnapshot.class::cast)
          .orElseThrow(() -> noSnapshotOf(type));
      case MERGE -> store.getMergeSnapshot()
          .map(PersistedSnapshot.class::cast)
          .orElseThrow(() -> noSnapshotOf(type));
    };
  }

  private static SnapshotException noSnapshotOf(final SnapshotType type) {
    return new SnapshotException("No snapshot of type " + type + " on this partition");
  }

  /**
   * 贪心装批：依次读取分片，累计字节将超限且批非空时回退（seek 到当前分片）封批发送；
   * 首片总是入批，保证进度不停滞。供拉取服务端与推送客户端共用。
   */
  static SnapshotTransferResponse fillBatch(
      final SnapshotChunkReader reader, final int maxBatchSize) {
    final List<byte[]> frames = new ArrayList<>();
    final List<String> names = new ArrayList<>();
    long accumulatedBytes = 0;
    while (reader.hasNext()) {
      final SnapshotChunk chunk = reader.next();
      final byte[] frame = new SnapshotChunkImpl(chunk).toByteBuffer().array();
      if (!frames.isEmpty() && accumulatedBytes + frame.length > maxBatchSize) {
        // 本片留待下一批：reader 回退到本片开头后结束本批
        reader.seek(chunkIdOf(chunk.getChunkName()));
        break;
      }
      frames.add(frame);
      names.add(chunk.getChunkName());
      accumulatedBytes += frame.length;
    }
    // 上面 next() 已推进越过末片，hasNext 即为是否还有后续批
    final boolean hasMore = reader.hasNext();
    return SnapshotTransferResponse.of(names.get(names.size() - 1), frames, hasMore);
  }

  private static ByteBuffer chunkIdOf(final String chunkName) {
    return ByteBuffer.wrap(chunkName.getBytes(StandardCharsets.UTF_8));
  }
}
