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
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/** 面向测试的内存版可接收快照存储，用于在无磁盘依赖的情况下模拟快照的持久化与接收。 */
public class TestSnapshotStore implements ReceivableSnapshotStore {

  /** 最近一次已提交的快照，可能为 null。 */
  final AtomicReference<InMemorySnapshot> currentPersistedSnapshot;

  /** 按索引组织的全部已持久化快照。 */
  final NavigableMap<Long, InMemorySnapshot> persistedSnapshots = new ConcurrentSkipListMap<>();

  /** 通过安装协议收到的快照记录。 */
  final List<InMemorySnapshot> receivedSnapshots = new CopyOnWriteArrayList<>();

  /** 快照落盘事件的监听者列表。 */
  final List<PersistedSnapshotListener> listeners = new CopyOnWriteArrayList<>();

  /** 本存储节点 id（测试缺省 "0"）。 */
  private volatile String nodeId = "0";

  private volatile Runnable beforeSnapshotApplied = () -> {};

  public TestSnapshotStore(final AtomicReference<InMemorySnapshot> persistedSnapshotRef) {
    currentPersistedSnapshot = persistedSnapshotRef;
  }

  /** 设置拍摄快照时使用的节点 id。 */
  public void setNodeId(final String nodeId) {
    this.nodeId = nodeId;
  }

  /** 返回最新已提交快照。 */
  @Override
  public Optional<RaftSnapshot> getLatestSnapshot() {
    return Optional.ofNullable(currentPersistedSnapshot.get());
  }

  /** 返回索引不大于给定值的最近快照。 */
  @Override
  public Optional<RaftSnapshot> getSnapshotAt(final long index) {
    return Optional.ofNullable(persistedSnapshots.floorEntry(index)).map(Map.Entry::getValue);
  }

  /** 压缩下界取最早持久化快照的索引，无快照时为 0。 */
  @Override
  public CompletableFuture<Long> getCompactionBound() {
    final var earliest = persistedSnapshots.firstEntry();
    return CompletableFuture.completedFuture(earliest == null ? 0L : earliest.getKey());
  }

  /** 内存测试存储不持有引导快照。 */
  @Override
  public Optional<BootstrapSnapshot> getBootstrapSnapshot() {
    return Optional.empty();
  }

  /** 内存测试存储不持有合并快照。 */
  @Override
  public Optional<MergeSnapshot> getMergeSnapshot() {
    return Optional.empty();
  }

  /** 测试存储不限制快照数量。 */
  @Override
  public int getMaxSnapshotCount() {
    return Integer.MAX_VALUE;
  }

  /** 删除指定索引及之后的全部未预留快照，返回实际删除数量。 */
  @Override
  public CompletableFuture<Integer> deleteSnapshotsFrom(final long index) {
    final var removable =
        persistedSnapshots.tailMap(index).entrySet().stream()
            .filter(entry -> !entry.getValue().isReserved())
            .map(Map.Entry::getKey)
            .toList();
    removable.forEach(persistedSnapshots::remove);
    return CompletableFuture.completedFuture(removable.size());
  }

  /** 丢弃所有接收中的快照。 */
  @Override
  public ActorFuture<Void> abortPendingSnapshots() {
    receivedSnapshots.clear();
    return CompletableActorFuture.completed();
  }

  /** 创建一个只在提交时生效的临时快照。 */
  @Override
  public Either<SnapshotException, PersistableSnapshot> newTransientSnapshot(
      final long index, final long term, final Map<String, Object> businessInfo) {
    final var pendingSnapshot =
        new InMemorySnapshot(this, index, term, Integer.parseInt(nodeId));
    return Either.right(pendingSnapshot);
  }

  /** 创建引导快照 pending，内存测试存储按常规快照同形态处理。 */
  @Override
  public Either<SnapshotException, PersistableSnapshot> newBootstrapSnapshot(
      final long index, final long term, final Map<String, Object> businessInfo) {
    return newTransientSnapshot(index, term, businessInfo);
  }

  /** 创建合并快照 pending，内存测试存储按常规快照同形态处理。 */
  @Override
  public Either<SnapshotException, PersistableSnapshot> newMergeSnapshot(
      final long index, final long term, final Map<String, Object> businessInfo) {
    return newTransientSnapshot(index, term, businessInfo);
  }

  /** 内存测试存储不支持快照复制，恒以异常完成。 */
  @Override
  public ActorFuture<PersistedSnapshot> copyForBootstrap(final BiConsumer<java.nio.file.Path, java.nio.file.Path> copySnapshot) {
    return CompletableActorFuture.completedExceptionally(
        new SnapshotException("TestSnapshotStore does not support copyForBootstrap"));
  }

  /** 注册快照落盘监听器。 */
  @Override
  public void addSnapshotListener(final PersistedSnapshotListener listener) {
    listeners.add(listener);
  }

  /** 移除快照落盘监听器。 */
  @Override
  public void removeSnapshotListener(final PersistedSnapshotListener listener) {
    listeners.remove(listener);
  }

  /** 登记一个新接收的快照并返回其 pending 句柄。 */
  @Override
  public CompletableFuture<PersistableSnapshot> newReceivedSnapshot(final String snapshotId) {
    final var incoming = new InMemorySnapshot(this, snapshotId);
    receivedSnapshots.add(incoming);
    return CompletableFuture.completedFuture(incoming);
  }

  /** 当前快照索引，尚未提交过快照时为 0。 */
  public long getCurrentSnapshotIndex() {
    final var latest = currentPersistedSnapshot.get();
    return latest == null ? 0 : latest.getIndex();
  }

  /** 提交快照：清理更早的未预留快照后登记新快照并通知监听器。 */
  public void newSnapshot(final InMemorySnapshot persistedSnapshot) {
    beforeSnapshotApplied.run();
    currentPersistedSnapshot.set(persistedSnapshot);

    final var obsolete =
        persistedSnapshots.headMap(persistedSnapshot.getIndex(), false).entrySet().stream()
            .filter(entry -> !entry.getValue().isReserved())
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    obsolete.forEach(persistedSnapshots::remove);

    persistedSnapshots.put(persistedSnapshot.getIndex(), persistedSnapshot);
    listeners.forEach(listener -> listener.onNewPersistedSnapshot(persistedSnapshot));
  }

  /** 摘除指定快照；若其恰为当前快照则同时清空引用。 */
  void removeSnapshot(final InMemorySnapshot snapshot) {
    persistedSnapshots.remove(snapshot.getIndex());
    if (currentPersistedSnapshot.get() == snapshot) {
      currentPersistedSnapshot.set(null);
    }
  }

  /** 设置在快照真正提交前执行的拦截回调。 */
  public void interceptOnNewSnapshot(final Runnable interceptor) {
    beforeSnapshotApplied = interceptor;
  }
}
