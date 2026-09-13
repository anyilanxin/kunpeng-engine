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

import com.anyilanxin.kunpeng.cluster.raft.snapshot.SimpleFileVerificationInfo;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotMetadata;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotFileInfo;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotFileInfoProvider;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.zip.CRC32;

/**
 * SFV 格式 .sfc 校验文件的内存形态：逐文件校验集 + 文件路径。
 *
 * @author zxuanhong
 * @since 1.0.0
 */
public final class SfvFileVerificationInfo implements SimpleFileVerificationInfo {

  private final Path sfvPath;
  private final Map<String, SnapshotFileInfo> fileInfos;

  public SfvFileVerificationInfo(
      final Path sfvPath, final Map<String, SnapshotFileInfo> fileInfos) {
    this.sfvPath = sfvPath;
    this.fileInfos = Map.copyOf(fileInfos);
  }

  @Override
  public Path getSfvPath() {
    return sfvPath;
  }

  @Override
  public Map<String, SnapshotFileInfo> getFileInfos() {
    return fileInfos;
  }

  /** 整体校验和：按文件名排序，逐文件把文件名、size、checksum 并入 CRC32。 */
  @Override
  public long getCombinedChecksum() {
    final var crc = new CRC32();
    fileInfos.entrySet().stream()
        .sorted(Map.Entry.comparingByKey())
        .forEach(
            entry -> {
              crc.update(entry.getKey().getBytes(StandardCharsets.UTF_8));
              crc.update(ByteBuffer.allocate(Long.BYTES).putLong(entry.getValue().size()).array());
              crc.update(
                  ByteBuffer.allocate(Long.BYTES).putLong(entry.getValue().checksum()).array());
            });
    return crc.getValue();
  }

  /**
   * 清单校验：仅核对 {@code .sfc} 清单内文件，目录中多出的文件不参与校验。非元数据文件的校验信息由 拍摄该镜像时的 {@code provider}
   * 重新计算比对（校验算法与拍摄对称，清单仅元数据时不调用）； {@code snapshot.metadata} 的校验值 固定由默认提供方（内容 CRC32）生成，不随分区 provider
   * 变化。
   */
  @Override
  public boolean verify(final Path snapshotDirectory, final SnapshotFileInfoProvider provider) {
    // 惰性调用：清单里出现非元数据条目时才用拍摄时的 provider 重算（仅元数据的空镜像不触发，如 rocksdb 提供方打开空目录会失败）
    Map<String, SnapshotFileInfo> recomputed = null;
    for (final var entry : fileInfos.entrySet()) {
      final Path file = snapshotDirectory.resolve(entry.getKey());
      try {
        if (!Files.isRegularFile(file)) {
          return false;
        }
        if (SnapshotMetadata.METADATA_FILE_NAME.equals(entry.getKey())) {
          if (Files.size(file) != entry.getValue().size()
              || DefaultSnapshotFileInfoProvider.crc32Of(file) != entry.getValue().checksum()) {
            return false;
          }
        } else {
          if (recomputed == null) {
            try {
              recomputed = provider.getSnapshotFilesInfo(snapshotDirectory);
            } catch (final Exception e) {
              return false;
            }
          }
          final var info = recomputed.get(entry.getKey());
          if (info == null
              || !entry.getValue().size().equals(info.size())
              || !entry.getValue().checksum().equals(info.checksum())) {
            return false;
          }
        }
      } catch (final IOException e) {
        return false;
      }
    }
    return true;
  }
}
