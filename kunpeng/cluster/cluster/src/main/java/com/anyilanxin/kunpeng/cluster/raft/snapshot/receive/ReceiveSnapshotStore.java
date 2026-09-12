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
package com.anyilanxin.kunpeng.cluster.raft.snapshot.receive;

import com.anyilanxin.kunpeng.cluster.raft.snapshot.FileSnapshotStore;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotException;
import com.anyilanxin.kunpeng.scheduler.future.ActorFuture;

/**
 * 接收式镜像存储入口：接收外部传入的镜像（install 复制 / 跨分区传输），逐分片写入后持久化。
 *
 * @author zxuanhong
 * @since 1.0.0
 */
public interface ReceiveSnapshotStore extends FileSnapshotStore {

  /**
   * 为给定镜像 id（{@code <hex(nodeId)>-<index>-<term>}）创建接收中的 pending 镜像， 由传输逻辑逐批写入分片，直到 {@code
   * persist()} 提交。
   *
   * <p>已存在相同或更新的镜像时 future 以 {@link SnapshotException.SnapshotAlreadyExistsException} 异常完成。
   */
  ActorFuture<ReceivedSnapshot> newReceivedSnapshot(final String snapshotId);
}
