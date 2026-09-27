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
