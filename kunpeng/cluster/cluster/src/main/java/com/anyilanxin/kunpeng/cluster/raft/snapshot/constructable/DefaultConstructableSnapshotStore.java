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

import com.anyilanxin.kunpeng.cluster.raft.snapshot.*;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotException.SnapshotAlreadyExistsException;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.impl.DefaultFileSnapshotStore;
import com.anyilanxin.kunpeng.scheduler.ConcurrencyControl;
import com.anyilanxin.kunpeng.scheduler.future.ActorFuture;
import com.anyilanxin.kunpeng.scheduler.future.CompletableActorFuture;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * 拍摄式镜像存储：组合公共存储实现 {@link DefaultFileSnapshotStore}（构造时创建）， 只实现拍摄入口 {@link
 * #newTransientSnapshot}——内容由调用方按次传入的 {@link SnapshotContentWriter} 写入，其余存储能力全部委托。
 *
 * @author zxuanhong
 * @since 1.0.0
 */
public class DefaultConstructableSnapshotStore {

  // 公共存储实现：目录管理、.sfc 提交标记、保留策略、启动加载等
  private final DefaultFileSnapshotStore snapshotStore;
  private final SnapshotFileInfoProvider snapshotFileInfoProvider;
  private final ConcurrencyControl actor;

  /** 注入共享公共存储的构造（供多入口组合门面复用同一存储实例）。 */
  public DefaultConstructableSnapshotStore(
      final DefaultFileSnapshotStore snapshotStore,
      final SnapshotFileInfoProvider snapshotFileInfoProvider,
      final ConcurrencyControl actor) {
    this.snapshotStore = snapshotStore;
    this.snapshotFileInfoProvider = snapshotFileInfoProvider;
    this.actor = actor;
  }

  /**
   * 拍摄入口（可强制）：{@code force=true} 时允许与当前最新镜像同 id 重拍——persist 阶段会原子覆盖同 id 旧目录，
   * 供"同水位但内容已变"的场景（如合并后重拍 raft 镜像）使用；{@code force=false} 时同 id 前置跳过（future 以
   * null 完成）。内容由 {@code contentWriter} 写入临时目录。
   */
  public ActorFuture<ConstructableSnapshot> newTransientSnapshot(
      final long index, final long term, final boolean force, final SnapshotContentWriter contentWriter) {
    final CompletableActorFuture<ConstructableSnapshot> future = new CompletableActorFuture<>();
    actor.run(
        () -> {
          final DefaultConstructableSnapshot pending;
          try {
            final var snapshotId = new SnapshotId(snapshotStore.nodeId(), index, term);
            final var current = snapshotStore.currentSnapshot();
            if (current != null) {
              final int order = current.snapshotId().compareTo(snapshotId);
              if (order == 0 && !force) {
                // 前置跳过: 相同 id 视为重复拍摄, 不建目录、不触发业务拍摄, future 以 null 完成
                future.complete(null);
                return;
              }
              if (order > 0) {
                throw new SnapshotAlreadyExistsException(
                    "Cannot take snapshot "
                        + snapshotId
                        + "; a newer snapshot exists: "
                        + current.snapshotId());
              }
            }
            final Path directory = snapshotStore.newTemporaryDirectory();
            Files.createDirectories(directory);
            pending =
                new DefaultConstructableSnapshot(
                    snapshotId, directory, snapshotStore, actor, snapshotFileInfoProvider);
            snapshotStore.addPending(pending);
          } catch (final Exception e) {
            future.completeExceptionally(e);
            return;
          }
          actor.runOnCompletion(
              pending.take(contentWriter),
              (ignored, error) -> {
                if (error != null) {
                  future.completeExceptionally(error);
                } else {
                  future.complete(pending);
                }
              });
        });
    return future;
  }
}
