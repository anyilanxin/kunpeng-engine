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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.rocksdb;

import static com.anyilanxin.kunpeng.kvstore.PredefinedColumnFamily.DEFAULT_COLUMN_FAMILY;
import static com.anyilanxin.kunpeng.kvstore.PredefinedColumnFamily.LOCAL_COLUMN_FAMILY;

import com.anyilanxin.kunpeng.kvstore.ColumnFamilies;
import com.anyilanxin.kunpeng.kvstore.PredefinedColumnFamily;

/** rocksdb 功能测试使用的列族定义：不可迁移的 default 虚拟列族 + 可迁移的 local 虚拟列族 */
public enum TestColumnFamilies implements ColumnFamilies {
  DEFAULT(DEFAULT_COLUMN_FAMILY, 0),
  FIRST(DEFAULT_COLUMN_FAMILY, 1),
  SECOND(DEFAULT_COLUMN_FAMILY, 2),
  TRANSFERABLE(LOCAL_COLUMN_FAMILY, 10),
  ;

  private final PredefinedColumnFamily family;
  private final int virtualFamily;

  TestColumnFamilies(final PredefinedColumnFamily family, final int virtualFamily) {
    this.family = family;
    this.virtualFamily = virtualFamily;
  }

  @Override
  public int entityFamily() {
    return family.getFamily();
  }

  @Override
  public byte[] entityFamilyName() {
    return family.getColumnFamilyName();
  }

  @Override
  public boolean enableTransfer() {
    return family.isEnableTransfer();
  }

  @Override
  public int virtualFamily() {
    return virtualFamily;
  }

  @Override
  public PredefinedColumnFamily[] allFamily() {
    return new PredefinedColumnFamily[] {DEFAULT_COLUMN_FAMILY, LOCAL_COLUMN_FAMILY};
  }
}
