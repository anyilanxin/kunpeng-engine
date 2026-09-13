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
package com.anyilanxin.kunpeng.cluster.dispatch.eventlog;

import com.anyilanxin.kunpeng.protocol.admin.impl.AdminRecordMetadata;
import com.anyilanxin.kunpeng.protocol.admin.record.AdminRecordMetadataEncoder;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;

public interface LogRecord<T extends UnifiedRecordValue> extends EventRecord<T> {

  @Override
  long getKey();

  long getOperationReferenceKey();

  @Override
  T getValue();

  AdminRecordMetadata getMetadata();

  long getRequestId();

  int getLength();

  default boolean hasRequestMetadata() {
    return getRequestId() != AdminRecordMetadataEncoder.requestIdNullValue();
  }
}
