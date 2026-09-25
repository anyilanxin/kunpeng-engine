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
package com.anyilanxin.kunpeng.client.store;

import com.anyilanxin.kunpeng.client.ClientLoggers;
import com.anyilanxin.kunpeng.cluster.utils.serializer.Namespace;
import com.anyilanxin.kunpeng.cluster.utils.serializer.Namespaces;
import com.anyilanxin.kunpeng.cluster.utils.serializer.Serializer;
import com.anyilanxin.kunpeng.utils.FileDataStoreUtils;
import com.anyilanxin.kunpeng.utils.FileUtil;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;

/**
 * @author zxuanhong
 * @since 2026.9.0
 */
public class GatewayInfoPersistedStore {

  private static final String FILE_NAME = ".client.meta";
  private static final Map<String, String> map;
  private static final Path configurationFile;
  private static final Logger LOGGER = ClientLoggers.LOGGER;
  public static final Serializer SERIALIZER =
      Serializer.using(
          new Namespace.Builder()
              .register(Namespaces.BASIC)
              .register(Void.class)
              .name("GatewayInfo")
              .build());

  static {
    final Path dataRootDirectory = Path.of("data");
    try {
      FileUtil.ensureDirectory(dataRootDirectory);
    } catch (final IOException e) {
      throw new UncheckedIOException("Failed to create data directory", e);
    }
    configurationFile = dataRootDirectory.resolve(FILE_NAME);
    map = new ConcurrentHashMap<>();
    try {
      readFromFile();
    } catch (final IOException e) {
      throw new UncheckedIOException("Failed to read cluster meta store", e);
    }
  }

  private static void writeToFile() {
    FileDataStoreUtils.writeToFile(configurationFile, SERIALIZER.encode(map));
  }

  private static void readFromFile() throws IOException {
    final Optional<byte[]> bytes = FileDataStoreUtils.readFromFile(configurationFile);
    if (bytes.isEmpty()) {
      writeToFile();
      return;
    }
    final Map<String, String> decoded = SERIALIZER.decode(bytes.get());
    if (decoded != null) {
      map.putAll(decoded);
    }
  }

  private GatewayInfoPersistedStore() {}

  public static Map<String, String> getGatewayInfo() {
    final Map<String, String> gatewayInfo = new HashMap<>(map.size());
    gatewayInfo.putAll(map);
    return gatewayInfo;
  }

  public static void setGatewayInfo(final Map<String, String> gatewayInfo) {
    map.clear();
    map.putAll(gatewayInfo);
    writeToFile();
  }

  public static void setGatewayInfo(final String key, final String value) {
    map.put(key, value);
    writeToFile();
  }

  public static void remove(final String key) {
    map.remove(key);
    writeToFile();
  }

  public static void clear() {
    map.clear();
    writeToFile();
  }
}
