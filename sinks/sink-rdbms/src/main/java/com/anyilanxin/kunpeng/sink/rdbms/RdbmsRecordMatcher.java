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
package com.anyilanxin.kunpeng.sink.rdbms;

import com.anyilanxin.kunpeng.protocol.business.ValueType;
import com.anyilanxin.kunpeng.protocol.business.record.RecordType;
import com.anyilanxin.kunpeng.sink.api.context.RecordMatcher;
import java.util.Map;

/**
 * 只放行「事件 + 本 sink 关心的负载类型」的匹配器。被拒绝的记录在引擎侧直接确认，零投递开销。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
final class RdbmsRecordMatcher implements RecordMatcher {

  private static final Map<ValueType, Boolean> HANDLED =
      Map.ofEntries(
          Map.entry(ValueType.PROCESS_DEFINITION, true),
          Map.entry(ValueType.PROCESS_INSTANCE, true),
          Map.entry(ValueType.ACTIVITY, true),
          Map.entry(ValueType.VARIABLE, true),
          Map.entry(ValueType.USER_TASK, true),
          Map.entry(ValueType.JOB, true),
          Map.entry(ValueType.INCIDENT, true),
          Map.entry(ValueType.TIMER, true),
          Map.entry(ValueType.MESSAGE_SUBSCRIPTION, true),
          Map.entry(ValueType.SIGNAL_SUBSCRIPTION, true),
          Map.entry(ValueType.HISTORY_CLEANUP, true));

  @Override
  public boolean acceptsRecordType(final RecordType recordType) {
    return recordType == RecordType.EVENT;
  }

  @Override
  public boolean acceptsValueType(final ValueType valueType) {
    return HANDLED.getOrDefault(valueType, false);
  }
}
