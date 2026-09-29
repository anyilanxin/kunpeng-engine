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
package com.anyilanxin.kunpeng.cluster.cluster.messaging;

import com.anyilanxin.kunpeng.cluster.cluster.Member;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.nodeinfo.MessageHeaderDecoder;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.nodeinfo.MessageHeaderEncoder;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.nodeinfo.NodeInfoDecoder;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.nodeinfo.NodeInfoEncoder;
import java.util.Base64;
import java.util.EnumMap;
import java.util.Map;
import java.util.Properties;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;

/**
 * 节点级元数据的单一载体（SBE 编码，{@code member-info-schema}）：当前为 {@link CommPortType} 端口注册表，后续节点级数据在此消息上演进。
 *
 * <p>作为<b>单个</b> SWIM 成员属性存储（Base64 包 SBE 字节）：写入侧原地更新属性触发 SWIM 元数据版本
 * +1，经既有二次拉取通道传播；拉取合并完成前读取方拿不到新值。 刻意不进内联白名单——节点级数据低频变更且拉取通道已保证收敛，不占线上 SWIM 报文体积。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class MemberNodeInfo {
  /** SWIM 成员属性键：值为 Base64(SBE NodeInfo)。 */
  public static final String PROPERTY_KEY = "kunpeng.node.info";

  private static final Base64.Encoder BASE64_ENCODER = Base64.getEncoder();
  private static final Base64.Decoder BASE64_DECODER = Base64.getDecoder();

  private MemberNodeInfo() {}

  /** 读取成员的端口映射；未广播（元数据未拉取补全）或解码失败时返回 {@code null}。 */
  public static Integer portOf(final Member member, final CommPortType type) {
    if (member == null) {
      return null;
    }
    return readPort(member.properties(), type);
  }

  /** 从属性集中读取端口；属性缺失或内容非法返回 {@code null}。 */
  public static Integer readPort(final Properties properties, final CommPortType type) {
    final Map<CommPortType, Integer> ports = portsOf(properties.getProperty(PROPERTY_KEY));
    return ports == null ? null : ports.get(type);
  }

  /** 原地更新本地属性中的端口映射（读旧-并-写新，单属性整体覆写）。 */
  public static void updatePort(
      final Properties properties, final CommPortType type, final int port) {
    final Map<CommPortType, Integer> ports = readCurrent(properties);
    ports.put(type, port);
    store(properties, ports);
  }

  /** 从本地属性移除一个端口；映射清空时连属性键一并移除。 */
  public static void removePort(final Properties properties, final CommPortType type) {
    final Map<CommPortType, Integer> ports = readCurrent(properties);
    if (ports.remove(type) == null) {
      return;
    }
    if (ports.isEmpty()) {
      properties.remove(PROPERTY_KEY);
    } else {
      store(properties, ports);
    }
  }

  private static Map<CommPortType, Integer> readCurrent(final Properties properties) {
    final Map<CommPortType, Integer> ports = portsOf(properties.getProperty(PROPERTY_KEY));
    return ports == null ? new EnumMap<>(CommPortType.class) : ports;
  }

  private static void store(final Properties properties, final Map<CommPortType, Integer> ports) {
    properties.setProperty(PROPERTY_KEY, BASE64_ENCODER.encodeToString(encode(ports)));
  }

  /** 解码属性值；缺属性或内容非法返回 {@code null}。 */
  private static Map<CommPortType, Integer> portsOf(final String propertyValue) {
    if (propertyValue == null || propertyValue.isEmpty()) {
      return null;
    }
    try {
      return decode(BASE64_DECODER.decode(propertyValue));
    } catch (final Exception error) {
      return null;
    }
  }

  private static byte[] encode(final Map<CommPortType, Integer> ports) {
    final int bufferLength =
        MessageHeaderEncoder.ENCODED_LENGTH
            + NodeInfoEncoder.BLOCK_LENGTH
            + NodeInfoEncoder.PortsEncoder.HEADER_SIZE
            + ports.size() * NodeInfoEncoder.PortsEncoder.sbeBlockLength();
    final MutableDirectBuffer buffer = new UnsafeBuffer(new byte[bufferLength]);

    final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
    final NodeInfoEncoder encoder = new NodeInfoEncoder();
    encoder.wrapAndApplyHeader(buffer, 0, headerEncoder);

    final NodeInfoEncoder.PortsEncoder portsEncoder = encoder.portsCount(ports.size());
    for (final Map.Entry<CommPortType, Integer> entry : ports.entrySet()) {
      portsEncoder.next().portType((short) entry.getKey().code()).port(entry.getValue());
    }
    final int encodedLength = headerEncoder.encodedLength() + encoder.encodedLength();
    final byte[] bytes = new byte[encodedLength];
    buffer.getBytes(0, bytes, 0, encodedLength);
    return bytes;
  }

  private static Map<CommPortType, Integer> decode(final byte[] bytes) {
    final UnsafeBuffer buffer = new UnsafeBuffer(bytes);
    final MessageHeaderDecoder headerDecoder = new MessageHeaderDecoder();
    final NodeInfoDecoder decoder = new NodeInfoDecoder();
    decoder.wrapAndApplyHeader(buffer, 0, headerDecoder);

    final Map<CommPortType, Integer> ports = new EnumMap<>(CommPortType.class);
    for (final NodeInfoDecoder.PortsDecoder entry : decoder.ports()) {
      final CommPortType type = CommPortType.of(entry.portType());
      // 未知类型编码(对端更新版本新增)安全跳过
      if (type != null) {
        ports.put(type, entry.port());
      }
    }
    return ports;
  }
}
