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
package com.anyilanxin.kunpeng.cluster.raft.snapshot.constructable;

import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotStore;
import com.anyilanxin.kunpeng.scheduler.CloseableSilently;
import com.anyilanxin.kunpeng.scheduler.future.ActorFuture;
import java.nio.file.Path;
import java.util.Map;

/**
 * 镜像内容拍摄 SPI：具体"拍什么、怎么拍"由业务系统实现，在 {@link ConstructableSnapshotStore} 构造时传入——一个 store 对应一种拍法。
 *
 * @author zxuanhong
 * @since 1.0.0
 */
public interface SnapshotProvider<T> extends CloseableSilently {

  /**
   * 拍摄镜像内容：把任意数量的内容文件写入 {@code snapshotDirectory}。
   *
   * <p>目录由镜像模块创建与管理，业务只写文件、不建/删目录；抛出异常即本次拍摄失败， 临时目录会被清理。
   *
   * @param snapshotDirectory 本次拍摄的临时目录
   * @return 业务信息键值清单（随镜像持久化到 snapshot.metadata；可为空/null； key/value 均不得包含 '=' 与换行，值取 {@code
   *     String.valueOf}）
   */
  Map<String, Object> takeSnapshot(Path snapshotDirectory);

  void setPartitionDirectory(Path partitionDirectory);

  Path getPartitionDirectory();

  void setRuntimeDirectory(Path runtimeDirectory);

  Path getRuntimeDirectory();

  void setSnapshotStore(SnapshotStore snapshotStore);

  SnapshotStore getSnapshotStore();

  ActorFuture<T> recover();
}
