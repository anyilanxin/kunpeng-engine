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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.anyilanxin.kunpeng.structpack.JsonSerializable;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.agrona.concurrent.UnsafeBuffer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** DocumentUtil 测试：JSON ↔ 标准 msgpack ↔ Map/Object 桥接 */
@DisplayName("DocumentUtil 文档桥接")
class DocumentUtilTest {

  @Test
  @DisplayName("JSON → msgpack → JSON 全类型往返")
  void jsonRoundTrip() {
    final String json =
        "{\"i\":42,\"neg\":-7,\"big\":9007199254740993,\"d\":1.5,\"s\":\"中文🚀\","
            + "\"b\":true,\"n\":null,\"arr\":[1,\"two\",false,null],"
            + "\"nested\":{\"deep\":{\"x\":1.25}},\"emptyMap\":{},\"emptyArr\":[]}";
    final byte[] msgpack = DocumentUtil.convertToMsgPack(json);
    final String back = DocumentUtil.convertToJson(msgpack);
    final tools.jackson.databind.ObjectMapper mapper = new tools.jackson.databind.ObjectMapper();
    assertThat(mapper.readTree(back)).isEqualTo(mapper.readTree(json));
  }

  @Test
  @DisplayName("InputStream 入口等价于 String 入口")
  void inputStreamEquivalent() {
    final String json = "{\"a\":1,\"b\":[true,null]}";
    final InputStream stream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
    assertThat(DocumentUtil.convertToMsgPack(stream))
        .isEqualTo(DocumentUtil.convertToMsgPack(json));
  }

  @Test
  @DisplayName("Map 入口与 JSON 入口产出一致的字节")
  void mapEntryEqualsJsonEntry() {
    final Map<String, Object> map = new LinkedHashMap<>();
    map.put("i", 42L);
    map.put("s", "v");
    map.put("arr", List.of(1L, 2L));
    final Map<String, Object> nested = new LinkedHashMap<>();
    nested.put("x", 1.5);
    map.put("m", nested);
    assertThat(DocumentUtil.convertToMsgPack(map))
        .isEqualTo(DocumentUtil.convertToMsgPack("{\"i\":42,\"s\":\"v\",\"arr\":[1,2],\"m\":{\"x\":1.5}}"));
  }

  @Test
  @DisplayName("DirectBuffer → Map（键序保持）")
  void convertToMap() {
    final Map<String, Object> expected = new LinkedHashMap<>();
    expected.put("b", 2L);
    expected.put("a", 1L);
    expected.put("list", List.of("x", "y"));
    final byte[] msgpack = DocumentUtil.convertToMsgPack(expected);
    final Map<String, Object> map = DocumentUtil.convertToMap(new UnsafeBuffer(msgpack));
    assertThat(map).isEqualTo(expected);
  }

