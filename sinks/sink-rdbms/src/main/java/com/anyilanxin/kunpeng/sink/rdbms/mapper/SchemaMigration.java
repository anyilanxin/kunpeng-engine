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

import com.anyilanxin.kunpeng.sink.rdbms.RdbmsSinkSettings;
import java.sql.Connection;
import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.database.AbstractJdbcDatabase;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;

/**
 * 建表迁移：changelog 见 {@code db/changelog/rdbms-sink/changelog-master.xml}（原生 createTable/createIndex
 * changeset，跨方言类型用 dbms 属性的 property 变量映射；演进时按版本追加 changesets/&lt;版本&gt;.xml）。
 *
 * <p>版本登记、执行锁都由 Liquibase 的 DATABASECHANGELOG / DATABASECHANGELOGLOCK 表承担—— 多个分区同时拉起 sink
 * 实例时天然串行化，不需要自己做协调。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
final class SchemaMigration {

  private static final String CHANGELOG = "db/changelog/rdbms-sink/changelog-master.xml";

  private SchemaMigration() {}

  static void run(final Connection connection, final RdbmsSinkSettings settings) {
    try {
      // findCorrectDatabaseImplementation 对 JDBC 连接只会返回 AbstractJdbcDatabase 实现
      final AbstractJdbcDatabase database =
          (AbstractJdbcDatabase)
              DatabaseFactory.getInstance()
                  .findCorrectDatabaseImplementation(new JdbcConnection(connection));
      // 元数据对象名比较是否区分大小写，透传给 Liquibase（与参考实现行为一致）
      database.setCaseSensitive(settings.isCaseSensitive());
      // Liquibase 簿记表同样走前缀 + 大小写策略，与业务表命名一致（也避免与同库其他 Liquibase 使用者冲突）
      database.setDatabaseChangeLogTableName(
          TableName.DATABASECHANGELOG.physicalName(
              settings.getTablePrefix(), settings.resolvedTableNameCase()));
      database.setDatabaseChangeLogLockTableName(
          TableName.DATABASECHANGELOGLOCK.physicalName(
              settings.getTablePrefix(), settings.resolvedTableNameCase()));
      try (final var liquibase =
          new Liquibase(CHANGELOG, new ClassLoaderResourceAccessor(), database)) {
        // changelog 里的标识符统一写 ${table.x}：完整物理名（前缀 + 大小写渲染）运行期注入
        TableName.registerAll(
            liquibase.getChangeLogParameters()::set,
            settings.getTablePrefix(),
            settings.resolvedTableNameCase());
        liquibase.update(new Contexts());
      }
    } catch (final Exception e) {
      throw new IllegalStateException("Failed to apply rdbms sink schema", e);
    }
  }
}
