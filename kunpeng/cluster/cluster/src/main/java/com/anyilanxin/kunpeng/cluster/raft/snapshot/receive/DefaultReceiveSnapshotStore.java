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
package com.anyilanxin.kunpeng.cluster.raft.snapshot.receive;

import com.anyilanxin.kunpeng.cluster.raft.snapshot.PersistedSnapshot;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.PersistedSnapshotListener;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotException.SnapshotAlreadyExistsException;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotId;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.impl.DefaultFileSnapshotStore;
import com.anyilanxin.kunpeng.scheduler.ConcurrencyControl;
import com.anyilanxin.kunpeng.scheduler.future.ActorFuture;
import com.anyilanxin.kunpeng.scheduler.future.CompletableActorFuture;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * 接收式镜像存储：组合公共存储实现 {@link DefaultFileSnapshotStore}（构造时创建）， 只实现接收入口 {@link
 * #newReceivedSnapshot}，其余存储能力全部委托。
 *
 * @author zxuanhong
 * @since 1.0.0
 */
public class DefaultReceiveSnapshotStore implements ReceiveSnapshotStore {

  // 公共存储实现：目录管理、.sfc 提交标记、保留策略、启动加载等
  private final DefaultFileSnapshotStore snapshotStore;
  private final ConcurrencyControl actor;

  /** 注入共享公共存储的构造（供多入口组合门面复用同一存储实例）。 */
  public DefaultReceiveSnapshotStore(
      final DefaultFileSnapshotStore snapshotStore, final ConcurrencyControl actor) {
    this.snapshotStore = snapshotStore;
    this.actor = actor;
  }

  @Override
  public ActorFuture<ReceivedSnapshot> newReceivedSnapshot(final String snapshotId) {
    final CompletableActorFuture<ReceivedSnapshot> future = new CompletableActorFuture<>();
    final SnapshotId id;
    try {
      id = SnapshotId.fromString(snapshotId);
    } catch (final IllegalArgumentException e) {
      future.completeExceptionally(e);
      return future;
    }
    actor.run(
        () -> {
          try {
            // 已存在相同或更新的镜像则拒绝接收
            final var current = snapshotStore.currentSnapshot();
            if (current != null && current.snapshotId().compareTo(id) >= 0) {
              throw new SnapshotAlreadyExistsException(
                  "Cannot receive snapshot "
                      + snapshotId
                      + "; a same or newer snapshot exists: "
                      + current.snapshotId());
            }
            final Path directory = snapshotStore.newTemporaryDirectory();
            Files.createDirectories(directory);
            final var pending = new DefaultReceivedSnapshot(id, directory, snapshotStore, actor);
            snapshotStore.addPending(pending);
            future.complete(pending);
          } catch (final Exception e) {
            future.completeExceptionally(e);
          }
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
