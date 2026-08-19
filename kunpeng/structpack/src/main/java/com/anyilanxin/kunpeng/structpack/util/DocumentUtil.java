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
package com.anyilanxin.kunpeng.structpack.util;

import com.anyilanxin.kunpeng.structpack.JsonSerializable;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.agrona.DirectBuffer;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * 文档(流程变量)内容互转工具：标准 msgpack 字节 ↔ JSON / Map / Object。
 *
 * <p>与 Record 帧格式无关——文档内容保持标准 msgpack（与存量变量字节、gateway/job API 完全兼容）。 msgpack 编解码由自研 {@link
 * DocumentCodec} 完成（字节与官方实现一致, 零外部依赖）； JSON 解析仍走 Jackson。
 */
public final class DocumentUtil {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private DocumentUtil() {}

  public static byte[] convertToMsgPack(final String json) {
    try {
      return DocumentCodec.pack(MAPPER.readValue(json, Object.class));
    } catch (final JacksonException e) {
      throw new IllegalArgumentException("Could not deserialize JSON", e);
    }
  }

  public static byte[] convertToMsgPack(final InputStream inputStream) {
    try {
      return DocumentCodec.pack(MAPPER.readValue(inputStream, Object.class));
    } catch (final JacksonException e) {
      throw new IllegalArgumentException("Could not read JSON from stream", e);
    }
  }

  public static byte[] convertToMsgPack(final Object value) {
    try {
      return DocumentCodec.pack(value);
    } catch (final IllegalArgumentException e) {
      // 兜底: POJO/日期/非字符串键等非树类型走 JSON 中转归一化（与原实现一致）
      return DocumentCodec.pack(normalize(value));
    }
  }

  public static String convertToJson(final DirectBuffer buffer) {
    return convertToJson(readBuffer(buffer));
  }

  public static String convertToJson(final byte[] msgPack) {
    return writeJson(DocumentCodec.unpack(msgPack));
  }

  public static Map<String, Object> convertToMap(final DirectBuffer buffer) {
    final Object result = convertToMsgPack(buffer);
    if (result instanceof Map) {
      @SuppressWarnings("unchecked")
      final Map<String, Object> map = (Map<String, Object>) result;
      return map;
    }
    throw new IllegalArgumentException(
        "Expected msgpack map but was: " + (result == null ? "null" : result.getClass()));
  }

  /** msgpack 字节 → Java 对象（Map/List/标量树） */
  public static Object convertToMsgPack(final DirectBuffer buffer) {
    final byte[] bytes = readBuffer(buffer);
    return DocumentCodec.unpack(bytes);
  }

  public static String convertJsonSerializableObjectToJson(final JsonSerializable serializable) {
    return serializable.toJson();
  }

  public static <T> T convertToObject(final DirectBuffer buffer, final Class<T> clazz) {
    final String json = convertToJson(buffer);
    try {
      return MAPPER.readValue(json, clazz);
    } catch (final JacksonException e) {
      throw new IllegalArgumentException("Could not bind msgpack to " + clazz, e);
    }
  }

  // ===== 内部 =====

  private static byte[] readBuffer(final DirectBuffer buffer) {
    return BufferUtil.bufferAsArray(buffer);
  }

  private static String writeJson(final Object value) {
    try {
      return MAPPER.writeValueAsString(value);
    } catch (final JacksonException e) {
      throw new IllegalArgumentException("Could not convert msgpack to JSON", e);
    }
  }

  /**
   * 归一化为 codec 可打包的树类型：Integer/Short 等统一为 Long, Float 统一为 Double, 容器逐层重建; POJO/日期等非树类型走 JSON
   * 中转（与原实现兜底一致）。
   */
  private static Object normalize(final Object value) {
    if (value == null
        || value instanceof Boolean
        || value instanceof String
        || value instanceof Double
        || value instanceof Float
        || value instanceof Long) {
      return value;
    }
    switch (value) {
      case final Number n -> {
        return n.longValue();
      }
      case final Map<?, ?> map -> {
        final Map<String, Object> out = new LinkedHashMap<>(Math.min(map.size(), 16));
        for (final Map.Entry<?, ?> entry : map.entrySet()) {
          out.put(String.valueOf(entry.getKey()), normalize(entry.getValue()));
        }
        return out;
      }
      case final Iterable<?> iterable -> {
        final List<Object> out = new ArrayList<>();
        for (final Object item : iterable) {
          out.add(normalize(item));
        }
        return out;
      }
      default -> {}
    }
    // 兜底: 走 JSON 中转(日期/POJO 等)
    return MAPPER.readValue(MAPPER.writeValueAsString(value), Object.class);
  }
}
