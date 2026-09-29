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

import com.anyilanxin.kunpeng.cluster.raft.snapshot.constructable.ConstructableSnapshot;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.receive.ReceivedSnapshot;
import com.anyilanxin.kunpeng.scheduler.future.ActorFuture;
import com.anyilanxin.kunpeng.scheduler.future.CompletableActorFuture;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * 面向测试的内存版镜像存储，用于在无磁盘依赖的情况下模拟镜像的拍摄、接收与持久化。
 */
public class TestSnapshotStore implements RaftSnapshotStore {

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

  @Override
  public Optional<PersistedSnapshot> getLatestSnapshot() {
    return Optional.ofNullable(currentPersistedSnapshot.get());
  }

  /**
   * 返回索引不大于给定值的最近快照（测试辅助方法，非接口）。
   */
  public Optional<InMemorySnapshot> getSnapshotAt(final long index) {
    return Optional.ofNullable(persistedSnapshots.floorEntry(index)).map(Map.Entry::getValue);
  }

  @Override
  public ActorFuture<Long> getCompactionBound() {
    final var earliest = persistedSnapshots.firstEntry();
    return CompletableActorFuture.completed(earliest == null ? 0L : earliest.getKey());
  }

  @Override
  public int getMaxSnapshotCount() {
    return Integer.MAX_VALUE;
  }

  @Override
  public ActorFuture<Void> abortPendingSnapshots() {
    receivedSnapshots.clear();
    return CompletableActorFuture.completed();
  }

  @Override
  public void start() {
  }

  @Override
  public ActorFuture<ConstructableSnapshot> newTransientSnapshot(final long index, final long term) {
    final var pending = new InMemorySnapshot(this, index, term, Integer.parseInt(nodeId));
    return CompletableActorFuture.completed(pending);
  }

  @Override
  public ActorFuture<ReceivedSnapshot> newReceivedSnapshot(final String snapshotId) {
    final var incoming = new InMemorySnapshot(this, snapshotId);
    receivedSnapshots.add(incoming);
    return CompletableActorFuture.completed(incoming);
  }

  @Override
  public ActorFuture<Boolean> addSnapshotListener(final PersistedSnapshotListener listener) {
    listeners.add(listener);
    return CompletableActorFuture.completed(true);
  }

  @Override
  public ActorFuture<Boolean> removeSnapshotListener(final PersistedSnapshotListener listener) {
    listeners.remove(listener);
    return CompletableActorFuture.completed(true);
  }

  @Override
  public long getCurrentSnapshotIndex() {
    final var latest = currentPersistedSnapshot.get();
    return latest == null ? 0 : latest.getIndex();
  }

  @Override
  public ActorFuture<Void> delete() {
    persistedSnapshots.clear();
    currentPersistedSnapshot.set(null);
    return CompletableActorFuture.completed();
  }

  @Override
  public Path getPath() {
    return null;
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
