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

import com.anyilanxin.kunpeng.eventlog.LoggedEntry;
import com.anyilanxin.kunpeng.protocol.admin.AdminValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.admin.AdminValueType;
import com.anyilanxin.kunpeng.protocol.admin.impl.AdminRecordMetadata;
import com.anyilanxin.kunpeng.protocol.admin.record.RecordType;
import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * {@link LogRecord} 的 logstreams 视图（替代旧模块的 LogRecordImpl）： 消费方在处理循环里 {@code
 * entry.readMetadata(metadata); entry.readValue(value);} 后 {@code wrap(entry, metadata, value)}
 * 复用本对象。
 */
public final class TypedRecordReader implements LogRecord {

  private final int partitionId;
  private LoggedEntry entry;
  private AdminRecordMetadata metadata;
  private UnifiedRecordValue value;

  public TypedRecordReader(final int partitionId) {
    this.partitionId = partitionId;
  }

  public void wrap(
      final LoggedEntry entry, final AdminRecordMetadata metadata, final UnifiedRecordValue value) {
    this.entry = entry;
    this.metadata = metadata;
    this.value = value;
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
  public long getPosition() {
    return entry.getPosition();
  }

  @Override
  public long getSourceRecordPosition() {
    return entry.getSourcePosition();
  }

  @Override
  public long getTimestamp() {
    return entry.getTimestamp();
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
  public int getResourceId() {
    return 0;
  }

  @Override
  public AdminValueType getValueType() {
    return metadata.getValueType();
  }

  @Override
  public EventRecord copyOf() {
    return null;
  }

  @Override
  public long getKey() {
    return entry.getKey();
  }

  @Override
  public UnifiedRecordValue getValue() {
    return value;
  }

  @JsonIgnore
  @Override
  public long getRequestId() {
    return metadata.getRequestId();
  }

  @JsonIgnore
  @Override
  public int getLength() {
    return metadata.getLength() + value.getLength();
  }

  @JsonIgnore
  public int getPartitionId() {
    return partitionId;
  }
}
