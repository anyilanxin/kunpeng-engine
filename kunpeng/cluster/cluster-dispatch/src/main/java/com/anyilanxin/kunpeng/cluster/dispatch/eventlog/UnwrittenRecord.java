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

import com.anyilanxin.kunpeng.protocol.admin.AdminValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.admin.AdminValueType;
import com.anyilanxin.kunpeng.protocol.admin.impl.AdminRecordMetadata;
import com.anyilanxin.kunpeng.protocol.admin.record.RecordType;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.fasterxml.jackson.annotation.JsonIgnore;

/** 尚未写入日志的 record 视图（处理过程中构造的响应/后续记录）；position 族访问即抛 */
public class UnwrittenRecord implements LogRecord {
  private final long key;
  private final int partitionId;
  private final UnifiedRecordValue value;
  private final AdminRecordMetadata metadata;

  public UnwrittenRecord(
      final long key,
      final int partitionId,
      final UnifiedRecordValue value,
      final AdminRecordMetadata metadata) {
    this.key = key;
    this.partitionId = partitionId;
    this.value = value;
    this.metadata = metadata;
  }

  @Override
  public long getKey() {
    return key;
  }

  @Override
  public long getOperationReferenceKey() {
    return metadata.getOperationReference();
  }

  @JsonIgnore
  @Override
  public AdminRecordMetadata getMetadata() {
    return metadata;
  }

  @Override
  public UnifiedRecordValue getValue() {
    return value;
  }

  public int getPartitionId() {
    return partitionId;
  }

  @Override
  public long getPosition() {
    throw new UnsupportedOperationException("未写入记录无 position");
  }

  @Override
  public long getSourceRecordPosition() {
    throw new UnsupportedOperationException("未写入记录无 position");
  }

  @Override
  public long getTimestamp() {
    throw new UnsupportedOperationException("未写入记录无时间戳");
  }

  @Override
  public RecordType getRecordType() {
    return metadata.getRecordType();
  }

  @Override
  public AdminValueLifeCycle getValueState() {
    return metadata.getLifeCycle();
  }

  @Override
  public int getResourceId() {
    return 0;
  }

  @Override
  public String getRejectionType() {
    return metadata.getRejectionType();
  }

  @Override
  public String getRejectionReason() {
    return metadata.getRejectionReason();
  }

  @Override
  public String getBrokerVersion() {
    return metadata.getBrokerVersion().toString();
  }

  @Override
  public int getRecordVersion() {
    return metadata.getRecordVersion();
  }

  @Override
  public AdminValueType getValueType() {
    return metadata.getValueType();
  }

  @Override
  public long getRequestId() {
    return metadata.getRequestId();
  }

  @Override
  public int getLength() {
    return metadata.getLength() + value.getLength();
  }

  @Override
  public EventRecord copyOf() {
    return null;
  }
}
