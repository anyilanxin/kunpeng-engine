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
package com.anyilanxin.kunpeng.cluster.benchmark;

import com.anyilanxin.kunpeng.cluster.utils.serializer.Namespace;
import com.anyilanxin.kunpeng.cluster.utils.serializer.Namespaces;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;
import org.apache.fory.Fory;
import org.apache.fory.ThreadSafeFory;

/**
 * 序列化基准共用支撑：模拟消息 POJO 与 Kryo/Fory 实例构建。
 *
 * <p>Kryo 侧复用项目真实用法：{@link Namespaces#BASIC} 基础注册 + 用户自定义 id 区段（池化实例，
 * {@code writeClassAndObject}/{@code readClassAndObject}）；Fory 侧按官方文档 native 模式注册。
 */
final class BenchmarkSupport {

  private BenchmarkSupport() {}

  /** 构建项目真实用法的 Kryo 命名空间（线程安全池化实例容器）。 */
  static Namespace newKryoNamespace() {
    return new Namespace.Builder()
        .register(Namespaces.BASIC)
        .nextId(Namespaces.BEGIN_USER_CUSTOM_ID)
        .register(MessageHeader.class, PartitionMessage.class)
        .name("BENCH")
        .build();
  }

  /** 构建 Fory 单线程实例：native 模式 + 显式类型 id 注册（官方文档推荐用法）。 */
  static Fory newFory() {
    final Fory fory = Fory.builder().withXlang(false).requireClassRegistration(true).build();
    fory.register(MessageHeader.class, 20);
    fory.register(PartitionMessage.class, 21);
    return fory;
  }

  /** 构建 Fory 线程安全实例：对应集群多线程消息收发场景。 */
  static ThreadSafeFory newThreadSafeFory() {
    final ThreadSafeFory fory =
        Fory.builder().withXlang(false).requireClassRegistration(true).buildThreadSafeFory();
    fory.register(MessageHeader.class, 20);
    fory.register(PartitionMessage.class, 21);
    return fory;
  }

  /** 构造模拟分区消息：嵌套对象 + 集合 + 字节数组，覆盖项目消息的典型字段形态。 */
  static PartitionMessage newMessage(
      final int payloadSize, final int memberCount, final int progressSize) {
    final var message = new PartitionMessage();
    message.index = 123456789L;
    message.header = new MessageHeader();
    message.header.term = 7;
    message.header.partitionId = 1;
    message.header.nodeId = "node-1";
    message.members = new ArrayList<>(memberCount);
    for (int i = 0; i < memberCount; i++) {
      message.members.add("member-node-" + i);
    }
    message.progress = new HashMap<>(progressSize * 2);
    for (int i = 0; i < progressSize; i++) {
      message.progress.put("member-node-" + i, (long) i * 1000);
    }
    message.payload = new byte[payloadSize];
    Arrays.fill(message.payload, (byte) 0x5A);
    return message;
  }

  /** 模拟集群消息头。 */
  public static final class MessageHeader {
    public long term;
    public int partitionId;
    public String nodeId;

    @Override
    public boolean equals(final Object obj) {
      if (!(obj instanceof MessageHeader that)) {
        return false;
      }
      return term == that.term
          && partitionId == that.partitionId
          && Objects.equals(nodeId, that.nodeId);
    }

    @Override
    public int hashCode() {
      return Objects.hash(term, partitionId, nodeId);
    }
  }

  /** 模拟分区消息体。 */
  public static final class PartitionMessage {
    public long index;
    public MessageHeader header;
    public ArrayList<String> members;
    public HashMap<String, Long> progress;
    public byte[] payload;

    @Override
    public boolean equals(final Object obj) {
      if (!(obj instanceof PartitionMessage that)) {
        return false;
      }
      return index == that.index
          && Objects.equals(header, that.header)
          && Objects.equals(members, that.members)
          && Objects.equals(progress, that.progress)
          && Arrays.equals(payload, that.payload);
    }

    @Override
    public int hashCode() {
      return Objects.hash(index, header, members, progress, Arrays.hashCode(payload));
    }
  }
}
