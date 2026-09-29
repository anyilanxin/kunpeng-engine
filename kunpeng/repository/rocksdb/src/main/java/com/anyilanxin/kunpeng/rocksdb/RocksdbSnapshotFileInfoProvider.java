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
package com.anyilanxin.kunpeng.rocksdb;

import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotFileInfo;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.SnapshotFileInfoProvider;
import com.anyilanxin.kunpeng.cluster.raft.snapshot.impl.DefaultSnapshotFileInfoProvider;
import com.anyilanxin.kunpeng.rocksdb.util.RocksdbUtil;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;

/**
 * RocksDB 快照文件信息提供器：清单以目录全文件遍历为准——RocksDB 能给出的校验信息（sst 文件校验和）优先使用， RocksDB
 * 清单之外的文件（MANIFEST/CURRENT/OPTIONS 及其他业务文件）逐文件计算 CRC32 补齐。
 *
 * <p>分片传输严格按清单进行，缺文件会让接收端目录不再是完整可打开的 RocksDB，因此清单必须覆盖快照目录全部文件。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class RocksdbSnapshotFileInfoProvider implements SnapshotFileInfoProvider {

  /**
   * 以只读方式打开快照目录取得 RocksDB 级校验信息，再遍历目录全部文件组装清单（缺失项逐文件补齐）。
   *
   * @param snapshotPath 快照目录路径
   * @return 文件名到快照文件信息的映射
   */
  @Override
  public Map<String, SnapshotFileInfo> getSnapshotFilesInfo(final Path snapshotPath) {
    Map<String, SnapshotFileInfo> provided = Map.of();
    try (final var db = RocksDB.openReadOnly(snapshotPath.toString())) {
      provided = RocksdbUtil.getChecksums(db);
    } catch (final RocksDBException e) {
      // 目录不是完整可打开的 RocksDB（如仅含业务文件的镜像）：全部退化为逐文件 CRC32
    }
    final Map<String, SnapshotFileInfo> providedByRocksdb = provided;
    final Map<String, SnapshotFileInfo> fileInfos = new HashMap<>();
    try (final var stream = Files.walk(snapshotPath)) {
      stream
          .filter(Files::isRegularFile)
          .forEach(
              file -> {
                final String name = snapshotPath.relativize(file).toString();
                final SnapshotFileInfo rocksdbInfo = providedByRocksdb.get(name);
                fileInfos.put(
                    name,
                    rocksdbInfo != null
                        ? rocksdbInfo
                        : DefaultSnapshotFileInfoProvider.ofFile(file));
              });
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
    return fileInfos;
  }
}
