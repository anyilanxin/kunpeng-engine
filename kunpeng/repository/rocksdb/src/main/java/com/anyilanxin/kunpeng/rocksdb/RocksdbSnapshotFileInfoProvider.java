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
import com.anyilanxin.kunpeng.rocksdb.util.RocksdbUtil;
import java.nio.file.Path;
import java.util.Map;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;

/** RocksDB 快照文件信息提供器，基于快照目录中各文件计算校验和与大小 */
public class RocksdbSnapshotFileInfoProvider implements SnapshotFileInfoProvider {
  /**
   * 以只读方式打开快照目录并计算其中各文件的校验和信息
   *
   * @param snapshotPath 快照目录路径
   * @return 文件名到快照文件信息的映射
   */
  @Override
  public Map<String, SnapshotFileInfo> getSnapshotFilesInfo(final Path snapshotPath) {
    try (final var db = RocksDB.openReadOnly(snapshotPath.toString())) {
      return RocksdbUtil.getChecksums(db);
    } catch (final RocksDBException e) {
      throw new RuntimeException(e);
    }
  }
}
