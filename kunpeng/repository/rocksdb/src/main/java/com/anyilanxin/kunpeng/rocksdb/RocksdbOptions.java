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

import com.anyilanxin.kunpeng.kvstore.PredefinedColumnFamily;
import java.util.Map;
import org.rocksdb.ColumnFamilyOptions;
import org.rocksdb.DBOptions;
import org.rocksdb.ReadOptions;
import org.rocksdb.WriteOptions;

/**
 * RocksDB 的数据库选项与 column family 选项是相互独立的，可根据自身配置进行调整。由于各选项 对象都需要单独关闭，该 record 便于在本模块内统一传递这些配置。
 *
 * <p>虽然 RocksDB 中每个 column family 都可以单独配置，但本模块只按实体列族类别使用统一的列族 配置，无需再进一步细分。
 *
 * @param dbOptions 打开 RocksDB 数据库所用的数据库选项
 * @param cfOptions 打开 RocksDB 数据库所用的 column family 选项，按 {@link PredefinedColumnFamily#getFamily()}
 *     为键，仅包含上层声明使用的实体列族（default 恒有）
 */
public record RocksdbOptions(
    DBOptions dbOptions,
    Map<Integer, ColumnFamilyOptions> cfOptions,
    WriteOptions writeOptions,
    ReadOptions prefixReadOptions,
    ReadOptions defaultReadOptions) {}
