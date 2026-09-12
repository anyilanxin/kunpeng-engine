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

import com.anyilanxin.kunpeng.cluster.raft.snapshot.FileSnapshotStore;
import com.anyilanxin.kunpeng.scheduler.future.ActorFuture;

/**
 * 拍摄式镜像存储入口：创建的 pending 镜像已由构造时传入的 {@link SnapshotProvider} 完成内容拍摄，业务信息键值清单由拍摄返回值提供。
 *
 * @author zxuanhong
 * @since 1.0.0
 */
public interface ConstructableSnapshotStore extends FileSnapshotStore {

  /**
   * 创建并拍摄一个 pending 镜像：future 完成即内容已写好、校验集已算好， 业务随后只需 {@code persist()}；拍摄失败则 future 异常完成且临时目录已清理。
   *
   * <p>已存在相同 id（index+term 相同）的镜像时视为重复拍摄，本次前置跳过： future 以 {@code null} 完成，不建目录、不触发业务拍摄；已存在更新 id
   * 的镜像时 future 以 {@link
   * com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotException.SnapshotAlreadyExistsException}
   * 异常完成。
   */
  ActorFuture<ConstructableSnapshot> newTransientSnapshot(final long index, final long term);
}
