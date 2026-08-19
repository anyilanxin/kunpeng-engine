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
package com.anyilanxin.kunpeng.cluster.raft.journal.snapshots;

import java.util.concurrent.CompletableFuture;

/** 可接收远端快照的存储（Follower 侧） */
public interface ReceivableSnapshotStore extends PersistedSnapshotStore {

  /** 为指定快照 ID 建立接收会话 */
  ReceivedSnapshot newReceivedSnapshot(String snapshotId);

  /** 中止全部未完成的快照（拍摄中/接收中） */
  CompletableFuture<Void> purgePendingSnapshots();

  /** 注册快照事件监听 */
  void addSnapshotListener(PersistedSnapshotListener listener);

  /** 移除快照事件监听 */
  void removeSnapshotListener(PersistedSnapshotListener listener);
}
