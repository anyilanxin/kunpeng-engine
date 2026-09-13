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
package com.anyilanxin.kunpeng.repository.admin;

import static com.anyilanxin.kunpeng.kvstore.PredefinedColumnFamily.DEFAULT_COLUMN_FAMILY;

import com.anyilanxin.kunpeng.kvstore.ColumnFamilies;
import com.anyilanxin.kunpeng.kvstore.PredefinedColumnFamily;

/**
 * 数据列族
 *
 * @author zxuanhong
 */
public enum AdminRepositoryColumnFamilies implements ColumnFamilies {
  DEFAULT(DEFAULT_COLUMN_FAMILY, 0),
  KEY(DEFAULT_COLUMN_FAMILY, 1),
  PROCESS_POSITION(DEFAULT_COLUMN_FAMILY, 2),

  ADMIN_CLUSTER_META(DEFAULT_COLUMN_FAMILY, 20),
  ADMIN_DISPATCH_PLAN(DEFAULT_COLUMN_FAMILY, 21),
  ADMIN_DISPATCH_PLAN_EXECUTION(DEFAULT_COLUMN_FAMILY, 22),

  BUSINESS_CLUSTER_META(DEFAULT_COLUMN_FAMILY, 30),
  BUSINESS_DISPATCH_PLAN(DEFAULT_COLUMN_FAMILY, 31),
  BUSINESS_DISPATCH_PLAN_LAST(DEFAULT_COLUMN_FAMILY, 32),
  BUSINESS_DISPATCH_PLAN_EXECUTION(DEFAULT_COLUMN_FAMILY, 33),
  BUSINESS_DISPATCH_PLAN_EXECUTION_ORDER(DEFAULT_COLUMN_FAMILY, 34),
  BUSINESS_DISPATCH_PLAN_EXECUTION_DUE_DATE(DEFAULT_COLUMN_FAMILY, 35),
  BUSINESS_DELAYED_DISPATCH_PLAN_DUE_DATE(DEFAULT_COLUMN_FAMILY, 36),

  DELAYED(DEFAULT_COLUMN_FAMILY, 40),
  DELAYED_DUE_DATE(DEFAULT_COLUMN_FAMILY, 41),

  NODE_SOURCE_META(DEFAULT_COLUMN_FAMILY, 50),
  NODE_SOURCE(DEFAULT_COLUMN_FAMILY, 51),
  PARTITION_SOURCE_META(DEFAULT_COLUMN_FAMILY, 52),
  PARTITION_SOURCE(DEFAULT_COLUMN_FAMILY, 53),
  ;
  private final PredefinedColumnFamily family;
  private final int virtualFamily;

  AdminRepositoryColumnFamilies(final PredefinedColumnFamily family, final int virtualFamily) {
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
    return new PredefinedColumnFamily[] {DEFAULT_COLUMN_FAMILY};
  }
}
