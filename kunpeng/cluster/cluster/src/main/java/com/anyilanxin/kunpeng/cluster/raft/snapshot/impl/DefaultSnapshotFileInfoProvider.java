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

import com.anyilanxin.kunpeng.kvstore.snapshot.SnapshotFileInfo;
import com.anyilanxin.kunpeng.kvstore.snapshot.SnapshotFileInfoProvider;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.CRC32;

/**
 * 默认逐文件校验信息计算：递归遍历镜像目录，逐文件记录 size + CRC32。
 *
 * @author zxuanhong
 * @since 1.0.0
 */
public final class DefaultSnapshotFileInfoProvider implements SnapshotFileInfoProvider {

  @Override
  public Map<String, SnapshotFileInfo> getSnapshotFilesInfo(final Path snapshotPath) {
    try (final var stream = Files.walk(snapshotPath)) {
      final Map<String, SnapshotFileInfo> fileInfos = new HashMap<>();
      stream
          .filter(Files::isRegularFile)
          .forEach(file -> fileInfos.put(snapshotPath.relativize(file).toString(), ofFile(file)));
      return fileInfos;
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /** 单文件的校验信息（CRC32 + size）。 */
  public static SnapshotFileInfo ofFile(final Path file) {
    try {
      return new SnapshotFileInfo(crc32Of(file), Files.size(file));
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /** 文件内容的 CRC32。 */
  public static long crc32Of(final Path file) {
    final var crc = new CRC32();
    try (final var input = Files.newInputStream(file)) {
      final var buffer = new byte[8192];
      int read;
      while ((read = input.read(buffer)) != -1) {
        crc.update(buffer, 0, read);
      }
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
    return crc.getValue();
  }
}
