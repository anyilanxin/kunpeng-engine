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

import java.io.UncheckedIOException;
import java.nio.file.Path;

/** 已持久化的快照（只读视图） */
public interface PersistedSnapshot {

  /** raft 日志索引 */
  long getIndex();

  /** 拍摄时的任期 */
  long getTerm();

  /** 快照唯一标识（目录名） */
  String getId();

  /** 快照格式版本 */
  int version();

  /** 快照校验和 */
  long getChecksum();

  /** 快照目录 */
  Path getPath();

  /** 创建块读取器；快照可能已被删除，抛 {@link UncheckedIOException} */
  SnapshotChunkReader newChunkReader();
}
