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

/** 可接收式快照存储：在本地拍摄之外，支持按分片接收 leader 或跨分区传来的快照。 */
public interface ReceivableSnapshotStore extends PersistedSnapshotStore {

  /**
   * 为给定快照 id（{@code index-term-hex(nodeId)}）创建待接收快照的临时目录，返回其 pending
   * 句柄；分片经 {@link SnapshotChunkAppender#of(PersistableSnapshot)} 逐片写入、校验，直到
   * {@link PersistableSnapshot#persist()} 提交。
   *
   * @param snapshotId the id of the snapshot to receive
   * @return a future completed with the pending snapshot
   * @throws SnapshotException.SnapshotAlreadyExistsException if an identical snapshot exists
   */
  CompletableFuture<PersistableSnapshot> newReceivedSnapshot(String snapshotId);
}
