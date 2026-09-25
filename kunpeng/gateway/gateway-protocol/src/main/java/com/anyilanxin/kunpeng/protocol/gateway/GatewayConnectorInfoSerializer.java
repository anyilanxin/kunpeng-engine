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

import static com.anyilanxin.kunpeng.protocol.gateway.GatewayConnectInfoEncoder.nodeIdHeaderLength;

import org.agrona.concurrent.UnsafeBuffer;

/**
 * 网关连接器信息序列化器：网关连接器 Record 的二进制编解码。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class GatewayConnectorInfoSerializer {
  private GatewayConnectorInfoSerializer() {}

  public static GatewayConnectorRecord decode(final byte[] bytes) {
    final MessageHeaderDecoder headerDecoder = new MessageHeaderDecoder();
    final GatewayConnectInfoDecoder bodyDecoder = new GatewayConnectInfoDecoder();
    final UnsafeBuffer buffer = new UnsafeBuffer(bytes);
    int offset = 0;
    headerDecoder.wrap(buffer, offset);
    offset += headerDecoder.encodedLength();
    bodyDecoder.wrap(buffer, offset, headerDecoder.blockLength(), headerDecoder.version());
    final int connectorNum = bodyDecoder.connectorNum();
    final String nodeId = bodyDecoder.nodeId();
    return new GatewayConnectorRecord(nodeId, connectorNum);
  }

  public static byte[] encode(final GatewayConnectorRecord record) {
    final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
    final GatewayConnectInfoEncoder bodyEncoder = new GatewayConnectInfoEncoder();
    final int length =
        headerEncoder.encodedLength()
            + bodyEncoder.sbeBlockLength()
            + nodeIdHeaderLength()
            + record.nodeId().length();
    final byte[] bytes = new byte[length];
    final UnsafeBuffer buffer = new UnsafeBuffer(bytes);
    bodyEncoder
        .wrapAndApplyHeader(buffer, 0, headerEncoder)
        .connectorNum(record.connectorNum())
        .nodeId(record.nodeId());
    return bytes;
  }
}
