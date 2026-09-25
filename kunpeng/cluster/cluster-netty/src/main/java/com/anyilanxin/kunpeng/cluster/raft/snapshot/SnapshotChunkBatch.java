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
package com.anyilanxin.kunpeng.cluster.raft.snapshot;

import java.util.List;

/**
 * 一次传输单元：{@link TransferKind#BATCH_FILES} 时为多个完整小文件（每个条目就是一整个文件）； {@link TransferKind#FILE_CHUNKS}
 * 时为某个大文件的连续分片，当前列表恒为单个分片。
 *
 * @param kind 传输单元类型
 * @param chunks 分片列表，按条目顺序排列
 * @param hasMore 本单元之后是否还有更多传输单元
 */
public record SnapshotChunkBatch(TransferKind kind, List<SnapshotChunk> chunks, boolean hasMore) {

  public SnapshotChunkBatch {
    chunks = List.copyOf(chunks);
  }
}
