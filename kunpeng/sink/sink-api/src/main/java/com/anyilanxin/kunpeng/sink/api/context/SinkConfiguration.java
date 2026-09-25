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
package com.anyilanxin.kunpeng.sink.api.context;

import java.util.Map;

/** Sink 在 broker 配置中的身份与原始参数。 */
public interface SinkConfiguration {

  /**
   * @return 本 Sink 在集群内的唯一 id
   */
  String getId();

  /**
   * @return 原样保管的配置参数；永不为 {@code null}
   */
  Map<String, Object> getArguments();

  /**
   * 将原始参数绑定到 Sink 自选的配置类上。
   *
   * <p>绑定是宽松的：属性名与枚举值大小写不敏感，接受单引号，忽略未知属性；数字键的 Map 会转换成 List，因此 {@code {"0": "a", "1": "b"}} 可以绑定到
   * {@code List<String>} 字段。
   *
   * @param settingsClass 待实例化并填充的类
   * @return 填充完成的实例；未配置参数时返回全新实例
   * @throws IllegalArgumentException 当参数无法绑定到给定类时抛出
   */
  <T> T createSettings(Class<T> settingsClass);
}
