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
package com.anyilanxin.kunpeng.kvstore;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.util.Arrays;

/**
 * 预定义根列族：所有数据库实例共有的顶层列族划分，由具体存储实现映射为物理列族
 *
 * @author zxuanhong
 */
public enum PredefinedColumnFamily {
  /** 默认列族，数据不可迁移 */
  DEFAULT_COLUMN_FAMILY(0, false, "default".getBytes(UTF_8)),
  /** 本地列族，集群变更需要迁移数据 */
  LOCAL_COLUMN_FAMILY(1, true, "local".getBytes(UTF_8)),
  /** 全局列族，引导新集群需要加载 */
  GLOBAL_COLUMN_FAMILY(2, true, "global".getBytes(UTF_8)),
  ;

  /** 列族 */
  private final int family;

  /** 是否允许数据迁移 */
  private final boolean enableTransfer;

  /** 列族名称 */
  private final byte[] columnFamilyName;

  PredefinedColumnFamily(
      final int family, final boolean enableTransfer, final byte[] columnFamilyName) {
    this.columnFamilyName = columnFamilyName;
    this.enableTransfer = enableTransfer;
    this.family = family;
  }

  public byte[] getColumnFamilyName() {
    return columnFamilyName;
  }

  public int getFamily() {
    return family;
  }

  public boolean isEnableTransfer() {
    return enableTransfer;
  }

  /** 根据列族名称字节数组查找对应的列族枚举，未找到时抛出 {@link IllegalArgumentException}。 */
  public static PredefinedColumnFamily fromColumnFamilyName(final byte[] name) {
    final PredefinedColumnFamily[] values = PredefinedColumnFamily.values();
    for (final PredefinedColumnFamily predefinedColumnFamily : values) {
      if (Arrays.equals(name, predefinedColumnFamily.getColumnFamilyName())) {
        return predefinedColumnFamily;
      }
    }
    throw new IllegalArgumentException("unknow column family name");
  }
}
