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
