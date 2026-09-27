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
package com.anyilanxin.kunpeng.sink.rdbms;

import com.anyilanxin.kunpeng.sink.rdbms.mapper.Dialect;
import java.time.Duration;

/**
 * RDBMS sink 的用户配置项（broker 配置中 sink 的 {@code args}）。
 *
 * <p>示例：{@code url = "jdbc:postgresql://localhost:5432/kunpeng"}。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class RdbmsSinkSettings {

  private static final Duration DEFAULT_FLUSH_INTERVAL = Duration.ofMillis(500);
  private static final int DEFAULT_FLUSH_THRESHOLD = 10_000;
  private static final int DEFAULT_MAX_POOL_SIZE = 8;

  /** JDBC 连接地址，必填；方言默认由地址前缀推断。 */
  private String url = "";

  private String userName = "";

  private String password = "";

  /** 显式指定方言（h2/postgresql/mysql）；留空表示按 url 推断。 */
  private String dialect = "";

  /** 表名前缀（mapper XML 与 Liquibase changelog 里的 {@code ${prefix}} / {@code ${tablePrefix}} 变量）。 */
  private String tablePrefix = "kp_";

  /** 共享连接池的最大连接数。 */
  private int maxPoolSize = DEFAULT_MAX_POOL_SIZE;

  /** 定时刷盘间隔；设为 0 表示每条记录处理完立即刷盘（退化模式，仅供排障）。 */
  private Duration flushInterval = DEFAULT_FLUSH_INTERVAL;

  /** 待写变更数达到该阈值时在处理线程内立即触发一次刷盘（背压上限）。 */
  private int flushThreshold = DEFAULT_FLUSH_THRESHOLD;

  /** 启动时是否自动补齐建表脚本。 */
  private boolean migrateSchema = true;

  public void validate() {
    if (url == null || url.isBlank()) {
      throw new IllegalArgumentException("Rdbms sink requires a 'url' argument");
    }
    resolvedDialect();
    if (tablePrefix == null || !tablePrefix.matches("[A-Za-z0-9_]*") || tablePrefix.isBlank()) {
      throw new IllegalArgumentException(
          "tablePrefix must match [A-Za-z0-9_]* and not be blank but was '" + tablePrefix + "'");
    }
    if (maxPoolSize < 1) {
      throw new IllegalArgumentException("maxPoolSize must be >= 1 but was " + maxPoolSize);
    }
    if (flushThreshold < 1) {
      throw new IllegalArgumentException("flushThreshold must be >= 1 but was " + flushThreshold);
    }
  }

  /**
   * @return 解析后的方言；显式配置优先，否则按 url 推断
   */
  public Dialect resolvedDialect() {
    if (dialect != null && !dialect.isBlank()) {
      return Dialect.valueOf(dialect.trim().toUpperCase());
    }
    return Dialect.fromUrl(url);
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(final String url) {
    this.url = url;
  }

  public String getUserName() {
    return userName;
  }

  public void setUserName(final String userName) {
    this.userName = userName;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(final String password) {
    this.password = password;
  }

  public String getDialect() {
    return dialect;
  }

  public void setDialect(final String dialect) {
    this.dialect = dialect;
  }

  public String getTablePrefix() {
    return tablePrefix;
  }

  public void setTablePrefix(final String tablePrefix) {
    this.tablePrefix = tablePrefix;
  }

  public int getMaxPoolSize() {
    return maxPoolSize;
  }

  public void setMaxPoolSize(final int maxPoolSize) {
    this.maxPoolSize = maxPoolSize;
  }

  public Duration getFlushInterval() {
    return flushInterval;
  }

  public void setFlushInterval(final Duration flushInterval) {
    this.flushInterval = flushInterval;
  }

  public int getFlushThreshold() {
    return flushThreshold;
  }

  public void setFlushThreshold(final int flushThreshold) {
    this.flushThreshold = flushThreshold;
  }

  public boolean isMigrateSchema() {
    return migrateSchema;
  }

  public void setMigrateSchema(final boolean migrateSchema) {
    this.migrateSchema = migrateSchema;
  }
}
