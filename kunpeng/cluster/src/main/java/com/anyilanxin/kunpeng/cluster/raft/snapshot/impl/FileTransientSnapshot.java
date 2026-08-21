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
package com.anyilanxin.kunpeng.cluster.raft.snapshot.impl;

import com.anyilanxin.kunpeng.cluster.raft.snapshot.PersistedSnapshot;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotException;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotMetadata;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.TransientSnapshot;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/** A snapshot being taken locally, written by a consumer into a temporary directory. */
final class FileTransientSnapshot implements TransientSnapshot {

  private final FileSnapshotStore store;
  private final SnapshotMetadata metadata;
  private final Path temporaryDirectory;

  FileTransientSnapshot(
      final FileSnapshotStore store,
      final SnapshotMetadata metadata,
      final Path temporaryDirectory) {
    this.store = store;
    this.metadata = metadata;
    this.temporaryDirectory = temporaryDirectory;
  }

  @Override
  public CompletableFuture<Void> take(final Consumer<Path> writer) {
    return CompletableFuture.runAsync(
        () -> {
          try {
            Files.createDirectories(temporaryDirectory);
          } catch (final IOException e) {
            throw new UncheckedIOException(e);
          }
          writer.accept(temporaryDirectory);
        });
  }

  @Override
  public CompletableFuture<PersistedSnapshot> commit() {
    return CompletableFuture.supplyAsync(
        () -> {
          final SnapshotManifest manifest = SnapshotManifest.of(metadata, temporaryDirectory);
          // 空快照拒绝：目录内除 manifest 外没有任何内容文件
          if (manifest.files.isEmpty()) {
            abort();
            throw new SnapshotException(
                "Cannot commit snapshot " + metadata.getSnapshotIdAsString()
                    + "; snapshot content is empty");
          }
          manifest.write(temporaryDirectory);
          return store.commit(manifest, temporaryDirectory);
        });
  }

  @Override
  public void abort() {
    FilePersistedSnapshot.deleteRecursively(temporaryDirectory);
  }

  @Override
  public String toString() {
    return "FileTransientSnapshot{metadata=" + metadata + ", path=" + temporaryDirectory + '}';
  }
}
