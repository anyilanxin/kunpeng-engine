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
package com.anyilanxin.kunpeng.protocol.common.encoding;

import com.anyilanxin.kunpeng.protocol.common.ClusterCommonConstant;
import com.anyilanxin.kunpeng.protocol.common.member.BrokerInfoDecoder;
import com.anyilanxin.kunpeng.protocol.common.member.BrokerInfoEncoder;
import com.anyilanxin.kunpeng.protocol.common.member.CommPortType;
import com.anyilanxin.kunpeng.protocol.common.member.MessageHeaderDecoder;
import com.anyilanxin.kunpeng.protocol.common.member.MessageHeaderEncoder;
import com.anyilanxin.kunpeng.protocol.common.member.PartitionHealth;
import com.anyilanxin.kunpeng.protocol.common.member.PartitionRole;
import com.anyilanxin.kunpeng.structpack.buffer.BufferReader;
import com.anyilanxin.kunpeng.structpack.buffer.BufferWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;

/**
 * broker 节点实体：端口注册表 + 本成员各分区状态，一个实体一个属性键，存放于成员属性。
 *
 * <p>本类是实体在属性中的唯一读写入口（对齐参考实现 BrokerInfo 的封装）：写侧原地改实体后 {@link #writeIntoProperties(Properties)}，读侧
 * {@link #fromProperties(Properties)}。实体只管把数据放进 properties——是否传播、何时传播由 SWIM 底层自行决定，业务侧不通知也不感知。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class BrokerInfo implements BufferReader, BufferWriter {
  /** 成员属性键（统一定义于 {@link ClusterCommonConstant}）。 */
  public static final String PROPERTY_NAME = ClusterCommonConstant.BROKER_INFO_PROPERTY;

  private static final Base64.Encoder BASE_64_ENCODER = Base64.getEncoder();
  private static final Base64.Decoder BASE_64_DECODER = Base64.getDecoder();

  private final MessageHeaderEncoder headerEncoder = new MessageHeaderEncoder();
  private final MessageHeaderDecoder headerDecoder = new MessageHeaderDecoder();
  private final BrokerInfoEncoder bodyEncoder = new BrokerInfoEncoder();
  private final BrokerInfoDecoder bodyDecoder = new BrokerInfoDecoder();

  private final Map<CommPortType, Integer> ports = new LinkedHashMap<>();
  private final List<PartitionEntry> partitions = new ArrayList<>();

  /** 分区条目：分区身份 + 角色/健康/任期 + source 元数据。 */
  public static final class PartitionEntry {
    public int partitionId;
    public String groupName = "";
    public PartitionRole role = PartitionRole.UNKNOWN;
    public PartitionHealth health = PartitionHealth.UNKNOWN;
    public long term;
    public int sourceId = -1;
    public List<Integer> agentSourceIds = new ArrayList<>();
  }

  public Map<CommPortType, Integer> getPorts() {
    return ports;
  }

  public List<PartitionEntry> getPartitions() {
    return partitions;
  }

  public BrokerInfo addPort(final CommPortType portType, final int port) {
    ports.put(portType, port);
    return this;
  }

  public BrokerInfo addPartition(final PartitionEntry entry) {
    partitions.add(entry);
    return this;
  }

  public BrokerInfo reset() {
    ports.clear();
    partitions.clear();
    return this;
  }

  /** 读取成员属性中的实体；缺属性或非法帧返回 {@code null}（非法帧记 warn 由调用方决定，此处静默以兼容滚动升级）。 */
  public static BrokerInfo fromProperties(final Properties properties) {
    final String encoded = properties.getProperty(PROPERTY_NAME);
    if (encoded == null || encoded.isEmpty()) {
      return null;
    }
    try {
      final byte[] bytes = BASE_64_DECODER.decode(encoded);
      final BrokerInfo brokerInfo = new BrokerInfo();
      brokerInfo.wrap(new UnsafeBuffer(bytes), 0, bytes.length);
      return brokerInfo;
    } catch (final RuntimeException e) {
      return null;
    }
  }

  public void writeIntoProperties(final Properties properties) {
    final byte[] bytes = new byte[getLength()];
    final UnsafeBuffer buffer = new UnsafeBuffer(bytes);
    write(buffer, 0);
    properties.setProperty(PROPERTY_NAME, BASE_64_ENCODER.encodeToString(bytes));
  }

  @Override
  public void wrap(final DirectBuffer buffer, int offset, final int length) {
    reset();
    headerDecoder.wrap(buffer, offset);
    offset += headerDecoder.encodedLength();
    bodyDecoder.wrap(buffer, offset, headerDecoder.blockLength(), headerDecoder.version());

    final BrokerInfoDecoder.PortsDecoder portsDecoder = bodyDecoder.ports();
    while (portsDecoder.hasNext()) {
      portsDecoder.next();
      ports.put(portsDecoder.portType(), portsDecoder.port());
    }

    final BrokerInfoDecoder.PartitionsDecoder partitionsDecoder = bodyDecoder.partitions();
    while (partitionsDecoder.hasNext()) {
      partitionsDecoder.next();
      final PartitionEntry entry = new PartitionEntry();
      entry.partitionId = partitionsDecoder.partitionId();
      entry.role = partitionsDecoder.role();
      entry.health = partitionsDecoder.health();
      entry.term = partitionsDecoder.term();
      entry.sourceId = partitionsDecoder.sourceId();
      final BrokerInfoDecoder.PartitionsDecoder.AgentSourceIdsDecoder agentDecoder =
          partitionsDecoder.agentSourceIds();
      while (agentDecoder.hasNext()) {
        agentDecoder.next();
        entry.agentSourceIds.add(agentDecoder.sourceId());
      }
      entry.groupName = partitionsDecoder.groupName();
      partitions.add(entry);
    }
  }

  @Override
  public int getLength() {
    int length =
        headerEncoder.encodedLength()
            + bodyEncoder.sbeBlockLength()
            + BrokerInfoEncoder.PortsEncoder.HEADER_SIZE
            + ports.size() * BrokerInfoEncoder.PortsEncoder.sbeBlockLength()
            + BrokerInfoEncoder.PartitionsEncoder.HEADER_SIZE;
    for (final PartitionEntry entry : partitions) {
      length +=
          BrokerInfoEncoder.PartitionsEncoder.sbeBlockLength()
              + BrokerInfoEncoder.PartitionsEncoder.AgentSourceIdsEncoder.HEADER_SIZE
              + entry.agentSourceIds.size()
                  * BrokerInfoEncoder.PartitionsEncoder.AgentSourceIdsEncoder.sbeBlockLength()
              + BrokerInfoEncoder.PartitionsEncoder.groupNameHeaderLength()
              + entry.groupName.getBytes(StandardCharsets.UTF_8).length;
    }
    return length;
  }

  @Override
  public void write(final MutableDirectBuffer buffer, final int offset) {
    headerEncoder
        .wrap(buffer, offset)
        .blockLength(bodyEncoder.sbeBlockLength())
        .templateId(bodyEncoder.sbeTemplateId())
        .schemaId(bodyEncoder.sbeSchemaId())
        .version(bodyEncoder.sbeSchemaVersion());

    bodyEncoder.wrap(buffer, offset + headerEncoder.encodedLength());

    final BrokerInfoEncoder.PortsEncoder portsEncoder = bodyEncoder.portsCount(ports.size());
    for (final Map.Entry<CommPortType, Integer> entry : ports.entrySet()) {
      portsEncoder.next().portType(entry.getKey()).port(entry.getValue());
    }

    final BrokerInfoEncoder.PartitionsEncoder partitionsEncoder =
        bodyEncoder.partitionsCount(partitions.size());
    for (final PartitionEntry entry : partitions) {
      final BrokerInfoEncoder.PartitionsEncoder next = partitionsEncoder.next();
      next.partitionId(entry.partitionId)
          .role(entry.role)
          .health(entry.health)
          .term(entry.term)
          .sourceId(entry.sourceId);
      final BrokerInfoEncoder.PartitionsEncoder.AgentSourceIdsEncoder agentEncoder =
          next.agentSourceIdsCount(entry.agentSourceIds.size());
      for (final Integer agentSourceId : entry.agentSourceIds) {
        agentEncoder.next().sourceId(agentSourceId);
      }
      final byte[] groupName = entry.groupName.getBytes(StandardCharsets.UTF_8);
      next.putGroupName(groupName, 0, groupName.length);
    }
  }
}
