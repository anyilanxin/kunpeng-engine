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
import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotException.SnapshotNotFoundException;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.impl.DefaultFileSnapshotStore;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.impl.DefaultSnapshotFileInfoProvider;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.impl.FilePersistedSnapshot;
import com.anyilanxin.kunpeng.kvstore.snapshot.SnapshotFileInfo;
import com.anyilanxin.kunpeng.kvstore.snapshot.SnapshotFileInfoProvider;
import com.anyilanxin.kunpeng.scheduler.ConcurrencyControl;
import com.anyilanxin.kunpeng.scheduler.future.ActorFuture;
import com.anyilanxin.kunpeng.scheduler.future.CompletableActorFuture;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 文件版 pending 镜像：内容由构造 store 时传入的 {@link SnapshotProvider} 拍摄到临时目录， {@link #persist()}
 * 时生成元数据文件与校验集，原子 move 到正式目录后由存储生成 .sfc 完成提交。
 *
 * @author zxuanhong
 * @since 1.0.0
 */
final class DefaultConstructableSnapshot implements ConstructableSnapshot {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultConstructableSnapshot.class);

  private final SnapshotId snapshotId;
  // 业务信息键值清单：由 SnapshotProvider.takeSnapshot 的返回值提供
  private Map<String, Object> metaInfo = Map.of();
  private final Path directory;
  private final DefaultFileSnapshotStore store;
  private final ConcurrencyControl actor;
  private final SnapshotFileInfoProvider fileInfoProvider;
  private final CompletableActorFuture<Void> takenFuture = new CompletableActorFuture<>();
  // take 成功后为 true；abort 后失效
  private boolean valid;
  private Map<String, SnapshotFileInfo> fileInfos;
  private PersistedSnapshot snapshot;

  DefaultConstructableSnapshot(
      final SnapshotId snapshotId,
      final Path directory,
      final DefaultFileSnapshotStore store,
      final ConcurrencyControl actor,
      final SnapshotFileInfoProvider fileInfoProvider) {
    this.snapshotId = snapshotId;
    this.directory = directory;
    this.store = store;
    this.actor = actor;
    this.fileInfoProvider = fileInfoProvider;
  }

  /** 拍摄入口：由 store 在创建时驱动，返回的 future 完成即内容已写好、校验集已算好。 */
  ActorFuture<Void> take(final SnapshotProvider provider) {
    actor.run(() -> takeInternal(provider));
    return takenFuture;
  }

  private void takeInternal(final SnapshotProvider provider) {
    try {
      final Map<String, Object> takenMetaInfo = provider.takeSnapshot(directory);
      metaInfo = takenMetaInfo == null ? Map.of() : Map.copyOf(takenMetaInfo);
      // 业务未写入内容也是合法拍摄：persist 时写入的元数据文件保证镜像非空（仅含元数据的空镜像）
      fileInfos =
          isEmpty(directory)
              ? new LinkedHashMap<>()
              : new LinkedHashMap<>(fileInfoProvider.getSnapshotFilesInfo(directory));
      valid = true;
      takenFuture.complete(null);
    } catch (final Exception e) {
      LOGGER.warn("Unexpected exception on taking snapshot ({})", snapshotId, e);
      abortInternal();
      takenFuture.completeExceptionally(e);
    }
  }

  private static boolean isEmpty(final Path directory) throws IOException {
    try (final var entries = Files.list(directory)) {
      return entries.findAny().isEmpty();
    }
  }

  @Override
  public ActorFuture<PersistedSnapshot> persist() {
    final CompletableActorFuture<PersistedSnapshot> future = new CompletableActorFuture<>();
    actor.run(() -> persistInternal(future));
    return future;
  }

  private void persistInternal(final CompletableActorFuture<PersistedSnapshot> future) {
    if (snapshot != null) {
      future.complete(snapshot);
      return;
    }
    if (!takenFuture.isDone() || takenFuture.isCompletedExceptionally()) {
      future.completeExceptionally(new IllegalStateException("Snapshot is not taken"));
      return;
    }
    if (!valid) {
      future.completeExceptionally(
          new SnapshotNotFoundException("Snapshot may have been already deleted."));
      return;
    }
    try {
      // 元数据文件写入镜像目录并计入校验集
      final var metadata = SnapshotMetadata.of(snapshotId, metaInfo);
      final Path metadataPath = metadata.writeTo(directory);
      fileInfos.put(
          SnapshotMetadata.METADATA_FILE_NAME,
          DefaultSnapshotFileInfoProvider.ofFile(metadataPath));

      final Path destination = store.getPath().resolve(snapshotId.asString());
      if (Files.exists(destination)) {
        // 启动不清理磁盘残留（如提交中断缺 .sfc），这里覆盖以允许同 id 重新提交；走到此处时该 id 未登记，必为残留
        LOGGER.warn("Overwriting unregistered snapshot residue at {}", destination);
        FilePersistedSnapshot.deleteRecursively(destination);
      }
      Files.move(directory, destination, StandardCopyOption.ATOMIC_MOVE);
      snapshot = store.persistNewSnapshot(destination, snapshotId, fileInfos, metadata);
      future.complete(snapshot);
    } catch (final AtomicMoveNotSupportedException e) {
      future.completeExceptionally(
          new SnapshotException("Atomic move is not supported by the file system", e));
    } catch (final Exception e) {
      future.completeExceptionally(e);
    } finally {
      store.removePending(this);
    }
  }

  @Override
  public ActorFuture<Void> abort() {
    final CompletableActorFuture<Void> future = new CompletableActorFuture<>();
    actor.run(
        () -> {
          abortInternal();
          future.complete(null);
        });
    return future;
  }

  private void abortInternal() {
    try {
      valid = false;
      snapshot = null;
      LOGGER.debug("Aborting pending snapshot {}", this);
      FilePersistedSnapshot.deleteRecursively(directory);
    } finally {
      store.removePending(this);
    }
  }

  @Override
  public SnapshotId snapshotId() {
    return snapshotId;
  }

  @Override
  public Path getPath() {
    return directory;
  }

  @Override
  public String toString() {
    return "DefaultConstructableSnapshot{directory="
        + directory
        + ", snapshotId="
        + snapshotId
        + '}';
  }
}
