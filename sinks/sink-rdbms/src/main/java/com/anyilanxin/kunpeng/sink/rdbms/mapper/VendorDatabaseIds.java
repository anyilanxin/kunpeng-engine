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

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.apache.ibatis.io.Resources;

/**
 * 加载 {@code db/vendor-properties/<产品名小写>.properties}，组装成 MyBatis {@link
 * org.apache.ibatis.mapping.VendorDatabaseIdProvider} 需要的「产品名 -> databaseId」映射。
 *
 * <p>每个文件两个字段：{@code productName}（JDBC 元数据里的产品名）、{@code databaseId}（mapper XML 中 databaseId 属性用的
 * id）。新增数据库支持 = 新增一个 properties 文件 + 在 {@link #PRODUCTS} 里登记 + mapper XML 补对应变体。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
final class VendorDatabaseIds {

  private static final String RESOURCE_DIR = "db/vendor-properties/";

  /** 支持的数据库产品名（与文件名的小写形式对应）。 */
  private static final java.util.List<String> PRODUCTS =
      java.util.List.of("h2", "postgresql", "mysql", "mariadb");

  private VendorDatabaseIds() {}

  /**
   * @return 产品名 -> databaseId 的映射，供 VendorDatabaseIdProvider 使用
   */
  static Properties load() {
    final var mapping = new Properties();
    for (final var product : PRODUCTS) {
      final var resource = RESOURCE_DIR + product + ".properties";
      final var props = read(resource);
      final var productName = props.getProperty("productName");
      final var databaseId = props.getProperty("databaseId");
      if (productName == null || databaseId == null) {
        throw new IllegalStateException(
            "Vendor properties %s must define productName and databaseId".formatted(resource));
      }
      mapping.setProperty(productName, databaseId);
    }
    return mapping;
  }

  private static Properties read(final String resource) {
    final Properties props = new Properties();
    try (final InputStream input = Resources.getResourceAsStream(resource)) {
      props.load(input);
    } catch (final IOException e) {
      throw new IllegalStateException("Failed to read vendor properties " + resource, e);
    }
    return props;
  }
}
