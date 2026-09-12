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

import com.anyilanxin.kunpeng.cluster.raft.snapshot.PersistableSnapshot;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotChunkBatch;
import com.anyilanxin.kunpeng.scheduler.future.ActorFuture;

/**
 * 接收中的 pending 镜像：由 {@link ReceiveSnapshotStore#newReceivedSnapshot} 创建， 分片按批写入；每个文件字节收齐时回读校验整文件
 * CRC32，全部写完后 {@code persist()} 提交。
 *
 * @author zxuanhong
 * @since 1.0.0
 */
public interface ReceivedSnapshot extends PersistableSnapshot {

  /** 写入一批分片：逐分片校验内容 CRC32、按 {@code 文件名@字节偏移} 定位落盘； 任一分片校验失败或文件不完整时 future 异常完成。 */
  ActorFuture<Void> write(final SnapshotChunkBatch batch);
}
