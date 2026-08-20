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

import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotMeta;
import com.anyilanxin.kunpeng.utils.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 暂存快照（写入中）：taker 写入 {@code staging-<hex>} 目录并返回元数据 →
 * persist 时经 handler 序列化元数据写 snapshot.metadata、计算清单、
 * 目录原子改名为最终六段名并提交。
 */
public final class StagedSnapshot {

  private static final Logger LOG = LoggerFactory.getLogger(StagedSnapshot.class);

  private final SnapshotVault vault;
  private final SnapshotRef ref;
  private final Path stagingDirectory;
  private final boolean forced;

  private boolean persisted;
  private boolean aborted;

  StagedSnapshot(
      final SnapshotVault vault,
      final SnapshotRef ref,
      final Path stagingDirectory,
      final boolean forced) {
    this.vault = vault;
    this.ref = ref;
    this.stagingDirectory = stagingDirectory;
    this.forced = forced;
  }

  public SnapshotRef ref() {
    return ref;
  }

  /** 暂存目录（taker 的写入目标） */
  public Path stagingDirectory() {
    return stagingDirectory;
  }

  /** 校验 taker 写入的内容；空目录视为失败（vault 串行线程上执行） */
  public void verifyContent() {
    if (aborted || persisted) {
      throw new IllegalStateException("暂存快照已终结: " + ref);
    }
    try {
      if (!Files.exists(stagingDirectory) || VaultFiles.listFilesSorted(stagingDirectory).isEmpty()) {
        throw new SnapshotStoreException.WriteFailure(
            "暂存目录为空, taker 未写入任何内容: " + stagingDirectory, null);
      }
    } catch (final Exception e) {
      LOG.warn("快照内容校验失败: {}", ref, e);
      throw e instanceof final RuntimeException runtime ? runtime : new IllegalStateException(e);
    }
  }

  /** 序列化元数据 + 计算清单 + 原子改名 + 提交（vault 串行线程上执行） */
  public void persist(final SnapshotMeta meta) throws IOException {
    if (aborted) {
      throw new IllegalStateException("暂存快照已中止: " + ref);
    }
    if (persisted) {
      throw new IllegalStateException("暂存快照已提交: " + ref);
    }
    VaultFiles.writeDurably(
        stagingDirectory.resolve(SnapshotLayout.METADATA_FILE), vault.encodeSnapshotMeta(meta));
    final var complete = vault.buildManifest(stagingDirectory);

    final Path target = stagingDirectory.resolveSibling(ref.toString());
    // 同内容快照校验和相同→目标名相同: 上次"改名成功但清单未写"崩溃会留下同名半成品
    if (Files.exists(target)) {
      if (Files.exists(SnapshotLayout.manifestPath(target))) {
        throw new SnapshotStoreException.AlreadyExists("落档快照已存在: " + ref);
      }
      VaultFiles.deleteRecursively(target);
    }
    try {
      FileUtil.moveDurably(stagingDirectory, target, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
    } catch (final DirectoryNotEmptyException
        | java.nio.file.FileAlreadyExistsException existing) {
      throw new SnapshotStoreException.AlreadyExists("落档快照已存在: " + ref);
    }
    VaultFiles.writeDurably(SnapshotLayout.manifestPath(target), complete.encode());
    persisted = true;
    vault.commit(ref, target, complete, meta, forced);
  }

  /** 放弃：删除暂存目录（幂等） */
  public void abort() {
    if (persisted) {
      LOG.warn("已提交的暂存快照不可中止: {}", ref);
      return;
    }
    aborted = true;
    try {
      VaultFiles.deleteRecursively(stagingDirectory);
    } catch (final IOException e) {
      LOG.warn("暂存目录删除失败: {}", stagingDirectory, e);
    }
  }
}
