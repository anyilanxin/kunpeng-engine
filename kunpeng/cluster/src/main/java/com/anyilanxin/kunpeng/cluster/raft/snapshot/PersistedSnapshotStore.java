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

import com.anyilanxin.kunpeng.scheduler.Either;
import com.anyilanxin.kunpeng.scheduler.future.ActorFuture;
import com.anyilanxin.kunpeng.scheduler.future.CompletableActorFuture;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

/** 本地持久快照存储：按类型查询、拍摄、删除与日志压缩联动。 */
public interface PersistedSnapshotStore {

  /** 最新常规快照（仅 {@link SnapshotType#REGULAR} 类型）。 */
  Optional<RaftSnapshot> getLatestSnapshot();

  /** 给定索引处的常规快照。 */
  Optional<RaftSnapshot> getSnapshotAt(long index);

  /** 日志压缩下界：仅统计常规快照（引导/合并不参与日志压缩），无快照时为 0。 */
  CompletableFuture<Long> getCompactionBound();

  /** 最新引导快照（该类型仅保留最新一个）。 */
  Optional<BootstrapSnapshot> getBootstrapSnapshot();

  /** 最新合并快照（该类型仅保留最新一个）。 */
  Optional<MergeSnapshot> getMergeSnapshot();

  /** 常规快照的最大保留数量。 */
  int getMaxSnapshotCount();

  /**
   * 删除给定索引及之后的全部未预留快照。
   *
   * @return a future completed with the number of deleted snapshots
   */
  CompletableFuture<Integer> deleteSnapshotsFrom(long index);

  /** 清理全部未完成的 pending 快照（拍摄/接收中的临时目录）。 */
  ActorFuture<Void> abortPendingSnapshots();

  /**
   * 创建常规快照的 pending 句柄：业务把内容文件写入 {@link PersistableSnapshot#getPath()}
   * 后调用 {@link PersistableSnapshot#persist()} 提交。
   *
   * @param index the Raft log index of the snapshot
   * @param term the Raft term of the snapshot
   * @param businessInfo 业务信息键值清单，落盘时按 {@code String.valueOf(value)} 序列化
   * @return Right 为 pending 快照；Left 表示创建失败
   */
  Either<SnapshotException, PersistableSnapshot> newTransientSnapshot(
      long index, long term, Map<String, Object> businessInfo);

  /** 创建引导快照的 pending 句柄，形态同 {@link #newTransientSnapshot}。 */
  Either<SnapshotException, PersistableSnapshot> newBootstrapSnapshot(
      long index, long term, Map<String, Object> businessInfo);

  /** 创建合并快照的 pending 句柄，形态同 {@link #newTransientSnapshot}。 */
  Either<SnapshotException, PersistableSnapshot> newMergeSnapshot(
      long index, long term, Map<String, Object> businessInfo);

  /**
   * 从最新常规快照复制产生引导快照：模块建 pending 目录后调用回调（源目录 → 目标目录），
   * 再走三阶段提交；无常规快照时以异常完成。
   */
  ActorFuture<PersistedSnapshot> copyForBootstrap(BiConsumer<Path, Path> copySnapshot);

  /** 注册快照提交（持久化）后的监听器。 */
  void addSnapshotListener(PersistedSnapshotListener listener);

  /** 移除已注册的监听器。 */
  void removeSnapshotListener(PersistedSnapshotListener listener);

  /** 关闭存储并清理资源，默认立即完成。 */
  default ActorFuture<Void> close() {
    return CompletableActorFuture.completed();
  }
}
