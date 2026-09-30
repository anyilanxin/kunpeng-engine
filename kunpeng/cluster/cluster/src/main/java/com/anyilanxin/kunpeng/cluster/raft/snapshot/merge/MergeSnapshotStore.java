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
package com.anyilanxin.kunpeng.cluster.raft.snapshot.merge;

import com.anyilanxin.kunpeng.cluster.raft.snapshot.PersistedSnapshot;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotFileInfoProvider;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotId;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotType;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.constructable.DefaultConstructableSnapshotStore;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.constructable.TransferSnapshotProvider;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.impl.DefaultFileSnapshotStore;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.impl.DefaultSimpleFileVerificationStore;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.receive.DefaultReceiveSnapshotStore;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.receive.ReceiveSnapshotStore;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.receive.ReceivedSnapshot;
import com.anyilanxin.kunpeng.scheduler.ConcurrencyControl;
import com.anyilanxin.kunpeng.scheduler.future.ActorFuture;
import com.anyilanxin.kunpeng.scheduler.future.CompletableActorFuture;
import java.nio.file.Path;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * 合并镜像存储：内容位于 raft 根目录 {@code snapshots/merge}，拉取/推送合并的源端拍摄与目标端接收共用； 拍摄统一走 {@link
 * TransferSnapshotProvider#takeMergeSnapshot}（拍摄参数由推送/拉取入口传入）。
 *
 * <p>拍摄复用语义：同一时段多个请求方可能请求同一份合并镜像且内容一致，已有镜像时直接 复用、不重拍——直到引用归零被删除后，下一个请求才重新拍摄。 生命周期由外部流程管控（{@code
 * SnapshotPullServer} 的 transferId 引用计数、合并完成清理与节点关闭清理），不参与常规镜像保留策略。
 *
 * <p>合并镜像不跨重启存活：{@link #start()} 加载后即清空残留。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class MergeSnapshotStore implements ReceiveSnapshotStore {

  /** 合并镜像最多保留 1 个：一次只进行一次转移，完成后即删除。 */
  private static final int MAX_MERGE_SNAPSHOT_COUNT = 1;

  private final DefaultFileSnapshotStore store;
  private final DefaultConstructableSnapshotStore constructable;
  private final DefaultReceiveSnapshotStore receive;
  private final ConcurrencyControl actor;
  private final TransferSnapshotProvider transferSnapshotProvider;

  public MergeSnapshotStore(
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
            partitionDirectory.resolve("snapshots").resolve(SnapshotType.MERGE.directoryName()),
            MAX_MERGE_SNAPSHOT_COUNT,
            new DefaultSimpleFileVerificationStore(),
            snapshotFileInfoProvider,
            actor);
    constructable = new DefaultConstructableSnapshotStore(store, snapshotFileInfoProvider, actor);
    receive = new DefaultReceiveSnapshotStore(store, actor);
  }

  /** 启动：加载磁盘既有镜像后立即清空——合并镜像的引用状态只在内存，重启后残留直接清理。 */
  @Override
  public void start() {
    store.start();
    store.deleteAllSnapshots();
  }

  /**
   * 拍摄合并镜像：已有镜像时直接复用（多请求共享同一镜像）；否则以给定 index/term 按拍摄参数拍摄并提交。
   *
   * @param index 拍摄位点（源分区当前 commit index）
   * @param term 拍摄任期（源分区当前 term）
   * @param parameters 拍摄参数（由实现自行解析）
   */
  public ActorFuture<PersistedSnapshot> takeMergeSnapshot(
      final long index, final long term, final Map<String, String> parameters) {
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
              .newTransientSnapshot(
                  index, term, false, directory -> takeMergeContent(directory, parameters))
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
                                "Merge snapshot " + index + "-" + term + " was skipped"));
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

  /** 合并镜像内容直写：委托 {@link TransferSnapshotProvider#takeMergeSnapshot}，业务信息清单随镜像持久化。 */
  private @Nullable Map<String, Object> takeMergeContent(
      final Path snapshotDirectory, final Map<String, String> parameters) {
    return transferSnapshotProvider.takeMergeSnapshot(snapshotDirectory, parameters);
  }

  /** 当前合并镜像（未拍摄或已删除时为空）。 */
  public ActorFuture<@Nullable PersistedSnapshot> getMergeSnapshot() {
    return actor.call(() -> store.getLatestSnapshot().orElse(null));
  }

  /** 删除指定合并镜像；不存在时静默完成（RELEASE 迟到/重复时幂等）。 */
  public ActorFuture<Void> deleteSnapshot(final SnapshotId snapshotId) {
    return store.deleteSnapshot(snapshotId);
  }

  /** 删除全部合并镜像（节点关闭/合并完成时主动清理）。 */
  public ActorFuture<Void> deleteAllSnapshots() {
    return store.deleteAllSnapshots();
  }

  /** 合并镜像目录。 */
  public Path getPath() {
    return store.getPath();
  }

  @Override
  public ActorFuture<ReceivedSnapshot> newReceivedSnapshot(final String snapshotId) {
    return receive.newReceivedSnapshot(snapshotId);
  }

  @Override
  public ActorFuture<Void> abortPendingSnapshots() {
    return store.abortPendingSnapshots();
  }

  @Override
  public ActorFuture<Long> getCompactionBound() {
    return store.getCompactionBound();
  }

  @Override
  public java.util.Optional<PersistedSnapshot> getLatestSnapshot() {
    return store.getLatestSnapshot();
  }

  @Override
  public ActorFuture<Boolean> addSnapshotListener(
      final com.anyilanxin.kunpeng.cluster.raft.snapshot.PersistedSnapshotListener listener) {
    return store.addSnapshotListener(listener);
  }

  @Override
  public ActorFuture<Boolean> removeSnapshotListener(
      final com.anyilanxin.kunpeng.cluster.raft.snapshot.PersistedSnapshotListener listener) {
    return store.removeSnapshotListener(listener);
  }

  @Override
  public long getCurrentSnapshotIndex() {
    return store.getCurrentSnapshotIndex();
  }

  @Override
  public int getMaxSnapshotCount() {
    return store.getMaxSnapshotCount();
  }

  @Override
  public ActorFuture<Void> delete() {
    return store.delete();
  }
}
