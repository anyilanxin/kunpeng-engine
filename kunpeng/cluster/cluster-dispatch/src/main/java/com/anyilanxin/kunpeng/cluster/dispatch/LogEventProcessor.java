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
package com.anyilanxin.kunpeng.cluster.dispatch;

import com.anyilanxin.kunpeng.cluster.dispatch.eventlog.LogRecord;
import com.anyilanxin.kunpeng.protocol.admin.AdminValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.admin.AdminValueType;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;

/**
 * @author zxuanhong
 * @since
 */
@SuppressWarnings("rawtypes")
public interface LogEventProcessor<T extends UnifiedRecordValue> {
  default void processRecord(final LogRecord<T> record) {}

  /**
   * 尝试处理过程中发生的错误。
   *
   * @param command 发生错误时正在处理的 command
   * @param error 已发生的错误，processor 应尝试对其处理
   * @return 处理错误的类型。默认为 {@link ProcessingError#UNEXPECTED_ERROR}。
   */
  default ProcessingError tryHandleError(final LogRecord<T> command, final Throwable error) {
    return ProcessingError.UNEXPECTED_ERROR;
  }

  enum ProcessingError {
    EXPECTED_ERROR,
    UNEXPECTED_ERROR
  }

  AdminValueLifeCycle[] valueLifeCycles();

  AdminValueType valueType();
}
