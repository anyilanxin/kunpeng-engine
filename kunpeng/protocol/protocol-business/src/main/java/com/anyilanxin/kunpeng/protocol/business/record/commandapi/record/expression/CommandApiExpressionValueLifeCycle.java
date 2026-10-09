/*
 * Copyright © 2026 anyilanxin zxh (anyilanxin@aliyun.com)
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
package com.anyilanxin.kunpeng.protocol.business.record.commandapi.record.expression;

import static com.anyilanxin.kunpeng.protocol.business.RecordMappingIndex.RECORD_INDEX_85;
import static com.anyilanxin.kunpeng.protocol.business.RecordMappingIndex.RECORD_INDEX_86;
import static com.anyilanxin.kunpeng.protocol.business.RecordProcessIndex.NOT_PROCESS_INDEX;
import static com.anyilanxin.kunpeng.protocol.business.RecordProcessIndex.PROCESS_INDEX_227;

import com.anyilanxin.kunpeng.protocol.business.ValueType;
import com.anyilanxin.kunpeng.protocol.business.record.commandapi.CommandApiValueLifeCycle;

/**
 * 表达式评估 API 生命周期：评估请求 / 评估结果。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public enum CommandApiExpressionValueLifeCycle implements CommandApiValueLifeCycle {
  EVALUATE_REQUEST((short) 0, PROCESS_INDEX_227, RECORD_INDEX_85),
  EVALUATE_RESPONSE((short) 1, NOT_PROCESS_INDEX, RECORD_INDEX_86);

  private final short value;
  private final short processIndex;
  private final short recordIndex;

  CommandApiExpressionValueLifeCycle(
      final short value, final short processIndex, final short recordIndex) {
    this.value = value;
    this.processIndex = processIndex;
    this.recordIndex = recordIndex;
  }

  public short getValueState() {
    return value;
  }

  public static CommandApiValueLifeCycle from(final short value) {
    return switch (value) {
      case 0 -> EVALUATE_REQUEST;
      case 1 -> EVALUATE_RESPONSE;
      default -> throw new IllegalStateException("Unexpected value: " + value);
    };
  }

  @Override
  public short value() {
    return value;
  }

  @Override
  public short processIndex() {
    return processIndex;
  }

  @Override
  public ValueType getValueType() {
    return ValueType.EXPRESSION_API;
  }

  @Override
  public short recordIndex() {
    return recordIndex;
  }
}
