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

import com.anyilanxin.kunpeng.configuration.broker.rocksdb.RocksdbConfiguration;
import com.anyilanxin.kunpeng.kvstore.ColumnFamilies;
import java.nio.file.Path;
import org.rocksdb.ColumnFamilyHandle;
import org.rocksdb.RocksDBException;

/**
 * 拆分载荷写出器：在目标目录创建全新 RocksDB，把裸 kv（带虚拟列族前缀，与拆分访问器 {@code writeKey/writeValue} 产出同构）
 * 写入本地物理列族；关闭后目录即为可被 {@code KvStore#merge} 摄入的镜像形态（与 {@code RocksdbUtil#createCopy} 产物同构）。
 *
 * <p>构造参数 {@code anyLocalFamily} 取任一本地（LOCAL）列族即可——本地列族共享同一物理列族，仅用于定位句柄。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class SplitPayloadWriter<ColumnFamilyType extends ColumnFamilies>
    implements AutoCloseable {
  private final RocksdbTransactionDb<ColumnFamilyType> db;
  private final ColumnFamilyHandle localFamily;

  public SplitPayloadWriter(final Path path, final ColumnFamilyType anyLocalFamily) {
    db =
        new DefaultRocksdbFactory<ColumnFamilyType>()
            .createDb(path.toString(), null, 0, new RocksdbConfiguration(), anyLocalFamily);
    localFamily = db.columnFamilyHandle(anyLocalFamily);
  }

  /** 写入一条裸 kv（WAL 落盘，close 后读侧开库可见） */
  public void write(final byte[] key, final byte[] value) {
    try {
      db.getRocksdb().put(localFamily, key, value);
    } catch (final RocksDBException e) {
      throw new RuntimeException("Failed to write split payload kv", e);
    }
  }

  @Override
  public void close() {
    try {
      db.close();
    } catch (final Exception e) {
      throw new RuntimeException("Failed to close split payload db", e);
    }
  }
}
