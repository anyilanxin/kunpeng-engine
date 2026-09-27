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

/**
 * 目标数据库方言。
 *
 * <p>方言差异只体现在资源选择上：mapper XML 中按 databaseId 取各方言版本的语句（MyBatis 内建机制， 产品名到 id 的映射见 {@code
 * db/vendor-properties/*.properties}）与建表脚本里的列类型变量。 Java 侧不拼接任何 SQL 文本。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public enum Dialect {
  /** H2 内存/文件库，用于开发与测试。 */
  H2("h2"),
  /** PostgreSQL。 */
  POSTGRESQL("postgresql"),
  /** MySQL 与 MariaDB。 */
  MYSQL("mysql");

  private final String id;

  Dialect(final String id) {
    this.id = id;
  }

  /**
   * @return 方言 id（仅用于日志与配置校验；SQL 的方言选择由 MyBatis databaseId 机制承担）
   */
  public String id() {
    return id;
  }

  /** 按 JDBC url 前缀推断方言。 */
  public static Dialect fromUrl(final String url) {
    if (url.startsWith("jdbc:postgresql:")) {
      return POSTGRESQL;
    }
    if (url.startsWith("jdbc:mysql:") || url.startsWith("jdbc:mariadb:")) {
      return MYSQL;
    }
    if (url.startsWith("jdbc:h2:")) {
      return H2;
    }
    throw new IllegalArgumentException(
        "Unsupported jdbc url for rdbms sink, expected one of [postgresql, mysql, mariadb, h2]"
            + " but got '"
            + url
            + "'");
  }
}
