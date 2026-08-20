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
import java.util.HashMap;
import java.util.Map;

/**
 * 跨节点调度响应。
 *
 * @param requestId 关联的请求 ID
 * @param success 是否成功
 * @param errorMessage 失败原因（成功时为 null）
 * @param responseData 附加数据（如查询结果）
 */
public record RaftScheduleResponse(
    String requestId, boolean success, String errorMessage, Map<String, String> responseData) {

  public RaftScheduleResponse {
    responseData = responseData == null ? Map.of() : Map.copyOf(responseData);
  }

  public static RaftScheduleResponse ok(final String requestId) {
    return new RaftScheduleResponse(requestId, true, null, Map.of());
  }

  public static RaftScheduleResponse ok(
      final String requestId, final Map<String, String> data) {
    return new RaftScheduleResponse(requestId, true, null, data);
  }

  public static RaftScheduleResponse error(final String requestId, final String message) {
    return new RaftScheduleResponse(requestId, false, message, Map.of());
  }

  public byte[] encode() {
    try {
      final var bos = new ByteArrayOutputStream();
      final var out = new DataOutputStream(bos);
      writeString(out, requestId);
      out.writeBoolean(success);
      writeString(out, errorMessage != null ? errorMessage : "");
      out.writeInt(responseData.size());
      for (final var entry : responseData.entrySet()) {
        writeString(out, entry.getKey());
        writeString(out, entry.getValue());
      }
      out.flush();
      return bos.toByteArray();
    } catch (final IOException e) {
      throw new RuntimeException("序列化失败", e);
    }
  }

  public static RaftScheduleResponse decode(final byte[] bytes) {
    try {
      final var in = new DataInputStream(new ByteArrayInputStream(bytes));
      final var requestId = readString(in);
      final var success = in.readBoolean();
      final var errorMsg = readString(in);
      final var size = in.readInt();
      final var data = new HashMap<String, String>();
      for (int i = 0; i < size; i++) {
        data.put(readString(in), readString(in));
      }
      return new RaftScheduleResponse(
          requestId, success, errorMsg.isEmpty() ? null : errorMsg, data);
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
