/*
 * Copyright © 2026 anyilanxin zxh (anyilanxin@aliyun.com)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

  /** 表名前缀，与逻辑名拼接成完整物理名注入 {@code ${table.x}} 变量。 */
  private String tablePrefix = "KP_";

  /** 逻辑表名/索引名的大小写渲染（UPPER/LOWER），与前缀配合保证标识符风格统一；默认大写。 */
  private String tableNameCase = "UPPER";

  /** Liquibase 元数据对象名比较是否区分大小写（透传 {@code AbstractJdbcDatabase#setCaseSensitive}）。 */
  private boolean caseSensitive = false;

  /** 共享连接池的最大连接数。 */
  private int maxPoolSize = DEFAULT_MAX_POOL_SIZE;

  /** 定时刷盘间隔；设为 0 表示每条记录处理完立即刷盘（退化模式，仅供排障）。 */
  private Duration flushInterval = DEFAULT_FLUSH_INTERVAL;

  /** 待写变更数达到该阈值时在处理线程内立即触发一次刷盘（背压上限）。 */
  private int flushThreshold = DEFAULT_FLUSH_THRESHOLD;

  /** 启动时是否自动执行 Liquibase 建表（沿用参考实现的配置键名，老配置可直接迁移）。 */
  private boolean autoDdl = true;

  public void validate() {
    if (url == null || url.isBlank()) {
      throw new IllegalArgumentException("Rdbms sink requires a 'url' argument");
    }
    resolvedDialect();
    if (tablePrefix == null || !tablePrefix.matches("[A-Za-z0-9_]*") || tablePrefix.isBlank()) {
      throw new IllegalArgumentException(
          "tablePrefix must match [A-Za-z0-9_]* and not be blank but was '" + tablePrefix + "'");
    }
    if (tableNameCase != null && !tableNameCase.isBlank()) {
      try {
        TableNameCase.valueOf(tableNameCase.trim().toUpperCase());
      } catch (final IllegalArgumentException e) {
        throw new IllegalArgumentException(
            "tableNameCase must be UPPER or LOWER but was '" + tableNameCase + "'");
      }
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

  /**
   * @return 解析后的表名大小写渲染策略
   */
  public TableNameCase resolvedTableNameCase() {
    if (tableNameCase != null && !tableNameCase.isBlank()) {
      return TableNameCase.valueOf(tableNameCase.trim().toUpperCase());
    }
    return TableNameCase.UPPER;
  }

  /** 表名/索引名的大小写渲染策略 */
  public enum TableNameCase {
    UPPER,
    LOWER
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

  public String getTableNameCase() {
    return tableNameCase;
  }

  public void setTableNameCase(final String tableNameCase) {
    this.tableNameCase = tableNameCase;
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

  public boolean isCaseSensitive() {
    return caseSensitive;
  }

  public void setCaseSensitive(final boolean caseSensitive) {
    this.caseSensitive = caseSensitive;
  }

  public boolean isAutoDdl() {
    return autoDdl;
  }

  public void setAutoDdl(final boolean autoDdl) {
    this.autoDdl = autoDdl;
  }
}
