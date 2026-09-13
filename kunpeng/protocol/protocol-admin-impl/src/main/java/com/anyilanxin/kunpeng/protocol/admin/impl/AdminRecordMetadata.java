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
package com.anyilanxin.kunpeng.protocol.admin.impl;

import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.bufferAsString;

import com.anyilanxin.kunpeng.protocol.admin.AdminValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.admin.AdminValueType;
import com.anyilanxin.kunpeng.protocol.admin.record.*;
import com.anyilanxin.kunpeng.protocol.common.VersionInfo;
import com.anyilanxin.kunpeng.structpack.buffer.BufferReader;
import com.anyilanxin.kunpeng.structpack.buffer.BufferWriter;
import com.anyilanxin.kunpeng.utils.VersionUtil;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;
import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;

public final class AdminRecordMetadata implements BufferWriter, BufferReader {
  public static final int BLOCK_LENGTH =
      MessageHeaderEncoder.ENCODED_LENGTH + AdminRecordMetadataEncoder.BLOCK_LENGTH;
  public static final int DEFAULT_RECORD_VERSION = 1;

  public static final VersionInfo CURRENT_BROKER_VERSION =
      VersionInfo.parse(VersionUtil.getVersion());

  private final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
  private final MessageHeaderDecoder headerDecoder = new MessageHeaderDecoder();
  private final AdminRecordMetadataEncoder encoder = new AdminRecordMetadataEncoder();
  private final AdminRecordMetadataDecoder decoder = new AdminRecordMetadataDecoder();
  private RecordType recordType = RecordType.NULL_VAL;
  private AdminValueType valueType = AdminValueType.UNKNOW;
  private AdminValueLifeCycle valueLifeCycle = AdminValueLifeCycle.UnknownState.UNKNOWN;
  private long requestId;
  private final UnsafeBuffer rejectionType = new UnsafeBuffer(0, 0);
  private final UnsafeBuffer rejectionReason = new UnsafeBuffer(0, 0);

  // 默认始终为当前版本
  private int protocolVersion = 6;
  private VersionInfo brokerVersion = CURRENT_BROKER_VERSION;
  private int recordVersion = DEFAULT_RECORD_VERSION;
  private long operationReference;

  public AdminRecordMetadata() {
    reset();
  }

  @Override
  public void wrap(final DirectBuffer buffer, int offset, final int length) {
    reset();
    headerDecoder.wrap(buffer, offset);
    offset += headerDecoder.encodedLength();
    decoder.wrap(buffer, offset, headerDecoder.blockLength(), headerDecoder.version());
    // 处理定长字段
    requestId = decoder.requestId();
    protocolVersion = decoder.protocolVersion();
    recordType = decoder.recordType();
    valueType = AdminValueType.valueOf(decoder.valueType());
    valueLifeCycle = AdminValueLifeCycle.fromProtocolValue(valueType, decoder.valueLifeCycle());
    operationReference = decoder.operationReference();
    brokerVersion =
        Optional.ofNullable(decoder.brokerVersion())
            .map(
                versionDecoder ->
                    new VersionInfo(
                        versionDecoder.majorVersion(),
                        versionDecoder.minorVersion(),
                        versionDecoder.patchVersion()))
            .orElse(VersionInfo.UNKNOWN);

    final int decodedRecordVersion = decoder.recordVersion();
    if (decodedRecordVersion == 0
        || decodedRecordVersion == AdminRecordMetadataDecoder.recordVersionNullValue()) {
      recordVersion = DEFAULT_RECORD_VERSION;
    } else {
      recordVersion = decodedRecordVersion;
    }

    // 处理变长字段
    final int rejectionTypeLength = decoder.rejectionTypeLength();
    if (rejectionTypeLength > 0) {
      decoder.wrapRejectionType(rejectionType);
    } else {
      decoder.skipRejectionType();
    }

    final int rejectionReasonLength = decoder.rejectionReasonLength();
    if (rejectionReasonLength > 0) {
      decoder.wrapRejectionReason(rejectionReason);
    } else {
      decoder.skipRejectionReason();
    }
  }

  @Override
  public int getLength() {
    return BLOCK_LENGTH
        + AdminRecordMetadataEncoder.rejectionTypeHeaderLength()
        + rejectionType.capacity()
        + AdminRecordMetadataEncoder.rejectionReasonHeaderLength()
        + rejectionReason.capacity();
  }

  @Override
  public void write(final MutableDirectBuffer buffer, int offset) {
    headerEncoder.wrap(buffer, offset);

    headerEncoder
        .blockLength(encoder.sbeBlockLength())
        .templateId(encoder.sbeTemplateId())
        .schemaId(encoder.sbeSchemaId())
        .version(encoder.sbeSchemaVersion());

    offset += headerEncoder.encodedLength();
    encoder.wrap(buffer, offset);

    // 处理定长字段
    encoder
        .requestId(requestId)
        .protocolVersion(protocolVersion)
        .recordType(recordType)
        .valueType(valueType.getValue())
        .valueLifeCycle(valueLifeCycle.value())
        .recordVersion(recordVersion)
        .operationReference(operationReference);

    encoder
        .brokerVersion()
        .majorVersion(brokerVersion.getMajorVersion())
        .minorVersion(brokerVersion.getMinorVersion())
        .patchVersion(brokerVersion.getPatchVersion());

    // 处理变长字段
    encoder.putRejectionType(rejectionType, 0, rejectionType.capacity());
    encoder.putRejectionReason(rejectionReason, 0, rejectionReason.capacity());
  }

