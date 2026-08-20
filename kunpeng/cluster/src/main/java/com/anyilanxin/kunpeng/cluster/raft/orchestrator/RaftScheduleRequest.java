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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.cluster.raft.orchestrator;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 跨节点调度请求（经 ClusterCommunicationService 传输）。
 *
 * @param requestId 唯一请求 ID（关联响应）
 * @param operation 调度操作类型
 * @param groupName 目标分区组名
 * @param targetNodeId 目标节点 ID（接收方校验）
 * @param parameters 操作参数（灵活的 key-value）
 */
public record RaftScheduleRequest(
    String requestId,
    RaftScheduleOperation operation,
    String groupName,
    String targetNodeId,
    Map<String, String> parameters) {

  public RaftScheduleRequest {
    if (requestId == null || requestId.isEmpty()) {
      throw new IllegalArgumentException("requestId 不能为空");
    }
    parameters = parameters == null ? Map.of() : Map.copyOf(parameters);
  }

  /** 获取字符串参数 */
  public String param(final String key) {
    return parameters.get(key);
  }

  /** 获取整数参数 */
  public int intParam(final String key, final int defaultValue) {
    final var value = parameters.get(key);
    return value != null ? Integer.parseInt(value) : defaultValue;
  }

  /** 序列化为字节数组（通过 ClusterCommunicationService 传输） */
  public byte[] encode() {
    try {
      final var bos = new ByteArrayOutputStream();
      final var out = new DataOutputStream(bos);
      writeString(out, requestId);
      out.writeUTF(operation.name());
      writeString(out, groupName);
      writeString(out, targetNodeId);
      out.writeInt(parameters.size());
      for (final var entry : parameters.entrySet()) {
        writeString(out, entry.getKey());
        writeString(out, entry.getValue());
      }
      out.flush();
      return bos.toByteArray();
    } catch (final IOException e) {
      throw new RuntimeException("序列化失败", e);
    }
  }

  /** 从字节数组反序列化 */
  public static RaftScheduleRequest decode(final byte[] bytes) {
    try {
      final var in = new DataInputStream(new ByteArrayInputStream(bytes));
      final var requestId = readString(in);
      final var operation = RaftScheduleOperation.valueOf(in.readUTF());
      final var groupName = readString(in);
      final var targetNodeId = readString(in);
      final var size = in.readInt();
      final var params = new HashMap<String, String>();
      for (int i = 0; i < size; i++) {
        params.put(readString(in), readString(in));
      }
      return new RaftScheduleRequest(requestId, operation, groupName, targetNodeId, params);
    } catch (final IOException e) {
      throw new RuntimeException("反序列化失败", e);
    }
  }

  private static void writeString(final DataOutputStream out, final String value) throws IOException {
    final var bytes = value.getBytes(StandardCharsets.UTF_8);
    out.writeInt(bytes.length);
    out.write(bytes);
  }

  private static String readString(final DataInputStream in) throws IOException {
    final var length = in.readInt();
    final var bytes = new byte[length];
    in.readFully(bytes);
    return new String(bytes, StandardCharsets.UTF_8);
  }
}
