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

import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotChunk;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotChunkBatch;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotChunkReader;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.TransferKind;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 分片装批器：把读取器吐出的分片贪心装批——整文件分片装入 {@link TransferKind#BATCH_FILES} 批 （累计字节 ≤ maxBatchSize），大文件分片独占一个
 * {@link TransferKind#FILE_CHUNKS} 批。 装不下的分片回读槽暂存，供下一批优先发出。
 *
 * <p>同时供传输模块与 raft install 链路使用；install 通过 {@link #seek}/{@link #reset} 支持 分片续传，通过 {@link
 * #nextChunkName()} 取得本批之后的下一个分片名作为续传 token。
 *
 * @author zxuanhong
 * @since 1.0.0
 */
public final class SnapshotChunkBatcher {

  private final SnapshotChunkReader reader;
  private SnapshotChunk lookahead;

  public SnapshotChunkBatcher(final SnapshotChunkReader reader) {
    this.reader = reader;
  }

  /** 是否还有未发出的分片。 */
  public boolean hasMore() {
    return lookahead != null || reader.hasNext();
  }

  /** 装下一批；无更多分片时返回空批（hasMore=false）。 */
  public SnapshotChunkBatch nextBatch(final int maxBatchSize) {
    final List<SnapshotChunk> chunks = new ArrayList<>();
    long bytes = 0;
    SnapshotChunk chunk = take();
    while (chunk != null) {
      final boolean wholeFile =
          chunk.getOffset() == 0 && chunk.getLength() == chunk.getTotalLength();
      if (!wholeFile) {
        if (chunks.isEmpty()) {
          return new SnapshotChunkBatch(TransferKind.FILE_CHUNKS, List.of(chunk), hasMore());
        }
        lookahead = chunk;
        break;
      }
      if (!chunks.isEmpty() && bytes + chunk.getLength() > maxBatchSize) {
        lookahead = chunk;
        break;
      }
      chunks.add(chunk);
      bytes += chunk.getLength();
      chunk = take();
    }
    return new SnapshotChunkBatch(TransferKind.BATCH_FILES, chunks, hasMore());
  }

  /** 定位到底层读取器的指定分片（丢弃回读槽），供失败重发时回退。 */
  public void seek(final ByteBuffer chunkId) {
    lookahead = null;
    reader.seek(chunkId);
  }

  /** 重置到底层读取器的第一个分片（丢弃回读槽）。 */
  public void reset() {
    lookahead = null;
    reader.reset();
  }

  /** 本批之后下一个待发分片的名称（无更多分片时返回 null）。 */
  public String nextChunkName() {
    if (lookahead != null) {
      return lookahead.getChunkName();
    }
    final ByteBuffer id = reader.nextId();
    return id == null ? null : StandardCharsets.UTF_8.decode(id.slice()).toString();
  }

  /** 调整底层读取器的分片尺寸（丢弃回读槽，因为重建会改变分片边界）。 */
  public void setMaximumChunkSize(final int maximumChunkSize) {
    lookahead = null;
    reader.setMaximumChunkSize(maximumChunkSize);
  }

  private SnapshotChunk take() {
    if (lookahead != null) {
      final var chunk = lookahead;
      lookahead = null;
      return chunk;
    }
    return reader.hasNext() ? reader.next() : null;
  }

  /** 关闭底层读取器。 */
  public void close() {
    reader.close();
  }
}