  public long getRequestId() {
    return requestId;
  }

  public AdminRecordMetadata requestId(final long requestId) {
    this.requestId = requestId;
    return this;
  }

  public AdminRecordMetadata protocolVersion(final int protocolVersion) {
    this.protocolVersion = protocolVersion;
    return this;
  }

  public int getProtocolVersion() {
    return protocolVersion;
  }

  public RecordType getRecordType() {
    return recordType;
  }

  public AdminRecordMetadata recordType(final RecordType recordType) {
    this.recordType = recordType;
    return this;
  }

  public AdminValueType getValueType() {
    return valueType;
  }

  public AdminRecordMetadata valueType(final AdminValueType valueType) {
    this.valueType = valueType;
    return this;
  }

  public AdminRecordMetadata valueLifeCycle(final AdminValueLifeCycle valueLifeCycle) {
    this.valueLifeCycle = valueLifeCycle;
    return this;
  }

  public AdminValueLifeCycle getLifeCycle() {
    return valueLifeCycle;
  }

  public AdminRecordMetadata rejectionType(final String rejectionType) {
    final byte[] bytes = rejectionType.getBytes(StandardCharsets.UTF_8);
    this.rejectionType.wrap(bytes);
    return this;
  }

  public AdminRecordMetadata rejectionType(final DirectBuffer buffer) {
    rejectionType.wrap(buffer);
    return this;
  }

  public String getRejectionType() {
    return bufferAsString(rejectionType);
  }

  public AdminRecordMetadata rejectionReason(final String rejectionReason) {
    final byte[] bytes = rejectionReason.getBytes(StandardCharsets.UTF_8);
    this.rejectionReason.wrap(bytes);
    return this;
  }

  public AdminRecordMetadata rejectionReason(final DirectBuffer buffer) {
    rejectionReason.wrap(buffer);
    return this;
  }

  public String getRejectionReason() {
    return bufferAsString(rejectionReason);
  }

  public AdminRecordMetadata brokerVersion(final VersionInfo brokerVersion) {
    this.brokerVersion = brokerVersion;
    return this;
  }

  public VersionInfo getBrokerVersion() {
    return brokerVersion;
  }

  public AdminRecordMetadata recordVersion(final int recordVersion) {
    this.recordVersion = recordVersion;
    return this;
  }

  public int getRecordVersion() {
    return recordVersion;
  }

  public AdminRecordMetadata operationReference(final long operationReference) {
    this.operationReference = operationReference;
    return this;
  }

  public long getOperationReference() {
    return operationReference;
  }

  public AdminRecordMetadata reset() {
    requestId = AdminRecordMetadataEncoder.requestIdNullValue();
    protocolVersion = 6;
    recordType = RecordType.NULL_VAL;
    valueType = AdminValueType.UNKNOW;
    valueLifeCycle = AdminValueLifeCycle.UnknownState.UNKNOWN;
    rejectionType.wrap(0, 0);
    rejectionReason.wrap(0, 0);
    brokerVersion = CURRENT_BROKER_VERSION;
    recordVersion = DEFAULT_RECORD_VERSION;
    operationReference = AdminRecordMetadataEncoder.operationReferenceNullValue();
    return this;
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        requestId,
        recordType,
        valueType,
        valueLifeCycle,
        rejectionType,
        rejectionReason,
        protocolVersion,
        brokerVersion,
        recordVersion,
        operationReference);
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final AdminRecordMetadata that = (AdminRecordMetadata) o;
    return requestId == that.requestId
        && valueLifeCycle == that.valueLifeCycle
        && protocolVersion == that.protocolVersion
        && recordType == that.recordType
        && valueType == that.valueType
        && rejectionType.equals(that.rejectionType)
        && rejectionReason.equals(that.rejectionReason)
        && brokerVersion.equals(that.brokerVersion)
        && recordVersion == that.recordVersion
        && operationReference == that.operationReference;
  }

  @Override
  public String toString() {
    // toString 有意精简，只保留调试所需的关键属性（主要用于测试）。
    // 若记录已写入日志（生产环境），我们有其他方式使其重新可读。
    final var builder =
        new StringBuilder(
            "AdminRecordMetadata{"
                + "recordType="
                + recordType
                + ", valueType="
                + valueType
                + ", valueLifeCycle="
                + valueLifeCycle);
    if (rejectionType.capacity() > 0) {
      builder.append(", rejectionType=").append(getRejectionType());
    }
    if (rejectionReason.capacity() > 0) {
      builder.append(", rejectionReason=").append(getRejectionReason());
    }
    if (operationReference != AdminRecordMetadataEncoder.operationReferenceNullValue()) {
      builder.append(", operationReference=").append(operationReference);
    }

    builder.append('}');
    return builder.toString();
  }

  public AdminRecordMetadata copy() {
    final AdminRecordMetadata metadata = new AdminRecordMetadata();
    metadata.requestId = requestId;
    metadata.protocolVersion = protocolVersion;
    metadata.recordType = recordType;
    metadata.valueType = valueType;
    metadata.valueLifeCycle = valueLifeCycle;
    metadata.rejectionType(rejectionType);
    metadata.recordVersion = recordVersion;
    metadata.operationReference = operationReference;
    metadata.rejectionReason(rejectionReason);
    metadata.brokerVersion = brokerVersion;
    return metadata;
  }
}
