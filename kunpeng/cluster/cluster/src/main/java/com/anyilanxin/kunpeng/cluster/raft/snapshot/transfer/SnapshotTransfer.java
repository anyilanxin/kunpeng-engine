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

import com.anyilanxin.kunpeng.cluster.cluster.PartitionId;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.PersistedSnapshot;
import com.anyilanxin.kunpeng.scheduler.future.ActorFuture;
import org.jspecify.annotations.Nullable;

/**
 * 镜像传输服务（对外门面，业务持有）：向指定分区拉取最新镜像——先取回镜像基本信息 （信息分片），再经本地 ReceiveSnapshotStore 逐批接收分片并持久化。
 *
 * @author zxuanhong
 * @since 1.0.0
 */
public interface SnapshotTransfer {
  /**
   * @param partitionId the partition to get the snapshot from
   * @return a persisted snapshot satisfying the parameters' requirements
   */
  ActorFuture<@Nullable PersistedSnapshot> getLatestSnapshot(final PartitionId partitionId);

  /** 把给定镜像逐批推送到目标分区的 leader（合并转移入口）。 */
  ActorFuture<Void> pushSnapshot(
      final PersistedSnapshot snapshot, final PartitionId targetPartitionId);
}
