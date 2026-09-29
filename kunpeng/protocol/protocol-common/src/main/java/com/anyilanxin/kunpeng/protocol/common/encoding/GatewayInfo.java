/*
 * Copyright © 2026 anyilanxin zxh(anyilanxin.com)
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
package com.anyilanxin.kunpeng.protocol.common.encoding;

import com.anyilanxin.kunpeng.protocol.common.ClusterCommonConstant;
import com.anyilanxin.kunpeng.protocol.common.member.GatewayInfoDecoder;
import com.anyilanxin.kunpeng.protocol.common.member.GatewayInfoEncoder;
import com.anyilanxin.kunpeng.protocol.common.member.MessageHeaderDecoder;
import com.anyilanxin.kunpeng.protocol.common.member.MessageHeaderEncoder;
import com.anyilanxin.kunpeng.structpack.buffer.BufferReader;
import com.anyilanxin.kunpeng.structpack.buffer.BufferWriter;
import java.util.Base64;
import java.util.Properties;
import org.agrona.DirectBuffer;
import org.agrona.ExpandableArrayBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;

/**
 * gateway 发现信息实体：网关的 client 接入地址（clientHost 为空时回落成员主机），一个实体一个属性键，存放于网关成员属性 （值为 Base64(SBE
 * 帧)，满足成员属性只能是字符串的约束）。
 *
 * <p>管理侧（网关启动）写入 {@link #advertise(Properties, String, int)}，收集侧（gateway 地址发现服务，提供给 client
 * 集群网关发现）{@link #fromProperties(Properties)}；实体只管把数据放进 properties——是否传播、何时传播由 SWIM 底层自行决定。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class GatewayInfo implements BufferReader, BufferWriter {
  /** 成员属性键（统一定义于 {@link ClusterCommonConstant}）。 */
  public static final String PROPERTY_NAME = ClusterCommonConstant.GATEWAY_INFO_PROPERTY;

  private static final Base64.Encoder BASE_64_ENCODER = Base64.getEncoder();
  private static final Base64.Decoder BASE_64_DECODER = Base64.getDecoder();

  private final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
  private final MessageHeaderDecoder headerDecoder = new MessageHeaderDecoder();

  private int clientPort;
  private String clientHost = "";

  public int getClientPort() {
    return clientPort;
  }

  public GatewayInfo setClientPort(final int clientPort) {
    this.clientPort = clientPort;
    return this;
  }

  /** 对外通告主机；空串表示回落成员主机（收集侧解析）。 */
  public String getClientHost() {
    return clientHost;
  }

  public GatewayInfo setClientHost(final String clientHost) {
    this.clientHost = clientHost == null ? "" : clientHost;
    return this;
  }

  /** 网关管理侧入口：把 client 接入地址写入本地成员属性。 */
  public static void advertise(
      final Properties properties, final String clientHost, final int clientPort) {
    new GatewayInfo()
        .setClientHost(clientHost)
        .setClientPort(clientPort)
        .writeIntoProperties(properties);
  }

  /** 读取成员属性中的实体；缺属性或非法帧返回 {@code null}。 */
  public static GatewayInfo fromProperties(final Properties properties) {
    final String encoded = properties.getProperty(PROPERTY_NAME);
    if (encoded == null || encoded.isEmpty()) {
      return null;
    }
    try {
      final byte[] bytes = BASE_64_DECODER.decode(encoded);
      final GatewayInfo info = new GatewayInfo();
      info.wrap(new UnsafeBuffer(bytes), 0, bytes.length);
      return info;
    } catch (final RuntimeException e) {
      return null;
    }
  }

  public void writeIntoProperties(final Properties properties) {
    properties.setProperty(PROPERTY_NAME, BASE_64_ENCODER.encodeToString(encodeFrame()));
  }

  @Override
  public void wrap(final DirectBuffer buffer, final int offset, final int length) {
    headerDecoder.wrap(buffer, offset);
    final GatewayInfoDecoder bodyDecoder = new GatewayInfoDecoder();
    bodyDecoder.wrap(
        buffer,
        offset + headerDecoder.encodedLength(),
        headerDecoder.blockLength(),
        headerDecoder.version());
    clientPort = bodyDecoder.clientPort();
    clientHost = bodyDecoder.clientHost();
  }

  @Override
  public int getLength() {
    return encodeFrame().length;
  }

  @Override
  public void write(final MutableDirectBuffer buffer, final int offset) {
    buffer.putBytes(offset, encodeFrame());
  }

  /** 编码为自扩展缓冲后拷出；帧长 = 消息头 + body 长（encodedLength 不含消息头）。 */
  private byte[] encodeFrame() {
    final ExpandableArrayBuffer buffer = new ExpandableArrayBuffer(64);
    final GatewayInfoEncoder encoder = new GatewayInfoEncoder();
    encoder.wrapAndApplyHeader(buffer, 0, headerEncoder);
    encoder.clientPort(clientPort).clientHost(clientHost);
    final byte[] bytes = new byte[MessageHeaderEncoder.ENCODED_LENGTH + encoder.encodedLength()];
    buffer.getBytes(0, bytes);
    return bytes;
  }
}
