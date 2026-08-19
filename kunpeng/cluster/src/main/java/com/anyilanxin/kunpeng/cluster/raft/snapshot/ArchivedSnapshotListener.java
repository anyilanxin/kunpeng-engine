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
package com.anyilanxin.kunpeng.cluster.raft.snapshot;

import com.anyilanxin.kunpeng.cluster.raft.snapshot.impl.ArchivedSnapshot;

/** 落档快照事件监听（新快照提交/旧快照淘汰） */
public interface ArchivedSnapshotListener {

  /** 新快照落档（更早的快照随后可能被淘汰） */
  void onArchived(ArchivedSnapshot snapshot);

  /** 快照被淘汰删除 */
  default void onPurged(final ArchivedSnapshot snapshot) {
  }
}
