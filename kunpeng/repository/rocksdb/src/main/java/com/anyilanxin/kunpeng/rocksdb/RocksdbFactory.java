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
import com.anyilanxin.kunpeng.kvstore.KvStore;
import io.micrometer.core.instrument.MeterRegistry;

/**
 * RocksDB 数据库工厂。{@link ColumnFamilyNames} 必须是枚举，用于定义 RocksDB 数据库的各个 column family
 *
 * @param <ColumnFamilyNames> column family 名称集合
 */
public sealed interface RocksdbFactory<ColumnFamilyNames extends ColumnFamilies>
    permits DefaultRocksdbFactory {

  /**
   * 在指定目录创建 RocksDB 数据库
   *
   * @param pathName 数据库创建路径
   * @return 创建的 RocksDB 数据库
   */
  KvStore<ColumnFamilyNames> createDb(
      final String pathName,
      final MeterRegistry registry,
      final int partitionId,
      final RocksdbConfiguration rocksDbConfiguration,
      final ColumnFamilies columnFamilies);

  /**
   * 以只读模式打开已存在的数据库，仅用于从中创建 snapshot
   *
   * <p>注意：如果将来需要支持真正可读取的只读数据库，可在此扩展。但要注意此类数据库无法使用 事务，更合适的做法是等脱离 transaction DB 列族之后再支持。
   *
   * @param pathName 已存在数据库的路径
   * @return 可创建 snapshot 的数据库
   */
  KvStore<ColumnFamilyNames> createReadOnlyDb(
      final String pathName, final ColumnFamilies columnFamilies);
}
