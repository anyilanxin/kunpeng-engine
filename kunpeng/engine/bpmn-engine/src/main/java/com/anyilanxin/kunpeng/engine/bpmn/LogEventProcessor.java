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
package com.anyilanxin.kunpeng.engine.bpmn;

import com.anyilanxin.kunpeng.protocol.business.ValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.ValueType;
import com.anyilanxin.kunpeng.protocol.business.impl.eventlog.BusinessLogRecord;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;

/**
 * 日志事件处理器接口：按 Record 类型分发事件处理。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@SuppressWarnings("rawtypes")
public interface LogEventProcessor<T extends UnifiedRecordValue> {
  default void processRecord(final BusinessLogRecord<T> record) {}

  /**
   * 尝试处理处理过程中发生的错误。
   *
   * @param command 错误发生时正在处理的命令
   * @param error 发生的错误，由处理器尝试处理
   * @return 处理错误类型；默认 {@link ProcessingError#UNEXPECTED_ERROR}
   */
  default ProcessingError tryHandleError(
      final BusinessLogRecord<T> command, final Throwable error) {
    return ProcessingError.UNEXPECTED_ERROR;
  }

  enum ProcessingError {
    EXPECTED_ERROR,
    UNEXPECTED_ERROR
  }

  ValueLifeCycle[] valueLifeCycles();

  ValueType valueType();
}
