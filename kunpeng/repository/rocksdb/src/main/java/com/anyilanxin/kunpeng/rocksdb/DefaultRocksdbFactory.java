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
import io.micrometer.core.instrument.MeterRegistry;
import java.util.ArrayList;
import java.util.List;
import org.rocksdb.RocksDB;

/**
 * RocksDB 数据库工厂的默认实现。{@link ColumnFamilyNames} 必须是枚举类型，用于定义 RocksDB 数据库中的各个列族。
 *
 * @param <ColumnFamilyNames> 列族名称
 * @author zxuanhong
 */
public final class DefaultRocksdbFactory<ColumnFamilyNames extends ColumnFamilies>
    implements RocksdbFactory<ColumnFamilyNames> {
  static {
    // 加载RocksDB本地库
    RocksDB.loadLibrary();
  }

  private final List<AutoCloseable> closeables;

  /** 构造工厂实例，并初始化待关闭资源列表。 */
  public DefaultRocksdbFactory() {
    closeables = new ArrayList<>();
  }

  @Override
  public RocksdbTransactionDb<ColumnFamilyNames> createDb(
      final String pathName,
      final MeterRegistry registry,
      final int partitionId,
      final RocksdbConfiguration rocksDbConfiguration,
      final ColumnFamilies columnFamilies) {
    final RocksdbTransactionDb<ColumnFamilyNames> transactionDb =
        new RocksdbTransactionDb<>(
            pathName,
            columnFamilies,
            true,
            registry,
            partitionId,
            rocksDbConfiguration,
            closeables);
    transactionDb.open();
    return transactionDb;
  }

  @Override
  public RocksdbReadOnlyDb<ColumnFamilyNames> createReadOnlyDb(
      final String pathName, final ColumnFamilies columnFamilies) {
    final RocksdbReadOnlyDb<ColumnFamilyNames> snapshotOnlyDb =
        new RocksdbReadOnlyDb<>(pathName, closeables, columnFamilies);
    snapshotOnlyDb.open();
    return snapshotOnlyDb;
  }
}
