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

import com.anyilanxin.kunpeng.scheduler.future.ActorFuture;
import java.nio.file.Path;

/**
 * 进行中（pending）快照句柄：拍摄与接收统一形态，内容来源不同。
 *
 * <p>业务或传输逻辑把内容直接写入 {@link #getPath()} 目录（拍摄），或经
 * {@link SnapshotChunkAppender} 逐片写入（接收）；随后 {@link #persist()} 走三阶段提交
 * （生成 manifest → 原子 move → .sfc 标记），失败则 {@link #abort()} 清理临时目录。
 */
public interface PersistableSnapshot {

  /** 该 pending 快照的标识三元组。 */
  SnapshotId snapshotId();

  /** 待写入内容的目录（临时目录），提交成功前对外不可见。 */
  Path getPath();

  /**
   * 提交该快照：拍摄模式生成 manifest 后三阶段提交；接收模式校验已传输的 manifest 与内容。
   */
  ActorFuture<PersistedSnapshot> persist();

  /** 放弃本次拍摄/接收，清理临时目录。 */
  ActorFuture<Void> abort();
}
