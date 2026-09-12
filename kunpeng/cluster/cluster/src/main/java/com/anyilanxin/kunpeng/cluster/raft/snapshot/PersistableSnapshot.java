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

import com.anyilanxin.kunpeng.scheduler.future.ActorFuture;
import java.nio.file.Path;

/**
 * @author zxuanhong
 */
public interface PersistableSnapshot {
  /**
   * 中断
   *
   * @return {@code ActorFuture<Void>}
   */
  ActorFuture<Void> abort();

  /**
   * 持久化
   *
   * @return {@code ActorFuture<PersistedSnapshot>}
   */
  ActorFuture<PersistedSnapshot> persist();

  /**
   * 镜像 id
   *
   * @return {@link SnapshotId }
   */
  SnapshotId snapshotId();

  /**
   * 镜像所在的路径
   *
   * @return {@link Path }
   */
  Path getPath();
}
