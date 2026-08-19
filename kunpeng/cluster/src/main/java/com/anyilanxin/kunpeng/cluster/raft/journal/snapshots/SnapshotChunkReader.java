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

import java.nio.ByteBuffer;

/** 快照块顺序读取器（Leader 侧向 Follower 发送用） */
public interface SnapshotChunkReader {

  /** 是否还有下一个块 */
  boolean hasNext();

  /** 读取下一个块 */
  SnapshotChunk next();

  /** 下一个块的标识（用于 InstallRequest.nextChunkId） */
  ByteBuffer nextId();

  /** 定位到指定块（重传） */
  void seek(ByteBuffer chunkId);
}
