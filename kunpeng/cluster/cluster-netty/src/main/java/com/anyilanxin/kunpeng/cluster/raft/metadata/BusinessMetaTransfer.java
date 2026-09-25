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
package com.anyilanxin.kunpeng.cluster.raft.metadata;

import com.anyilanxin.kunpeng.cluster.raft.storage.serializer.BusinessMetaEntryDecoder;
import com.anyilanxin.kunpeng.cluster.raft.storage.serializer.BusinessMetaEntryEncoder;
import com.anyilanxin.kunpeng.cluster.raft.storage.serializer.MessageHeaderDecoder;
import com.anyilanxin.kunpeng.cluster.raft.storage.serializer.MessageHeaderEncoder;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.agrona.ExpandableArrayBuffer;
import org.agrona.concurrent.UnsafeBuffer;

/** 业务元数据修改请求/响应的线格式编解码（byte[] 载荷，经 ClusterCommunicationService 传输）。 */
public final class BusinessMetaTransfer {

  /** 请求：1 字节 forwarded 标志 + SBE BusinessMetaEntry 编码的全量键值对（整体覆盖语义）。 */
  public record Request(Map<String, String> entries, boolean forwarded) {}

  private BusinessMetaTransfer() {}

  public static byte[] encodeRequest(final Map<String, String> entries, final boolean forwarded) {
    final var buffer = new ExpandableArrayBuffer(64);
    buffer.putByte(0, (byte) (forwarded ? 1 : 0));
    final var encoder = new BusinessMetaEntryEncoder();
    final var header = new MessageHeaderEncoder();
    encoder.wrapAndApplyHeader(buffer, Byte.BYTES, header);
    final var itemsEncoder = encoder.itemsCount(entries.size());
    for (final var entry : entries.entrySet()) {
      final byte[] key = entry.getKey().getBytes(StandardCharsets.UTF_8);
      final byte[] value = entry.getValue().getBytes(StandardCharsets.UTF_8);
      itemsEncoder.next().putKey(key, 0, key.length).putValue(value, 0, value.length);
    }
    final int totalLength = Byte.BYTES + header.encodedLength() + encoder.encodedLength();
    final byte[] out = new byte[totalLength];
    buffer.getBytes(0, out);
    return out;
  }

  public static Request decodeRequest(final byte[] payload) {
    final var buffer = new UnsafeBuffer(payload);
    final boolean forwarded = buffer.getByte(0) == 1;
    final var decoder = new BusinessMetaEntryDecoder();
    final var header = new MessageHeaderDecoder();
    decoder.wrapAndApplyHeader(buffer, Byte.BYTES, header);
    final BusinessMetaEntryDecoder.ItemsDecoder itemsDecoder = decoder.items();
    final Map<String, String> entries = new HashMap<>(itemsDecoder.count());
    for (final BusinessMetaEntryDecoder.ItemsDecoder item : itemsDecoder) {
      final byte[] key = new byte[item.keyLength()];
      item.getKey(key, 0, key.length);
      final byte[] value = new byte[item.valueLength()];
      item.getValue(value, 0, value.length);
      entries.put(
          new String(key, StandardCharsets.UTF_8), new String(value, StandardCharsets.UTF_8));
    }
    return new Request(entries, forwarded);
  }

  public static byte[] encodeResponse(final BusinessMetaUpdateResponse response) {
    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    final DataOutputStream data = new DataOutputStream(out);
    try {
      data.writeBoolean(response.success());
      data.writeLong(response.index());
      data.writeUTF(response.error() == null ? "" : response.error());
      data.flush();
    } catch (final IOException e) {
      throw new IllegalStateException("Failed to encode business meta response", e);
    }
    return out.toByteArray();
  }

  public static BusinessMetaUpdateResponse decodeResponse(final byte[] payload) {
    try {
      final DataInputStream in = new DataInputStream(new ByteArrayInputStream(payload));
      final boolean success = in.readBoolean();
      final long index = in.readLong();
      final String error = in.readUTF();
      return new BusinessMetaUpdateResponse(success, index, error.isEmpty() ? null : error);
    } catch (final IOException e) {
      throw new IllegalArgumentException("Failed to decode business meta response", e);
    }
  }
}
