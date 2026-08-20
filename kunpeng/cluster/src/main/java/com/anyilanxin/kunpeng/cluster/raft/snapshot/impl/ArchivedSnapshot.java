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

import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotHandler;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotMeta;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

/** 已落档快照（只读视图；lease 期间不被淘汰清理） */
public final class ArchivedSnapshot {

  private final SnapshotRef ref;
  private final Path directory;
  private final ChecksumManifest manifest;
  private final SnapshotMeta meta;
  private final AtomicInteger leases = new AtomicInteger();

  ArchivedSnapshot(
      final SnapshotRef ref,
      final Path directory,
      final ChecksumManifest manifest,
      final SnapshotMeta meta) {
    this.ref = ref;
    this.directory = directory;
    this.manifest = manifest;
    this.meta = meta;
  }

  /**
   * 从落档目录装载：清单直接读同级 {@code <目录名>.sfv} 文件（不扫描目录），
   * 元数据读目录内 snapshot.metadata 并经 handler 反序列化。
   */
  static ArchivedSnapshot load(final Path directory, final SnapshotHandler snapshotHandler) {
    final SnapshotRef ref = SnapshotRef.parse(directory.getFileName().toString());
    try {
      final var manifestBytes = Files.readAllBytes(SnapshotLayout.manifestPath(directory));
      final var metaBytes = Files.readAllBytes(directory.resolve(SnapshotLayout.METADATA_FILE));
      final SnapshotMeta meta = snapshotHandler != null
          ? snapshotHandler.decodeSnapshotMeta(metaBytes)
          : null;
      return new ArchivedSnapshot(ref, directory, ChecksumManifest.decode(manifestBytes), meta);
    } catch (final Exception e) {
      throw new SnapshotStoreException.WriteFailure("落档目录内容不可读: " + directory, e);
    }
  }

  public SnapshotRef ref() {
    return ref;
  }

  public Path directory() {
    return directory;
  }

  public ChecksumManifest manifest() {
    return manifest;
  }

  public SnapshotMeta meta() {
    return meta;
  }

  /** 传输块读取器 */
  public BlockStreamReader blockReader(final int maxBlockBytes) {
    return new BlockStreamReader(ref.toString(), directory, manifest, maxBlockBytes);
  }

  /** 淘汰保护租约（close 后可被清理） */
  public Lease reserve() {
    leases.incrementAndGet();
    return new Lease(this);
  }

  boolean leased() {
    return leases.get() > 0;
  }

  public static final class Lease implements AutoCloseable {
    private final ArchivedSnapshot snapshot;

    private Lease(final ArchivedSnapshot snapshot) {
      this.snapshot = snapshot;
    }

    public ArchivedSnapshot snapshot() {
      return snapshot;
    }

    @Override
    public void close() {
      snapshot.leases.decrementAndGet();
    }
  }
}
