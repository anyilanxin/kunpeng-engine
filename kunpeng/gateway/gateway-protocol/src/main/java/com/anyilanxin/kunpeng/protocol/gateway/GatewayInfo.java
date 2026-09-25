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
package com.anyilanxin.kunpeng.protocol.gateway;

import static com.anyilanxin.kunpeng.protocol.gateway.GatewayInfoEncoder.*;
import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.wrapString;

import com.anyilanxin.kunpeng.structpack.buffer.BufferReader;
import com.anyilanxin.kunpeng.structpack.buffer.BufferWriter;
import com.anyilanxin.kunpeng.structpack.util.BufferUtil;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Base64.Decoder;
import java.util.Base64.Encoder;
import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;

/**
 * 网关信息：网关节点的标识与连接信息编解码。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class GatewayInfo implements BufferReader, BufferWriter {

  private static final String GATEWAY_INFO_PROPERTY_NAME = "gatewayInfoV2";
  private static final Encoder BASE_64_ENCODER = Base64.getEncoder();
  private static final Decoder BASE_64_DECODER = Base64.getDecoder();
  private static final Charset BASE_64_CHARSET = StandardCharsets.UTF_8;

  private final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
  private final MessageHeaderDecoder headerDecoder = new MessageHeaderDecoder();

  private final GatewayInfoEncoder bodyEncoder = new GatewayInfoEncoder();
  private final GatewayInfoDecoder bodyDecoder = new GatewayInfoDecoder();
  private DirectBuffer nodeId = new UnsafeBuffer();
  private DirectBuffer version = new UnsafeBuffer();
  private DirectBuffer grpcAddress = new UnsafeBuffer();
  private boolean allowClientDiscovery;

  public GatewayInfo() {
    reset();
  }

  public GatewayInfo(final String nodeId, final String grpcAddress) {
    reset();
    this.nodeId = wrapString(nodeId);
    this.grpcAddress = wrapString(grpcAddress);
  }

  public GatewayInfo reset() {
    nodeId.wrap(0, 0);
    grpcAddress.wrap(0, 0);
    version.wrap(0, 0);
    allowClientDiscovery = false;
    return this;
  }

  public String getNodeId() {
    return BufferUtil.bufferAsString(nodeId);
  }

  public GatewayInfo setNodeId(final String nodeId) {
    this.nodeId = wrapString(nodeId);
    return this;
  }

  public void setNodeId(final DirectBuffer buffer, final int offset, final int length) {
    nodeId.wrap(buffer, offset, length);
  }

  public String getGrpcAddress() {
    return BufferUtil.bufferAsString(grpcAddress);
  }

  public GatewayInfo setGrpcAddress(final String grpcAddress) {
    this.grpcAddress = wrapString(grpcAddress);
    return this;
  }

  public void setGrpcAddress(final DirectBuffer buffer, final int offset, final int length) {
    grpcAddress.wrap(buffer, offset, length);
  }

  /**
   * @deprecated use {@link #getGrpcAddress()} instead.
   */
  @Deprecated
  public String getGrpcApiAddress() {
    return getGrpcAddress();
  }

  /**
   * @deprecated use {@link #setGrpcAddress(String)} instead.
   */
  @Deprecated
  public GatewayInfo setGrpcApiAddress(final String address) {
    return setGrpcAddress(address);
  }

  public boolean isAllowClientDiscovery() {
    return allowClientDiscovery;
  }

  public GatewayInfo setAllowClientDiscovery(final boolean allowClientDiscovery) {
    this.allowClientDiscovery = allowClientDiscovery;
    return this;
  }

  public String getVersion() {
    return BufferUtil.bufferAsString(version);
  }

  public void setVersion(final String version) {
    this.version = wrapString(version);
  }

  public void setVersion(final DirectBuffer buffer, final int offset, final int length) {
    version.wrap(buffer, offset, length);
  }

  @Override
  public void wrap(final DirectBuffer buffer, int offset, final int length) {
    reset();

    final int frameEnd = offset + length;

    headerDecoder.wrap(buffer, offset);

    offset += headerDecoder.encodedLength();

    bodyDecoder.wrap(buffer, offset, headerDecoder.blockLength(), headerDecoder.version());
    allowClientDiscovery = bodyDecoder.allowClientDiscovery() == BooleanType.TRUE;
    if (bodyDecoder.grpcAddressLength() > 0) {
      bodyDecoder.wrapGrpcAddress(grpcAddress);
    } else {
      bodyDecoder.skipGrpcAddress();
    }
    if (bodyDecoder.nodeIdLength() > 0) {
      bodyDecoder.wrapNodeId(nodeId);
    } else {
      bodyDecoder.skipNodeId();
    }
    if (bodyDecoder.versionLength() > 0) {
      bodyDecoder.wrapVersion(version);
    } else {
      bodyDecoder.skipVersion();
    }

    assert bodyDecoder.limit() == frameEnd
        : "Decoder read only to position "
            + bodyDecoder.limit()
            + " but expected "
            + frameEnd
            + " as final position";
  }

  @Override
  public int getLength() {
    return headerEncoder.encodedLength()
        + bodyEncoder.sbeBlockLength()
        + grpcAddressHeaderLength()
        + grpcAddress.capacity()
        + nodeIdHeaderLength()
        + nodeId.capacity()
        + versionHeaderLength()
        + version.capacity();
  }

  @Override
  public void write(final MutableDirectBuffer buffer, int offset) {
    headerEncoder
        .wrap(buffer, offset)
        .blockLength(bodyEncoder.sbeBlockLength())
        .templateId(bodyEncoder.sbeTemplateId())
        .schemaId(bodyEncoder.sbeSchemaId())
        .version(bodyEncoder.sbeSchemaVersion());

    offset += headerEncoder.encodedLength();

    bodyEncoder.wrap(buffer, offset);
    bodyEncoder.allowClientDiscovery(allowClientDiscovery ? BooleanType.TRUE : BooleanType.FALSE);
    bodyEncoder.putGrpcAddress(grpcAddress, 0, grpcAddress.capacity());
    bodyEncoder.putNodeId(nodeId, 0, nodeId.capacity());
    bodyEncoder.putVersion(version, 0, version.capacity());
  }

  public static GatewayInfo fromProperties(final Properties properties) {
    final String property = properties.getProperty(GATEWAY_INFO_PROPERTY_NAME);
    if (property != null) {
      return readFromString(property);
    } else {
      return null;
    }
  }

  private static GatewayInfo readFromString(final String property) {
    final byte[] bytes = BASE_64_DECODER.decode(property.getBytes(BASE_64_CHARSET));
    final GatewayInfo gatewayInfo = new GatewayInfo();
    gatewayInfo.wrap(new UnsafeBuffer(bytes), 0, bytes.length);
    return gatewayInfo;
  }

  public void writeIntoProperties(final Properties memberProperties) {
    memberProperties.setProperty(GATEWAY_INFO_PROPERTY_NAME, writeToString());
  }

  private String writeToString() {
    final byte[] bytes = new byte[getLength()];
    final UnsafeBuffer buffer = new UnsafeBuffer(bytes);
    write(buffer, 0);
    return new String(BASE_64_ENCODER.encode(bytes), BASE_64_CHARSET);
  }

  @Override
  public boolean equals(final Object object) {
    if (!(object instanceof final GatewayInfo that)) {
      return false;
    }
    return Objects.equals(nodeId, that.nodeId);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(nodeId);
  }

  @Override
  public String toString() {
    return "GatewayInfo{"
        + "nodeId="
        + BufferUtil.bufferAsString(nodeId)
        + ", grpcAddress="
        + BufferUtil.bufferAsString(grpcAddress)
        + ", version="
        + BufferUtil.bufferAsString(version)
        + ", allowClientDiscovery="
        + allowClientDiscovery
        + '}';
  }
}
