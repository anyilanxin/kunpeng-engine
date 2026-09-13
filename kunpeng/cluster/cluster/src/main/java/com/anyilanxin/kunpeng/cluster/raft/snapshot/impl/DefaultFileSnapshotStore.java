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
import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotException.SnapshotAlreadyExistsException;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotFileInfo;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotFileInfoProvider;
import com.anyilanxin.kunpeng.scheduler.ConcurrencyControl;
import com.anyilanxin.kunpeng.scheduler.future.ActorFuture;
import com.anyilanxin.kunpeng.scheduler.future.CompletableActorFuture;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 文件镜像存储公共实现：承载与模块无关的全部存储逻辑——目录与临时目录管理、启动加载与 内容校验、.sfc 提交标记、保留策略、监听器、删除。
 *
 * <p>各模块（拍摄/接收/传输）的 store 在构造时创建并组合本类，只实现自己的 pending 入口； {@link #persistNewSnapshot}/{@link
 * #newTemporaryDirectory}/{@link #addPending}/ {@link #removePending}/{@link #currentSnapshot}
 * 为模块内部 SPI，供各模块的 pending 实现调用，不属于对外契约。所有状态变更串行在持有的 actor 上执行。
 *
 * @author zxuanhong
 * @since 1.0.0
 */
public final class DefaultFileSnapshotStore implements FileSnapshotStore {

  static final String TEMP_DIRECTORY_PREFIX = "tmp-";
  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultFileSnapshotStore.class);

  private final String nodeId;
  private final Path snapshotPath;
  private final int maxSnapshotCount;
  private final SimpleFileVerificationStore simpleFileVerificationStore;
  // 启动加载校验用的文件信息提供方，与拍摄时的 provider 一致（校验算法对称）
  private final SnapshotFileInfoProvider snapshotFileInfoProvider;
  // 所有状态变更串行在该 actor 上执行
  private final ConcurrencyControl actor;

  private final Set<PersistedSnapshotListener> listeners = new CopyOnWriteArraySet<>();
  // 进行中的 pending 镜像，供 abortPendingSnapshots 统一清理
  private final Set<PersistableSnapshot> pendingSnapshots = new HashSet<>();
  private final Set<FilePersistedSnapshot> availableSnapshots = new HashSet<>();
  // 查询不经 actor，直接读引用
  private final AtomicReference<FilePersistedSnapshot> currentSnapshot = new AtomicReference<>();
  private final AtomicLong temporaryDirectoryIds = new AtomicLong();

  public DefaultFileSnapshotStore(
      final String nodeId,
      final Path snapshotPath,
      final int maxSnapshotCount,
      final SimpleFileVerificationStore simpleFileVerificationStore,
      final SnapshotFileInfoProvider snapshotFileInfoProvider,
      final ConcurrencyControl actor) {
    this.nodeId = nodeId;
    this.snapshotPath = snapshotPath;
    this.maxSnapshotCount = maxSnapshotCount;
    this.simpleFileVerificationStore = simpleFileVerificationStore;
    this.snapshotFileInfoProvider = snapshotFileInfoProvider;
    this.actor = actor;
    try {
      Files.createDirectories(snapshotPath);
    } catch (final IOException e) {
      throw new UncheckedIOException("Failed to create snapshot directory " + snapshotPath, e);
    }
    purgeTemporaryDirectories();
  }

  /** 本节点 id（生成镜像 id 用）。 */
  public String nodeId() {
    return nodeId;
  }

  /** 当前最新镜像（不经 actor，直接读引用）。 */
  public FilePersistedSnapshot currentSnapshot() {
    return currentSnapshot.get();
  }

  /** 新建临时目录（{@code tmp-} 前缀，启动时统一清理）。 */
  public Path newTemporaryDirectory() {
    return snapshotPath.resolve(
        TEMP_DIRECTORY_PREFIX + System.nanoTime() + "-" + temporaryDirectoryIds.incrementAndGet());
  }

  /** 登记 pending 镜像。 */
  public void addPending(final PersistableSnapshot pending) {
    pendingSnapshots.add(pending);
  }

  /** pending 提交完成/放弃时移出跟踪列表。 */
  public void removePending(final PersistableSnapshot pending) {
    pendingSnapshots.remove(pending);
  }

  /** 提交已就位的镜像目录：生成 .sfc 校验文件（提交完成标记）、登记为最新、按保留策略删旧、 通知监听器。 */
  public PersistedSnapshot persistNewSnapshot(
      final Path destination,
      final SnapshotId snapshotId,
      final Map<String, SnapshotFileInfo> fileInfos,
      final SnapshotMetadata metadata) {
    // 并发场景兜底：persist 前已有更新的镜像提交，则本次提交作废并回滚已 move 的目录
    final var current = currentSnapshot.get();
    if (current != null && current.snapshotId().compareTo(snapshotId) > 0) {
      FilePersistedSnapshot.deleteRecursively(destination);
      throw new SnapshotAlreadyExistsException(
          "Cannot commit snapshot "
              + snapshotId
              + "; a newer snapshot exists: "
              + current.snapshotId());
    }

    final SimpleFileVerificationInfo verificationInfo;
    try {
      verificationInfo =
          simpleFileVerificationStore.newFileVerificationInfo(destination, fileInfos);
    } catch (final Exception e) {
      // 校验文件写失败：回滚已 move 的镜像目录
      FilePersistedSnapshot.deleteRecursively(destination);
      throw e;
    }
    // 父目录 fsync，保证 rename 元数据落盘
    try (final var dirChannel = FileChannel.open(snapshotPath)) {
      dirChannel.force(true);
    } catch (final IOException e) {
      LOGGER.debug("Failed to fsync snapshot directory for {}", snapshotId, e);
    }

    final var persisted =
        new FilePersistedSnapshot(destination, verificationInfo, snapshotId, metadata);
    // 同 id 强制重拍覆盖提交：先移除旧登记，避免保留策略按相等的 id 任意排序时误删新镜像
    availableSnapshots.removeIf(existing -> existing.snapshotId().equals(snapshotId));
    currentSnapshot.set(persisted);
    availableSnapshots.add(persisted);
    enforceRetention();
    LOGGER.debug("Committed new snapshot {} at {}", snapshotId, destination);
    // 镜像已登记即持久化成功: 监听器异常只告警不回传, 否则调用方会把半提交误报为失败
    listeners.forEach(
        listener -> {
          try {
            listener.onNewPersistedSnapshot(persisted);
          } catch (final Exception e) {
            LOGGER.warn("Snapshot persisted listener failed after committing {}", snapshotId, e);
          }
        });
    return persisted;
  }

  /**
   * 保留策略：作用于本存储内全部 raft 相关镜像（本节点拍摄 + raft 复制接收），保留最新 {@code max(1, maxSnapshotCount)} 个，其余删除（含
   * .sfc）。bootstrap/merge 镜像位于各自类型目录的独立 store， 由外部流程管控生命周期，不经过本策略。
   */
  private void enforceRetention() {
    final int keepCount = Math.max(1, maxSnapshotCount);
    if (availableSnapshots.size() <= keepCount) {
      return;
    }
    final var retained =
        availableSnapshots.stream()
            .sorted(Comparator.comparing(FilePersistedSnapshot::snapshotId).reversed())
            .toList();
    retained.subList(keepCount, retained.size()).forEach(this::deleteSnapshot);
  }

  private void deleteSnapshot(final FilePersistedSnapshot snapshot) {
    availableSnapshots.remove(snapshot);
    FilePersistedSnapshot.deleteRecursively(snapshot.getPath());
    simpleFileVerificationStore.delete(snapshot.getPath());
    LOGGER.debug("Deleted snapshot {} exceeding the retention policy", snapshot.snapshotId());
  }

  private void loadSnapshots() {
    try (final var entries = Files.list(snapshotPath)) {
      entries.filter(Files::isDirectory).forEach(this::loadSnapshot);
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
    // 启动只加载不清理：无效目录仅跳过告警，历史镜像不得在启动时删除；提交中断的残留由同 id 重新提交时覆盖
    currentSnapshot.set(
        availableSnapshots.stream()
            .max(Comparator.comparing(FilePersistedSnapshot::snapshotId))
            .orElse(null));
  }

  /** 加载单个镜像目录；任何无效情况（目录名不可解析、缺 .sfc、元数据不可读、校验不过）只告警跳过，绝不删除。 */
  private void loadSnapshot(final Path directory) {
    final SnapshotId snapshotId;
    try {
      snapshotId = SnapshotId.fromString(directory.getFileName().toString());
    } catch (final IllegalArgumentException e) {
      LOGGER.warn("Snapshot directory {} has an unparseable name, skipping it", directory, e);
      return;
    }
    // 缺失 .sfc 标记 = 未完整提交的残留，保留在磁盘，由同 id 重新提交时覆盖
    if (!simpleFileVerificationStore.exists(directory)) {
      LOGGER.warn("Snapshot directory {} has no verification marker, skipping it", directory);
      return;
    }
    final SimpleFileVerificationInfo verificationInfo;
    final SnapshotMetadata metadata;
    try {
      verificationInfo = simpleFileVerificationStore.load(directory);
      metadata = SnapshotMetadata.readFrom(directory, snapshotId);
    } catch (final Exception e) {
      LOGGER.warn("Snapshot directory {} cannot be loaded, skipping it", directory, e);
      return;
    }
    if (!verificationInfo.verify(directory, snapshotFileInfoProvider)) {
      LOGGER.warn("Snapshot directory {} failed content verification, skipping it", directory);
      return;
    }
    availableSnapshots.add(
        new FilePersistedSnapshot(directory, verificationInfo, snapshotId, metadata));
  }

  private void purgeTemporaryDirectories() {
    try (final var directories = Files.list(snapshotPath)) {
      directories
          .filter(Files::isDirectory)
          .filter(dir -> dir.getFileName().toString().startsWith(TEMP_DIRECTORY_PREFIX))
          .forEach(FilePersistedSnapshot::deleteRecursively);
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public void start() {
    purgeTemporaryDirectories();
    loadSnapshots();
  }

  @Override
  public ActorFuture<Void> abortPendingSnapshots() {
    final CompletableActorFuture<Void> future = new CompletableActorFuture<>();
    actor.run(
        () -> {
          final var aborted = pendingSnapshots.stream().map(PersistableSnapshot::abort).toList();
          actor.runOnCompletion(
              aborted,
              error -> {
                if (error != null) {
                  future.completeExceptionally(error);
                } else {
                  purgeTemporaryDirectories();
                  future.complete(null);
                }
              });
        });
    return future;
  }

  @Override
  public Optional<PersistedSnapshot> getLatestSnapshot() {
    return Optional.ofNullable(currentSnapshot.get());
  }

  @Override
  public ActorFuture<Boolean> addSnapshotListener(final PersistedSnapshotListener listener) {
    return actor.call(() -> listeners.add(listener));
  }

  @Override
  public ActorFuture<Boolean> removeSnapshotListener(final PersistedSnapshotListener listener) {
    return actor.call(() -> listeners.remove(listener));
  }

  @Override
  public long getCurrentSnapshotIndex() {
    return getLatestSnapshot().map(PersistedSnapshot::getIndex).orElse(0L);
  }

  @Override
  public int getMaxSnapshotCount() {
    return maxSnapshotCount;
  }

  @Override
  public ActorFuture<Long> getCompactionBound() {
    return actor.call(
        () ->
            availableSnapshots.stream()
                .mapToLong(FilePersistedSnapshot::getIndex)
                .min()
                .orElse(0L));
  }

  /** 删除整个镜像根目录并清空内存状态。 */
  @Override
  public ActorFuture<Void> delete() {
    return actor.call(
        () -> {
          currentSnapshot.set(null);
          availableSnapshots.clear();
          pendingSnapshots.clear();
          FilePersistedSnapshot.deleteRecursively(snapshotPath);
          return null;
        });
  }

  /**
   * 删除指定镜像（磁盘目录与 .sfc，含内存登记）；被删除的是当前最新镜像时，currentSnapshot 回退到 剩余最新者（无剩余则为
   * null）。供外部管控生命周期的镜像（如 bootstrap）按需删除。
   */
  public ActorFuture<Void> deleteSnapshot(final SnapshotId snapshotId) {
    return actor.call(
        () -> {
          availableSnapshots.stream()
              .filter(snapshot -> snapshot.snapshotId().equals(snapshotId))
              .toList()
              .forEach(this::deleteSnapshot);
          currentSnapshot.set(
              availableSnapshots.stream()
                  .max(Comparator.comparing(FilePersistedSnapshot::snapshotId))
                  .orElse(null));
          return null;
        });
  }

  /** 删除本存储内全部已提交镜像（磁盘与内存），保留根目录本身；引导镜像不跨重启存活时用于启动清理。 */
  public ActorFuture<Void> deleteAllSnapshots() {
    return actor.call(
        () -> {
          availableSnapshots.forEach(
              snapshot -> FilePersistedSnapshot.deleteRecursively(snapshot.getPath()));
          availableSnapshots.forEach(
              snapshot -> simpleFileVerificationStore.delete(snapshot.getPath()));
          availableSnapshots.clear();
          currentSnapshot.set(null);
          return null;
        });
  }

  @Override
  public Path getPath() {
    return snapshotPath;
  }
}
