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
