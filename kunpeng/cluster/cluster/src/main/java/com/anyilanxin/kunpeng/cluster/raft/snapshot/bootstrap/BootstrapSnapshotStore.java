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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.cluster.raft.snapshot.bootstrap;

import com.anyilanxin.kunpeng.cluster.raft.snapshot.PersistedSnapshot;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotId;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotType;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.constructable.DefaultConstructableSnapshotStore;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.constructable.TransferSnapshotProvider;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.impl.DefaultFileSnapshotStore;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.impl.DefaultSimpleFileVerificationStore;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotFileInfoProvider;
import com.anyilanxin.kunpeng.scheduler.ConcurrencyControl;
import com.anyilanxin.kunpeng.scheduler.future.ActorFuture;
import com.anyilanxin.kunpeng.scheduler.future.CompletableActorFuture;
import java.nio.file.Path;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * 引导镜像拍摄端存储：内容位于 raft 根目录 {@code snapshots/} {@link SnapshotType#BOOTSTRAP} 子目录，
 * 与常规镜像（{@link SnapshotType#RAFT}）互不影响。
 *
 * <p>拍摄复用语义：同一时段多个新分区可能请求引导镜像且内容一致，因此已有引导镜像时直接 复用、不重拍——直到引用归零被删除后，下一个请求才重新拍摄。
 * 生命周期由外部流程管控（{@link BootstrapSnapshotServer} 的 transferId 引用计数 与节点关闭清理），不参与常规镜像保留策略。
 *
 * <p>引导镜像不跨重启存活：{@link #start()} 加载后即清空残留——引用状态只在内存，重启后无人 再发 RELEASE，残留只会泄漏磁盘。
 *
 * @author zxuanhong
 * @since 1.0.0
 */
public final class BootstrapSnapshotStore {

  /** 引导镜像最多保留 1 个：复用语义下同时只会存在一个，重拍前必先删除。 */
  private static final int MAX_BOOTSTRAP_SNAPSHOT_COUNT = 1;

  private final DefaultFileSnapshotStore store;
  private final DefaultConstructableSnapshotStore constructable;
  private final ConcurrencyControl actor;
  private final TransferSnapshotProvider transferSnapshotProvider;

  public BootstrapSnapshotStore(
      final String nodeId,
      final Path partitionDirectory,
      final TransferSnapshotProvider transferSnapshotProvider,
      final SnapshotFileInfoProvider snapshotFileInfoProvider,
      final ConcurrencyControl actor) {
    this.actor = actor;
    this.transferSnapshotProvider = transferSnapshotProvider;
    store =
        new DefaultFileSnapshotStore(
            nodeId,
            partitionDirectory.resolve("snapshots").resolve(SnapshotType.BOOTSTRAP.directoryName()),
            MAX_BOOTSTRAP_SNAPSHOT_COUNT,
            new DefaultSimpleFileVerificationStore(),
            snapshotFileInfoProvider,
            actor);
    constructable =
        new DefaultConstructableSnapshotStore(store, snapshotFileInfoProvider, actor);
  }

  /**
   * 启动：加载磁盘既有镜像后立即清空——引导镜像的引用计数只在内存，重启后无法推进删除， 残留直接清理，避免泄漏磁盘。
   */
  public void start() {
    store.start();
    store.deleteAllSnapshots();
  }

  /**
   * 拍摄引导镜像：已有引导镜像时直接复用（多请求共享同一镜像）；否则以给定 index/term 用 引导 provider 拍摄并提交。
   *
   * @param index 拍摄位点（源分区当前 commit index）
   * @param term 拍摄任期（源分区当前 term）
   */
  public ActorFuture<PersistedSnapshot> takeBootstrapSnapshot(final long index, final long term) {
    final CompletableActorFuture<PersistedSnapshot> future = new CompletableActorFuture<>();
    actor.run(
        () -> {
          // 复用判定必须在 actor 上与删除串行：引用未归零期间不重拍，保证正在拉取的会话目录稳定
          final var existing = store.currentSnapshot();
          if (existing != null) {
            future.complete(existing);
            return;
          }
          constructable
              .newTransientSnapshot(index, term, false, this::takeBootstrapContent)
              .onComplete(
                  (pending, error) -> {
                    if (error != null) {
                      future.completeExceptionally(error);
                      return;
                    }
                    // 复用窗口内被并发拍摄补位（pending 为 null）时直接取现有镜像
                    if (pending == null) {
                      final var current = store.currentSnapshot();
                      if (current != null) {
                        future.complete(current);
                      } else {
                        future.completeExceptionally(
                            new IllegalStateException(
                                "Bootstrap snapshot " + index + "-" + term + " was skipped"));
                      }
                      return;
                    }
                    pending
                        .persist()
                        .onComplete(
                            (persisted, persistError) -> {
                              if (persistError != null) {
                                future.completeExceptionally(persistError);
                              } else {
                                future.complete(persisted);
                              }
                            });
                  });
        });
    return future;
  }

  /** 引导内容直写：委托 {@link TransferSnapshotProvider#takeBootstrapSnapshot}，业务信息清单随镜像持久化。 */
  private @Nullable Map<String, Object> takeBootstrapContent(final Path snapshotDirectory) {
    return transferSnapshotProvider.takeBootstrapSnapshot(snapshotDirectory);
  }

  /** 当前引导镜像（未拍摄或已删除时为空）。 */
  public ActorFuture<@Nullable PersistedSnapshot> getBootstrapSnapshot() {
    return actor.call(() -> store.getLatestSnapshot().orElse(null));
  }

  /** 删除指定引导镜像；不存在时静默完成（RELEASE 迟到/重复时幂等）。 */
  public ActorFuture<Void> deleteBootstrapSnapshot(final SnapshotId snapshotId) {
    return store.deleteSnapshot(snapshotId);
  }

  /** 删除全部引导镜像（节点关闭时主动清理）。 */
  public ActorFuture<Void> deleteBootstrapSnapshots() {
    return store.deleteAllSnapshots();
  }

  /** 引导镜像目录。 */
  public Path getPath() {
    return store.getPath();
  }
}
