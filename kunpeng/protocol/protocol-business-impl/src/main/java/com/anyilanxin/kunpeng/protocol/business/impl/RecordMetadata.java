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
package com.anyilanxin.kunpeng.protocol.business.impl;

import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.bufferAsString;

import com.anyilanxin.kunpeng.protocol.business.ValueLifeCycle;
import com.anyilanxin.kunpeng.protocol.business.ValueType;
import com.anyilanxin.kunpeng.protocol.business.record.*;
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

/**
 * 业务协议 Record 元数据：请求头的编解码。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class RecordMetadata implements BufferWriter, BufferReader {
  public static final int BLOCK_LENGTH =
      MessageHeaderEncoder.ENCODED_LENGTH + RecordMetadataEncoder.BLOCK_LENGTH;
  public static final int DEFAULT_RECORD_VERSION = 1;

  public static final VersionInfo CURRENT_BROKER_VERSION =
      VersionInfo.parse(VersionUtil.getVersion());

  private final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
  private final MessageHeaderDecoder headerDecoder = new MessageHeaderDecoder();
  private final RecordMetadataEncoder encoder = new RecordMetadataEncoder();
  private final RecordMetadataDecoder decoder = new RecordMetadataDecoder();

  private RecordType recordType = RecordType.NULL_VAL;
  private ValueType valueType = ValueType.UNKNOW;
  private ValueLifeCycle valueState = ValueLifeCycle.UnknownState.UNKNOWN;
  private ValueLifeCycle valueFollowState = ValueLifeCycle.UnknownState.UNKNOWN;
  private long requestId;
  private int requestStreamId;
  //  private final AuthInfo authorization = new AuthInfo();
  private final UnsafeBuffer rejectionType = new UnsafeBuffer(0, 0);
  private final UnsafeBuffer rejectionReason = new UnsafeBuffer(0, 0);

  // always the current version by default
  private int protocolVersion = 6;
  private VersionInfo brokerVersion = CURRENT_BROKER_VERSION;
  private int recordVersion = DEFAULT_RECORD_VERSION;
  private long operationReference;
  private long batchOperationReference;

  public RecordMetadata() {
    reset();
  }

  @Override
  public void wrap(final DirectBuffer buffer, int offset, final int length) {
    reset();

    headerDecoder.wrap(buffer, offset);

    offset += headerDecoder.encodedLength();

    decoder.wrap(buffer, offset, headerDecoder.blockLength(), headerDecoder.version());

    // working with fixed-length fields
    recordType = decoder.recordType();
    requestStreamId = decoder.requestStreamId();
    requestId = decoder.requestId();
    protocolVersion = decoder.protocolVersion();
    valueType = ValueType.valueOf(decoder.valueType());
    valueState = ValueLifeCycle.fromProtocolValue(valueType, decoder.valueState());
    operationReference = decoder.operationReference();
    batchOperationReference = decoder.batchOperationReference();

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
        || decodedRecordVersion == RecordMetadataDecoder.recordVersionNullValue()) {
      recordVersion = DEFAULT_RECORD_VERSION;
    } else {
      recordVersion = decodedRecordVersion;
    }

    // working with variable-length fields
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

    //    final int authorizationLength = decoder.authorizationLength();
    //    if (authorizationLength > 0) {
    //      final DirectBuffer authBuffer = new UnsafeBuffer();
    //      decoder.wrapAuthorization(authBuffer);
    ////      authorization.wrap(authBuffer);
    //    } else {
    //      decoder.skipAuthorization();
    //    }
  }

  @Override
  public int getLength() {
    return BLOCK_LENGTH
        + RecordMetadataEncoder.rejectionTypeHeaderLength()
        + rejectionType.capacity()
        + RecordMetadataEncoder.rejectionReasonHeaderLength()
        + rejectionReason.capacity();
    //      + RecordMetadataEncoder.authoriz;ationHeaderLength()
    //      + authorization.getLength();
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

    // working with fixed-length fields
    encoder
        .recordType(recordType)
        .requestStreamId(requestStreamId)
        .requestId(requestId)
        .protocolVersion(protocolVersion)
        .valueType(valueType.getValue())
        .valueState(valueState.value())
        .valueFollowState(valueFollowState.value())
        .recordVersion(recordVersion)
        .operationReference(operationReference)
        .batchOperationReference(batchOperationReference);

    encoder
        .brokerVersion()
        .majorVersion(brokerVersion.getMajorVersion())
        .minorVersion(brokerVersion.getMinorVersion())
        .patchVersion(brokerVersion.getPatchVersion());

    // working with variable-length fields
    encoder.putRejectionType(rejectionType, 0, rejectionType.capacity());
    encoder.putRejectionReason(rejectionReason, 0, rejectionReason.capacity());
    //    encoder.putAuthorization(authorization.toDirectBuffer(), 0, authorization.getLength());
  }

  public long getRequestId() {
    return requestId;
  }

  public RecordMetadata requestId(final long requestId) {
    this.requestId = requestId;
    return this;
  }

  public int getRequestStreamId() {
    return requestStreamId;
  }

  public RecordMetadata requestStreamId(final int requestStreamId) {
    this.requestStreamId = requestStreamId;
    return this;
  }

  public RecordMetadata protocolVersion(final int protocolVersion) {
    this.protocolVersion = protocolVersion;
    return this;
  }

  public int getProtocolVersion() {
    return protocolVersion;
  }

  public ValueType getValueType() {
    return valueType;
  }

  public RecordMetadata valueType(final ValueType eventType) {
    valueType = eventType;
    return this;
  }

  public RecordMetadata valueLifeCycle(final ValueLifeCycle valueState) {
    this.valueState = valueState;
    return this;
  }

  public ValueLifeCycle getLifeCycle() {
    return valueState;
  }

  public RecordMetadata valueFollowState(final ValueLifeCycle valueFollowState) {
    this.valueFollowState = valueFollowState;
    return this;
  }

  public ValueLifeCycle getValueFollowState() {
    return valueFollowState;
  }

  public RecordMetadata recordType(final RecordType recordType) {
    this.recordType = recordType;
    return this;
  }

  public RecordType getRecordType() {
    return recordType;
  }

  public RecordMetadata rejectionType(final RejectionType rejectionType) {
    if (rejectionType == null || rejectionType == RejectionType.NULL_VAL) {
      this.rejectionType.wrap(0, 0);
    } else {
      this.rejectionType.wrap(rejectionType.name().getBytes(StandardCharsets.UTF_8));
    }
    return this;
  }

  public RecordMetadata rejectionType(final String rejectionType) {
    if (rejectionType == null || rejectionType.isEmpty()) {
      this.rejectionType.wrap(0, 0);
    } else {
      this.rejectionType.wrap(rejectionType.getBytes(StandardCharsets.UTF_8));
    }
    return this;
  }

  public RecordMetadata rejectionType(final DirectBuffer buffer) {
    rejectionType.wrap(buffer);
    return this;
  }

  public RejectionType getRejectionType() {
    return rejectionType.capacity() == 0
        ? RejectionType.NULL_VAL
        : RejectionType.valueOf(bufferAsString(rejectionType));
  }

  public RecordMetadata rejectionReason(final String rejectionReason) {
    final byte[] bytes = rejectionReason.getBytes(StandardCharsets.UTF_8);
    this.rejectionReason.wrap(bytes);
    return this;
  }

  public RecordMetadata rejectionReason(final DirectBuffer buffer) {
    rejectionReason.wrap(buffer);
    return this;
  }

  public String getRejectionReason() {
    return bufferAsString(rejectionReason);
  }

  //  public RecordMetadata authorization(final AuthInfo authorization) {
  //    this.authorization.copyFrom(authorization);
  //    return this;
  //  }
  //
  //  public RecordMetadata authorization(final DirectBuffer buffer) {
  //    authorization.wrap(buffer);
  //    return this;
  //  }
  //
  //  public AuthInfo getAuthorization() {
  //    return authorization;
  //  }

  public RecordMetadata brokerVersion(final VersionInfo brokerVersion) {
    this.brokerVersion = brokerVersion;
    return this;
  }

  public VersionInfo getBrokerVersion() {
    return brokerVersion;
  }

  public RecordMetadata recordVersion(final int recordVersion) {
    this.recordVersion = recordVersion;
    return this;
  }

  public int getRecordVersion() {
    return recordVersion;
  }

  public RecordMetadata operationReference(final long operationReference) {
    this.operationReference = operationReference;
    return this;
  }

  public long getOperationReference() {
    return operationReference;
  }

  public RecordMetadata batchOperationReference(final long batchOperationReference) {
    this.batchOperationReference = batchOperationReference;
    return this;
  }

  public long getBatchOperationReference() {
    return batchOperationReference;
  }

  public RecordMetadata reset() {
    recordType = RecordType.NULL_VAL;
    requestId = RecordMetadataEncoder.requestIdNullValue();
    requestStreamId = RecordMetadataEncoder.requestStreamIdNullValue();
    protocolVersion = 6;
    valueType = ValueType.UNKNOW;
    valueState = ValueLifeCycle.UnknownState.UNKNOWN;
    valueFollowState = ValueLifeCycle.UnknownState.UNKNOWN;
    rejectionType.wrap(0, 0);
    rejectionReason.wrap(0, 0);
    //    authorization.reset();
    brokerVersion = CURRENT_BROKER_VERSION;
    recordVersion = DEFAULT_RECORD_VERSION;
    operationReference = RecordMetadataEncoder.operationReferenceNullValue();
    batchOperationReference = RecordMetadataEncoder.batchOperationReferenceNullValue();
    return this;
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        requestId,
        valueType,
        recordType,
        valueState,
        valueFollowState,
        requestStreamId,
        rejectionType,
        rejectionReason,
        protocolVersion,
        brokerVersion,
        recordVersion,
        operationReference,
        batchOperationReference);
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final RecordMetadata that = (RecordMetadata) o;
    return requestId == that.requestId
        && valueState == that.valueState
        && valueFollowState == that.valueFollowState
        && requestStreamId == that.requestStreamId
        && protocolVersion == that.protocolVersion
        && valueType == that.valueType
        && recordType == that.recordType
        && rejectionType.equals(that.rejectionType)
        && rejectionReason.equals(that.rejectionReason)
        //      && authorization.equals(that.authorization)
        && brokerVersion.equals(that.brokerVersion)
        && recordVersion == that.recordVersion
        && operationReference == that.operationReference
        && batchOperationReference == that.batchOperationReference;
  }

  @Override
  public String toString() {
    // The toString is intentionally cut-down to the only important properties for debugging
    // (mostly for tests).
    // If the record is already written to the log (in production) we have other ways to make
    // it readable again.
    final var builder =
        new StringBuilder(
            "RecordMetadata{"
                + "recordType="
                + recordType
                + ", valueType="
                + valueType
                + ", valueState="
                + valueState);
    if (rejectionType.capacity() > 0) {
      builder.append(", rejectionType=").append(getRejectionType());
    }
    if (rejectionReason.capacity() > 0) {
      builder.append(", rejectionReason=").append(bufferAsString(rejectionReason));
    }

    //    if (!authorization.isEmpty()) {
    //      builder.append(", authorization=").append(authorization);
    //    }
    if (operationReference != RecordMetadataEncoder.operationReferenceNullValue()) {
      builder.append(", operationReference=").append(operationReference);
    }
    if (batchOperationReference != RecordMetadataEncoder.batchOperationReferenceNullValue()) {
      builder.append(", batchOperationReference=").append(batchOperationReference);
    }

    builder.append('}');
    return builder.toString();
  }

  public RecordMetadata copy() {
    final RecordMetadata metadata = new RecordMetadata();
    metadata.recordType = recordType;
    metadata.requestStreamId = requestStreamId;
    metadata.requestId = requestId;
    metadata.protocolVersion = protocolVersion;
    metadata.valueType = valueType;
    metadata.valueState = valueState;
    metadata.valueFollowState = valueFollowState;
    metadata.rejectionType(getRejectionType());
    metadata.recordVersion = recordVersion;
    metadata.operationReference = operationReference;
    metadata.batchOperationReference = batchOperationReference;
    metadata.rejectionReason(getRejectionReason());
    metadata.brokerVersion = brokerVersion;
    return metadata;
  }
}