  @Test
  @DisplayName("convertToMap 拒绝非 map 形态")
  void convertToMapRejectsNonMap() {
    final byte[] msgpack = DocumentUtil.convertToMsgPack("[1,2]");
    assertThatThrownBy(() -> DocumentUtil.convertToMap(new UnsafeBuffer(msgpack)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Expected msgpack map");
  }

  @Test
  @DisplayName("msgpack bin 形态解出字节数组")
  void binaryPayload() {
    // msgpack bin8: 0xC4 + len + 3 字节
    final byte[] bytes = {(byte) 0xC4, 3, 1, 2, 3};
    final Object value = DocumentUtil.convertToMsgPack(new UnsafeBuffer(bytes));
    assertThat(value).isEqualTo(new byte[] {1, 2, 3});
  }

  @Test
  @DisplayName("msgpack nil 形态解出 null")
  void nilPayload() {
    final byte[] bytes = {(byte) 0xC0};
    assertThat(DocumentUtil.convertToMsgPack(new UnsafeBuffer(bytes))).isNull();
  }

  @Test
  @DisplayName("msgpack 字节绑定到 Java record")
  void convertToObjectBinding() {
    record Person(String name, int age) {}
    final byte[] msgpack = DocumentUtil.convertToMsgPack("{\"name\":\"张三\",\"age\":30}");
    final Person person = DocumentUtil.convertToObject(new UnsafeBuffer(msgpack), Person.class);
    assertThat(person.name()).isEqualTo("张三");
    assertThat(person.age()).isEqualTo(30);
  }

  @Test
  @DisplayName("非法 JSON 抛 IllegalArgumentException")
  void invalidJson() {
    assertThatThrownBy(() -> DocumentUtil.convertToMsgPack("{invalid"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Could not deserialize JSON");
  }

  @Test
  @DisplayName("非法 msgpack 抛 IllegalArgumentException")
  void invalidMsgpack() {
    final byte[] bytes = {(byte) 0xC1}; // 永不合法的 msgpack 首字节
    assertThatThrownBy(() -> DocumentUtil.convertToJson(bytes))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("JsonSerializable 对象转 JSON 字符串")
  void jsonSerializable() {
    final JsonSerializable serializable = new JsonSerializable() {
      @Override
      public String toJson() {
        return "{\"k\":1}";
      }
    };
    assertThat(DocumentUtil.convertJsonSerializableObjectToJson(serializable))
        .isEqualTo("{\"k\":1}");
  }

  @Test
  @DisplayName("空文档: 空 JSON 对象与空数组")
  void emptyDocuments() {
    assertThat(DocumentUtil.convertToJson(DocumentUtil.convertToMsgPack("{}"))).isEqualTo("{}");
    assertThat(DocumentUtil.convertToJson(DocumentUtil.convertToMsgPack("[]"))).isEqualTo("[]");
  }

  @Test
  @DisplayName("Double/Float 打包为双精度并往返")
  void floatingPointPacking() {
    final Map<String, Object> map = new LinkedHashMap<>();
    map.put("f", 2.5f);
    map.put("d", Math.PI);
    final String json = DocumentUtil.convertToJson(DocumentUtil.convertToMsgPack(map));
    assertThat(json).isEqualTo("{\"f\":2.5,\"d\":3.141592653589793}");
  }

  @Test
  @DisplayName("Integer/Short 直接打包（无需归一化中转）")
  void integerSubtypes() {
    final Map<String, Object> map = new LinkedHashMap<>();
    map.put("i", 42);
    map.put("s", (short) 7);
    map.put("big", 9007199254740993L);
    final String json = DocumentUtil.convertToJson(DocumentUtil.convertToMsgPack(map));
    assertThat(json).isEqualTo("{\"i\":42,\"s\":7,\"big\":9007199254740993}");
  }

  @Test
  @DisplayName("POJO 走 JSON 中转兜底（与原实现一致）")
  void pojoFallback() {
    record Point(int x, int y) {}
    final byte[] msgpack = DocumentUtil.convertToMsgPack((Object) new Point(3, 4));
    assertThat(DocumentUtil.convertToJson(msgpack)).isEqualTo("{\"x\":3,\"y\":4}");
  }

  @Test
  @DisplayName("非字符串 map 键经 String.valueOf 转换")
  void nonStringKeyFallback() {
    final Map<Integer, String> map = new LinkedHashMap<>();
    map.put(1, "one");
    map.put(2, "two");
    final byte[] msgpack = DocumentUtil.convertToMsgPack(map);
    assertThat(DocumentUtil.convertToJson(msgpack)).isEqualTo("{\"1\":\"one\",\"2\":\"two\"}");
  }

  @Test
  @DisplayName("byte[] 兜底为 base64 字符串（保持原行为）")
  void byteArrayFallback() {
    final byte[] msgpack = DocumentUtil.convertToMsgPack((Object) new byte[] {1, 2, 3});
    assertThat(DocumentUtil.convertToJson(msgpack)).isEqualTo("\"AQID\"");
  }
}
