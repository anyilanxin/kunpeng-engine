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
package com.anyilanxin.kunpeng.cluster.raft.snapshot.impl;

import com.anyilanxin.kunpeng.cluster.raft.snapshot.*;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.constructable.ConstructableSnapshot;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.constructable.DefaultConstructableSnapshotStore;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.constructable.RaftSnapshotProvider;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.constructable.SnapshotContentWriter;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.receive.DefaultReceiveSnapshotStore;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.receive.ReceivedSnapshot;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotFileInfoProvider;
import com.anyilanxin.kunpeng.scheduler.ConcurrencyControl;
import com.anyilanxin.kunpeng.scheduler.future.ActorFuture;
import java.nio.file.Path;
import java.util.Optional;

/**
 * {@link RaftSnapshotStore} 的默认实现：组合一个共享 {@link DefaultFileSnapshotStore}， 拍摄与接收两个入口分别委托给对应的模块
 * store，二者共享同一持久存储实例。
 *
 * @author zxuanhong
 * @since 1.0.0
 */
public final class DefaultRaftSnapshotStore implements RaftSnapshotStore {

  private final DefaultFileSnapshotStore store;
  private final DefaultConstructableSnapshotStore constructable;
  private final DefaultReceiveSnapshotStore receive;
  private final SnapshotContentWriter contentWriter;

  public DefaultRaftSnapshotStore(
      final String nodeId,
      final Path snapshotPath,
      final int maxSnapshotCount,
      final SimpleFileVerificationStore simpleFileVerificationStore,
      final SnapshotFileInfoProvider snapshotFileInfoProvider,
      final RaftSnapshotProvider snapshotProvider,
      final ConcurrencyControl actor) {
    this(
        nodeId,
        snapshotPath,
        maxSnapshotCount,
        simpleFileVerificationStore,
        snapshotFileInfoProvider,
      snapshotProvider::takeSnapshot,
        actor);
  }

  /**
   * 内容写入器构造：供不走业务 SPI 的存储（如合并镜像存储）直接给定拍法， 免去整接口适配。
   */
  public DefaultRaftSnapshotStore(
      final String nodeId,
      final Path snapshotPath,
      final int maxSnapshotCount,
      final SimpleFileVerificationStore simpleFileVerificationStore,
      final SnapshotFileInfoProvider snapshotFileInfoProvider,
      final SnapshotContentWriter contentWriter,
      final ConcurrencyControl actor) {
    store =
        new DefaultFileSnapshotStore(
            nodeId,
            snapshotPath,
            maxSnapshotCount,
            simpleFileVerificationStore,
            snapshotFileInfoProvider,
            actor);
    this.contentWriter = contentWriter;
    constructable =
        new DefaultConstructableSnapshotStore(store, snapshotFileInfoProvider, actor);
    receive = new DefaultReceiveSnapshotStore(store, actor);
  }

  @Override
  public ActorFuture<ConstructableSnapshot> newTransientSnapshot(
      final long index, final long term) {
    return constructable.newTransientSnapshot(index, term, false, contentWriter);
  }

  /**
   * 拍摄入口（可强制）：{@code force=true} 时允许与当前最新镜像同 id 重拍覆盖（见 {@link
   * DefaultConstructableSnapshotStore#newTransientSnapshot(long, long, boolean,
   * SnapshotContentWriter)}）， 供合并后同水位重拍 raft 镜像使用。
   */
  public ActorFuture<ConstructableSnapshot> newTransientSnapshot(
      final long index, final long term, final boolean force) {
    return constructable.newTransientSnapshot(index, term, force, contentWriter);
  }

  @Override
  public ActorFuture<ReceivedSnapshot> newReceivedSnapshot(final String snapshotId) {
    return receive.newReceivedSnapshot(snapshotId);
  }

  @Override
  public void start() {
    store.start();
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
  public Optional<PersistedSnapshot> getLatestSnapshot() {
    return store.getLatestSnapshot();
  }

  @Override
  public ActorFuture<Boolean> addSnapshotListener(final PersistedSnapshotListener listener) {
    return store.addSnapshotListener(listener);
  }

  @Override
  public ActorFuture<Boolean> removeSnapshotListener(final PersistedSnapshotListener listener) {
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

  /** 删除本存储内全部已提交镜像（磁盘与内存），保留根目录；引导/合并等外部生命周期镜像的清理入口。 */
  public ActorFuture<Void> deleteAllSnapshots() {
    return store.deleteAllSnapshots();
  }

  @Override
  public Path getPath() {
    return store.getPath();
  }
}
