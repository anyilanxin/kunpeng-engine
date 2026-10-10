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

import com.anyilanxin.kunpeng.sink.rdbms.mapper.TableSpec;
import java.util.List;
import java.util.Objects;

/**
 * 待写入的单行变更，save 与 update 严格区分（对应 mapper 的 insert / update 语句）：
 *
 * <ul>
 *   <li>{@link Kind#INSERT}：全列插入，携带该实体的完整行模型，只在创建事件时产生。
 *   <li>{@link Kind#UPDATE}：按主键更新，行模型里只需填 update 语句涉及的列。
 *   <li>{@link Kind#DELETE}：按某列等值删除（历史清理按 {@code processInstanceId} 删数据时使用）。
 * </ul>
 *
 * <p>主键值兼任缓冲区里的合并键：同一窗口内同主键的多个 update 只保留最后一个；insert 与 update 各占一个键位并保持先后顺序（先 insert 后
 * update，事务内自然合并成最终状态）。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class RowChange {

  /** 变更形态。 */
  public enum Kind {
    INSERT,
    UPDATE,
    DELETE
  }

  private final Kind kind;
  private final TableSpec table;
  private final List<Object> keyValues;
  private final String filterColumn;
  private final Object model;

  private RowChange(
      final Kind kind,
      final TableSpec table,
      final List<Object> keyValues,
      final String filterColumn,
      final Object model) {
    this.kind = kind;
    this.table = table;
    this.keyValues = List.copyOf(keyValues);
    this.filterColumn = filterColumn;
    this.model = model;
  }

  /** 构造一条全列插入。主键值须与 {@link TableSpec#primaryKey()} 顺序一致。 */
  public static RowChange insert(
      final TableSpec table, final List<Object> keyValues, final Object model) {
    return new RowChange(Kind.INSERT, table, keyValues, null, model);
  }

  /** 构造一条按主键更新。 */
  public static RowChange update(
      final TableSpec table, final List<Object> keyValues, final Object model) {
    return new RowChange(Kind.UPDATE, table, keyValues, null, model);
  }

  /** 构造一条按列等值删除。 */
  public static RowChange deleteByColumn(
      final TableSpec table, final String filterColumn, final Object value) {
    return new RowChange(Kind.DELETE, table, List.of(value), filterColumn, null);
  }

  public Kind kind() {
    return kind;
  }

  public TableSpec table() {
    return table;
  }

  List<Object> keyValues() {
    return keyValues;
  }

  String filterColumn() {
    return filterColumn;
  }

  Object model() {
    return model;
  }

  /** 缓冲合并用的稳定键：形态 + 表 + 主键。 */
  String coalesceKey() {
    return kind + ":" + table.name() + ":" + keyValues;
  }

  @Override
  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof final RowChange change)) {
      return false;
    }
    return kind == change.kind
        && table.equals(change.table)
        && keyValues.equals(change.keyValues)
        && Objects.equals(filterColumn, change.filterColumn)
        && Objects.equals(model, change.model);
  }

  @Override
  public int hashCode() {
    return Objects.hash(kind, table, keyValues, filterColumn);
  }
}
