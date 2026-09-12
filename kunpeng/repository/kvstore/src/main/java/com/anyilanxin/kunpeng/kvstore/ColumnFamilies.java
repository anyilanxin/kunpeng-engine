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

/**
 * 列族定义，描述实体列族、虚拟列族的编号、名称等信息
 *
 * @author zxuanhong
 */
public interface ColumnFamilies {
  /** 列族 */
  int entityFamily();

  /** 实体列族名称 */
  byte[] entityFamilyName();

  /** 是否允许数据迁移 */
  boolean enableTransfer();

  /** 虚拟列族 */
  int virtualFamily();

  /** 使用的实体列族 */
  PredefinedColumnFamily[] allFamily();
}
