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
package com.anyilanxin.kunpeng.sink.rdbms.write;

import tools.jackson.databind.ObjectMapper;

/**
 * 把记录里的集合/映射类字段（候选人、变量值等）序列化成 JSON 文本列。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class JsonValues {

  private static final ObjectMapper JSON = new ObjectMapper();

  private JsonValues() {}

  /**
   * @return 序列化结果；入参为 {@code null} 时返回 {@code null}，失败时抛出非法状态异常
   */
  public static String toJson(final Object value) {
    if (value == null) {
      return null;
    }
    try {
      return JSON.writeValueAsString(value);
    } catch (final Exception e) {
      throw new IllegalStateException("Failed to serialize column value to json", e);
    }
  }
}
