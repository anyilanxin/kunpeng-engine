/*
 * Copyright © 2026 anyilanxin zxh (anyilanxin@aliyun.com)
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
package com.anyilanxin.kunpeng.cluster.dispatch.api;

import static com.anyilanxin.kunpeng.protocol.admin.impl.record.command.common.ExecutionRecordSerialize.decode;
import static com.anyilanxin.kunpeng.protocol.admin.impl.record.command.common.ExecutionRecordSerialize.encode;

import com.anyilanxin.kunpeng.protocol.common.UnifiedRecordValue;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * 集群元数据读取请求/应答 wire 编解码（消息通道内聚，不进 raft 日志与协议编号池）。
 *
 * <pre>
 * 请求: [type:1B][paramLen:2B][paramUtf]
 * 单值应答: [present:1B][recordBytes]（present=0 即 null）
 * 列表应答: [count:4B] + [len:4B][recordBytes]*
 * </pre>
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
final class ClusterMetaReadProtocol {
  static final byte TYPE_NODE_SOURCE = 1;
  static final byte TYPE_NODE_SOURCE_META = 2;
  static final byte TYPE_PARTITION_SOURCE = 3;
  static final byte TYPE_PARTITION_SOURCES = 4;
  static final byte TYPE_PARTITION_SOURCE_META = 5;
  static final byte TYPE_ADMIN_CLUSTER_META = 6;

  private ClusterMetaReadProtocol() {}

  record Request(byte type, String param) {}

  static byte[] encodeRequest(final byte type, final String param) {
    final byte[] paramBytes = param == null ? new byte[0] : param.getBytes(StandardCharsets.UTF_8);
    final ByteBuffer buffer = ByteBuffer.allocate(3 + paramBytes.length);
    buffer.put(type).putShort((short) paramBytes.length).put(paramBytes);
    return buffer.array();
  }

  static Request decodeRequest(final byte[] bytes) {
    final ByteBuffer buffer = ByteBuffer.wrap(bytes);
    final byte type = buffer.get();
    final int paramLength = buffer.getShort() & 0xFFFF;
    final byte[] paramBytes = new byte[paramLength];
    buffer.get(paramBytes);
    return new Request(
        type, paramLength == 0 ? null : new String(paramBytes, StandardCharsets.UTF_8));
  }

  static byte[] encodeSingle(final UnifiedRecordValue record) {
    if (record == null) {
      return new byte[] {0};
    }
    final byte[] payload = encode(record);
    final ByteBuffer buffer = ByteBuffer.allocate(1 + payload.length);
    buffer.put((byte) 1).put(payload);
    return buffer.array();
  }

  static byte[] encodeList(final List<? extends UnifiedRecordValue> records) {
    final List<byte[]> payloads = new ArrayList<>(records.size());
    int totalLength = 4;
    for (final UnifiedRecordValue record : records) {
      final byte[] payload = encode(record);
      payloads.add(payload);
      totalLength += 4 + payload.length;
    }
    final ByteBuffer buffer = ByteBuffer.allocate(totalLength);
    buffer.putInt(records.size());
    for (final byte[] payload : payloads) {
      buffer.putInt(payload.length).put(payload);
    }
    return buffer.array();
  }

  static <T extends UnifiedRecordValue> T decodeSingle(
      final byte[] bytes, final Supplier<T> valueFactory) {
    if (bytes[0] == 0) {
      return null;
    }
    final byte[] payload = new byte[bytes.length - 1];
    System.arraycopy(bytes, 1, payload, 0, payload.length);
    return decode(payload, valueFactory.get());
  }

  static <T extends UnifiedRecordValue> List<T> decodeList(
      final byte[] bytes, final Supplier<T> valueFactory) {
    final ByteBuffer buffer = ByteBuffer.wrap(bytes);
    final int count = buffer.getInt();
    final List<T> records = new ArrayList<>(count);
    for (int i = 0; i < count; i++) {
      final int payloadLength = buffer.getInt();
      final byte[] payload = new byte[payloadLength];
      buffer.get(payload);
      records.add(decode(payload, valueFactory.get()));
    }
    return records;
  }
}
