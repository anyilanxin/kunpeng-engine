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

/**
 * 文件镜像存储公共契约：各模块（拍摄/接收/传输）的 store 共用的存储能力—— 启动加载与 pending 清理。镜像目录、.sfc 校验文件、保留策略等由实现统一承载。
 *
 * @author zxuanhong
 * @since 1.0.0
 */
public interface FileSnapshotStore extends SnapshotStore {

  /** 启动：加载已存储的镜像，清理未完整提交与临时目录残留。 */
  void start();

  /** 统一 abort 所有 pending 镜像并清理临时目录残留。 */
  ActorFuture<Void> abortPendingSnapshots();

  /** 日志压缩下界：可用镜像的最小 index，无镜像时为 0。 */
  ActorFuture<Long> getCompactionBound();
}
