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
import com.anyilanxin.kunpeng.kvstore.snapshot.SnapshotFileInfoProvider;
import com.anyilanxin.kunpeng.scheduler.ConcurrencyControl;
import com.anyilanxin.kunpeng.scheduler.future.ActorFuture;
import com.anyilanxin.kunpeng.scheduler.future.CompletableActorFuture;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * 拍摄式镜像存储：组合公共存储实现 {@link DefaultFileSnapshotStore}（构造时创建）， 只实现拍摄入口 {@link
 * #newTransientSnapshot}，其余存储能力全部委托。
 *
 * @author zxuanhong
 * @since 1.0.0
 */
public class DefaultConstructableSnapshotStore implements ConstructableSnapshotStore {

  // 公共存储实现：目录管理、.sfc 提交标记、保留策略、启动加载等
  private final DefaultFileSnapshotStore snapshotStore;
  // 外部拍摄逻辑：一个 store 对应一种拍法
  private final SnapshotProvider snapshotProvider;
  private final SnapshotFileInfoProvider snapshotFileInfoProvider;
  private final ConcurrencyControl actor;

  /** 注入共享公共存储的构造（供多入口组合门面复用同一存储实例）。 */
  public DefaultConstructableSnapshotStore(
      final DefaultFileSnapshotStore snapshotStore,
      final SnapshotProvider snapshotProvider,
      final SnapshotFileInfoProvider snapshotFileInfoProvider,
      final ConcurrencyControl actor) {
    this.snapshotStore = snapshotStore;
    this.snapshotProvider = snapshotProvider;
    this.snapshotFileInfoProvider = snapshotFileInfoProvider;
    this.actor = actor;
  }

  @Override
  public ActorFuture<ConstructableSnapshot> newTransientSnapshot(
      final long index, final long term) {
    final CompletableActorFuture<ConstructableSnapshot> future = new CompletableActorFuture<>();
    actor.run(
        () -> {
          final DefaultConstructableSnapshot pending;
          try {
            final var snapshotId = new SnapshotId(snapshotStore.nodeId(), index, term);
            final var current = snapshotStore.currentSnapshot();
            if (current != null) {
              final int order = current.snapshotId().compareTo(snapshotId);
              if (order == 0) {
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
              pending.take(snapshotProvider),
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

  @Override
  public void start() {
    snapshotStore.start();
  }

  @Override
  public ActorFuture<Void> abortPendingSnapshots() {
    return snapshotStore.abortPendingSnapshots();
  }

  @Override
  public ActorFuture<Long> getCompactionBound() {
    return snapshotStore.getCompactionBound();
  }

  @Override
  public Optional<PersistedSnapshot> getLatestSnapshot() {
    return snapshotStore.getLatestSnapshot();
  }

  @Override
  public ActorFuture<Boolean> addSnapshotListener(final PersistedSnapshotListener listener) {
    return snapshotStore.addSnapshotListener(listener);
  }

  @Override
  public ActorFuture<Boolean> removeSnapshotListener(final PersistedSnapshotListener listener) {
    return snapshotStore.removeSnapshotListener(listener);
  }

  @Override
  public long getCurrentSnapshotIndex() {
    return snapshotStore.getCurrentSnapshotIndex();
  }

  @Override
  public int getMaxSnapshotCount() {
    return snapshotStore.getMaxSnapshotCount();
  }

  @Override
  public ActorFuture<Void> delete() {
    return snapshotStore.delete();
  }

  @Override
  public Path getPath() {
    return snapshotStore.getPath();
  }
}
