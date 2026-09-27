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
package com.anyilanxin.kunpeng.sink.rdbms.mapper;

import java.util.List;

/**
 * 一张目标表的物理形状：列清单与主键。表名为不带前缀的逻辑名，实际表名在 XML 中以 {@code ${prefix}} 变量拼接。
 *
 * <p>「哪些列随 update 更新」不在这里表达——那是各实体 mapper XML 的 update 语句职责。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public record TableSpec(String name, List<String> columns, List<String> primaryKey) {

  public TableSpec {
    columns = List.copyOf(columns);
    primaryKey = List.copyOf(primaryKey);
  }
}
