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

/**
 * 副本传输客户端（ReplicaSenderService 的远端调用侧；partition 维度，通道显式声明）
 */
public interface ReplicaTransferClient extends AsyncClosable {

  /** 建立会话并取首块（无可用快照时完成于 null） */
  ActorFuture<SnapshotBlock> getLatestSnapshot(
      int partition, long upToIndex, UUID transferId, ReplicaChannel channel);

  /** 会话内顺序取下一块（读完返回 null） */
  ActorFuture<SnapshotBlock> getNextChunk(
      int partition,
      String snapshotId,
      String previousBlockName,
      UUID transferId,
      ReplicaChannel channel);

  /** 通知对端清理该分区的副本缓存与全部会话 */
  ActorFuture<Void> deleteSnapshots(int partition, ReplicaChannel channel);
}
