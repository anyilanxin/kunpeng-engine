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
package com.anyilanxin.kunpeng.cluster.raft.snapshot.impl;

import com.anyilanxin.kunpeng.utils.scheduler.AsyncClosable;
import com.anyilanxin.kunpeng.utils.scheduler.future.ActorFuture;
import java.util.UUID;

/** 副本发送服务（bootstrap/merge 传输的服务端会话接口） */
public interface ReplicaSenderService extends AsyncClosable {

  /** 建立会话并返回首块；副本过旧时经 takeSnapshot 回调补拍 */
  ActorFuture<SnapshotBlock> getLatestSnapshot(
      int partition, long lastProcessedPosition, UUID transferId);

  /** 会话内顺序取下一块（校验 snapshotId 与上一块游标） */
  ActorFuture<SnapshotBlock> getNextChunk(
      int partition, String snapshotId, String previousChunkName, UUID transferId);

  /** 清空该分区的副本缓存与全部会话 */
  ActorFuture<Void> deleteSnapshots(int partitionId);

  /** 补拍回调（由分区状态控制器提供） */
  interface SnapshotTaker {

    ActorFuture<ArchivedSnapshot> takeSnapshot(long lastProcessedPosition);
  }
}
