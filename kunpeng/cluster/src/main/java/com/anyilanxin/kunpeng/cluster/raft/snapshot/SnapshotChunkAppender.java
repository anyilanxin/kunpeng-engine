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
package com.anyilanxin.kunpeng.cluster.raft.snapshot;

import java.util.concurrent.CompletableFuture;

/**
 * 接收侧分片写入器：把单个 {@link SnapshotChunk} 按 {@code 文件名@字节偏移} 写入
 * pending 快照目录，校验 checksum 并跟踪 totalCount 一致性。
 *
 * <p>供 PassiveRole（install 逐块推送）与跨分区传输客户端（批量拉取）使用；通过
 * {@link #of(PersistableSnapshot)} 获取：pending 快照自实现本接口时直接复用，否则退化为
 * 基于 {@link PersistableSnapshot#getPath()} 的文件写入实现。
 */
public interface SnapshotChunkAppender {

  /**
   * 为给定 pending 快照获取分片写入器：自实现者直接返回自身，否则返回文件写入实现。
   */
  static SnapshotChunkAppender of(final PersistableSnapshot snapshot) {
    if (snapshot instanceof SnapshotChunkAppender appender) {
      return appender;
    }
    return new com.anyilanxin.kunpeng.cluster.raft.snapshot.impl.FileSnapshotChunkAppender(
        snapshot);
  }

  /**
   * 校验并缓冲单个分片：checksum 不符、chunkName 非法或 totalCount 与首片不一致时以异常完成。
   * 实现可按累计字节批量写盘（见 {@link #flush()}）。
   */
  CompletableFuture<Void> append(SnapshotChunk chunk);

  /**
   * 把尚未落盘的缓冲分片整批写入并 force；完成/提交前必须调用。立即写实现为空操作。
   */
  default void flush() {}

  /**
   * 校验分片完整性：实际接收到的不同分片数必须等于分片声明的 totalCount。
   *
   * @throws SnapshotException if the received chunk count does not match
   */
  void verifyComplete();
}
