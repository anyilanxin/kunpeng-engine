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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 接收中的远端副本：块按 (文件,偏移) 乱序定位写入目标目录；全部块到齐且校验一致后
 * persist 写 manifest.bin 提交（提交前目录无清单，扫描时按部分写清理）。
 */
public final class IncomingReplica {

  private static final Logger LOG = LoggerFactory.getLogger(IncomingReplica.class);

  private final SnapshotVault vault;
  private final SnapshotRef ref;
  private final Path targetDirectory;

  private int expectedBlockCount;

  private final Set<String> receivedBlocks = new HashSet<>();
  private final Map<String, Long> receivedBytes = new HashMap<>();
  private final Map<String, Long> fileSizes = new HashMap<>();
  private final TreeMap<Long, byte[]> metadataParts = new TreeMap<>();
  private final ChecksumManifest manifest = ChecksumManifest.empty();

  private boolean persisted;
  private boolean aborted;

  IncomingReplica(
      final SnapshotVault vault,
      final SnapshotRef ref,
      final int expectedBlockCount,
      final Path targetDirectory) {
    this.vault = vault;
    this.ref = ref;
    this.expectedBlockCount = expectedBlockCount;
    this.targetDirectory = targetDirectory;
  }

  /** 首块到达时登记总块数；后续块校验一致 */
  void registerBlockCount(final int blockCount) {
    if (expectedBlockCount < 0) {
      expectedBlockCount = blockCount;
    } else if (expectedBlockCount != blockCount) {
      throw new SnapshotStoreException.Corrupted(
          "总块数不一致: 期望 " + expectedBlockCount + " 实际 " + blockCount);
    }
  }

  public SnapshotRef ref() {
    return ref;
  }

  /** 目标写入目录 */
  public Path targetDirectory() {
    return targetDirectory;
  }

  /** 应用一个块（乱序安全；重复块幂等跳过） */
  public void apply(final SnapshotBlock block) throws IOException {
    if (aborted || persisted) {
      throw new IllegalStateException("接收副本已终结: " + ref);
    }
    if (!block.snapshotId().equals(ref.toString())) {
      throw new SnapshotStoreException.Corrupted(
          "块归属不符: 期望 " + ref + " 实际 " + block.snapshotId());
    }
    registerBlockCount(block.blockCount());
    final String blockName = block.blockName();
    if (!receivedBlocks.add(blockName)) {
      LOG.debug("重复块跳过: {}", blockName);
      return;
    }
    if (!block.verifyCrc()) {
      throw new SnapshotStoreException.Corrupted("块 CRC 不符: " + blockName);
    }
    final int separator = blockName.lastIndexOf(':');
    final String fileName = blockName.substring(0, separator);

    if (SnapshotLayout.METADATA_FILE.equals(fileName)) {
      metadataParts.put(block.fileOffset(), block.payload().clone());
      receivedBytes.merge(fileName, (long) block.payload().length, Long::sum);
      fileSizes.putIfAbsent(fileName, block.fileSize());
      return;
    }
    VaultFiles.positionedWrite(
        targetDirectory.resolve(fileName), block.fileOffset(), block.payload());
    receivedBytes.merge(fileName, (long) block.payload().length, Long::sum);
    fileSizes.putIfAbsent(fileName, block.fileSize());
  }

  /** 全部块到齐 → 写 manifest.bin 并经 vault 提交 */
  public void persist() throws IOException {
    if (aborted) {
      throw new IllegalStateException("接收副本已中止: " + ref);
    }
    if (persisted) {
      throw new IllegalStateException("接收副本已提交: " + ref);
    }
    if (receivedBlocks.size() != expectedBlockCount) {
      throw new SnapshotStoreException.Corrupted(
          "块数不符: 期望 " + expectedBlockCount + " 实际 " + receivedBlocks.size());
    }
    if (!metadataParts.isEmpty()) {
      final byte[] metadata = assembleMetadata();
      VaultFiles.writeDurably(targetDirectory.resolve(SnapshotLayout.METADATA_FILE), metadata);
    }
    // 逐完成文件计 CRC（文件大小与接收字节一致才计入）
    for (final String fileName : VaultFiles.listFilesSorted(targetDirectory)) {
      if (SnapshotLayout.METADATA_FILE.equals(fileName)) {
        continue;
      }
      final Long size = fileSizes.get(fileName);
      final Long bytes = receivedBytes.get(fileName);
      if (size == null || bytes == null || size.longValue() != bytes.longValue()) {
        throw new SnapshotStoreException.Corrupted("文件不完整: " + fileName);
      }
      manifest.add(fileName, VaultFiles.fileCrc(targetDirectory.resolve(fileName)));
    }
    if (Files.exists(targetDirectory.resolve(SnapshotLayout.METADATA_FILE))) {
      manifest.add(
          SnapshotLayout.METADATA_FILE,
          VaultFiles.fileCrc(targetDirectory.resolve(SnapshotLayout.METADATA_FILE)));
    }
    ref.checksum(Long.toHexString(manifest.combined()));
    VaultFiles.writeDurably(
        targetDirectory.resolve(SnapshotLayout.MANIFEST_FILE), manifest.encode());
    persisted = true;
    vault.commit(
        ref,
        targetDirectory,
        manifest,
        SnapshotMeta.decode(
            Files.readAllBytes(targetDirectory.resolve(SnapshotLayout.METADATA_FILE))),
        false);
  }

  private byte[] assembleMetadata() {
    final long total = metadataParts.values().stream().mapToLong(a -> a.length).sum();
    final byte[] out = new byte[(int) total];
    metadataParts.forEach(
        (offset, window) -> System.arraycopy(window, 0, out, offset.intValue(), window.length));
    return out;
  }

  /** 放弃：删除目标目录（幂等） */
  public void abort() {
    if (persisted) {
      LOG.warn("已提交的接收副本不可中止: {}", ref);
      return;
    }
    aborted = true;
    try {
      VaultFiles.deleteRecursively(targetDirectory);
    } catch (final IOException e) {
      LOG.warn("接收目录删除失败: {}", targetDirectory, e);
    }
  }

  public int receivedCount() {
    return receivedBlocks.size();
  }
}
