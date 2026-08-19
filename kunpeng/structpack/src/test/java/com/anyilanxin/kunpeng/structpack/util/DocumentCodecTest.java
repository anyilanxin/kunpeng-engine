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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** DocumentCodec 测试：字节级 msgpack 规范（与官方 msgpack-java 默认 packer 字节一致） */
@DisplayName("DocumentCodec 自研 msgpack 编解码")
class DocumentCodecTest {

  /** "cc 80" → [0xCC, 0x80] */
  static byte[] bytes(final String hex) {
    final String clean = hex.replaceAll("\\s+", "");
    final byte[] out = new byte[clean.length() / 2];
    for (int i = 0; i < out.length; i++) {
      out[i] = (byte) Integer.parseInt(clean.substring(i * 2, i * 2 + 2), 16);
    }
    return out;
  }

  @Test
  @DisplayName("标量编码字节精确: nil/true/false")
  void scalars() {
    assertThat(DocumentCodec.pack(null)).isEqualTo(bytes("c0"));
    assertThat(DocumentCodec.pack(true)).isEqualTo(bytes("c3"));
    assertThat(DocumentCodec.pack(false)).isEqualTo(bytes("c2"));
  }

  @Test
  @DisplayName("整数编码字节精确: fixint/无符号/有符号全宽度")
  void integers() {
    assertThat(DocumentCodec.pack(0L)).isEqualTo(bytes("00"));
    assertThat(DocumentCodec.pack(127L)).isEqualTo(bytes("7f"));
    assertThat(DocumentCodec.pack(128L)).isEqualTo(bytes("cc 80"));
    assertThat(DocumentCodec.pack(255L)).isEqualTo(bytes("cc ff"));
    assertThat(DocumentCodec.pack(256L)).isEqualTo(bytes("cd 01 00"));
    assertThat(DocumentCodec.pack(65535L)).isEqualTo(bytes("cd ff ff"));
    assertThat(DocumentCodec.pack(65536L)).isEqualTo(bytes("ce 00 01 00 00"));
    assertThat(DocumentCodec.pack(4294967295L)).isEqualTo(bytes("ce ff ff ff ff"));
    assertThat(DocumentCodec.pack(4294967296L))
        .isEqualTo(bytes("cf 00 00 00 01 00 00 00 00"));
    assertThat(DocumentCodec.pack(-1L)).isEqualTo(bytes("ff"));
    assertThat(DocumentCodec.pack(-32L)).isEqualTo(bytes("e0"));
    assertThat(DocumentCodec.pack(-33L)).isEqualTo(bytes("d0 df"));
    assertThat(DocumentCodec.pack(-128L)).isEqualTo(bytes("d0 80"));
    assertThat(DocumentCodec.pack(-129L)).isEqualTo(bytes("d1 ff 7f"));
    assertThat(DocumentCodec.pack(-32768L)).isEqualTo(bytes("d1 80 00"));
    assertThat(DocumentCodec.pack(-32769L)).isEqualTo(bytes("d2 ff ff 7f ff"));
    assertThat(DocumentCodec.pack(-2147483648L)).isEqualTo(bytes("d2 80 00 00 00"));
    assertThat(DocumentCodec.pack(Long.MIN_VALUE))
        .isEqualTo(bytes("d3 80 00 00 00 00 00 00 00"));
    assertThat(DocumentCodec.pack(Long.MAX_VALUE))
        .isEqualTo(bytes("cf 7f ff ff ff ff ff ff ff"));
  }

  @Test
  @DisplayName("字符串编码字节精确: fixstr/str8/str16")
  void strings() {
    assertThat(DocumentCodec.pack("")).isEqualTo(bytes("a0"));
    assertThat(DocumentCodec.pack("a")).isEqualTo(bytes("a1 61"));
    assertThat(DocumentCodec.pack("中文")).isEqualTo(bytes("a6 e4 b8 ad e6 96 87"));
    assertThat(DocumentCodec.pack("x".repeat(31))).isEqualTo(bytes("bf" + "78".repeat(31)));
    assertThat(DocumentCodec.pack("x".repeat(32))).isEqualTo(bytes("d9 20" + "78".repeat(32)));
    assertThat(DocumentCodec.pack("x".repeat(255))).isEqualTo(bytes("d9 ff" + "78".repeat(255)));
    assertThat(DocumentCodec.pack("x".repeat(256)))
        .isEqualTo(bytes("da 01 00" + "78".repeat(256)));
  }

  @Test
  @DisplayName("数组/映射编码字节精确: fixarray/16 位、fixmap/16 位")
  void containers() {
    assertThat(DocumentCodec.pack(List.of())).isEqualTo(bytes("90"));
    assertThat(DocumentCodec.pack(java.util.Collections.nCopies(15, 1L)))
        .isEqualTo(bytes("9f" + "01".repeat(15)));
    assertThat(DocumentCodec.pack(java.util.Collections.nCopies(16, 1L)))
        .isEqualTo(bytes("dc 00 10" + "01".repeat(16)));

    assertThat(DocumentCodec.pack(Map.of())).isEqualTo(bytes("80"));
    final Map<String, Object> map15 = new LinkedHashMap<>();
    for (int i = 0; i < 15; i++) {
      map15.put("k" + i, 1L);
    }
    // fixmap(1) + "k0".."k9" 各 4 字节 + "k10".."k14" 各 5 字节
    assertThat(DocumentCodec.pack(map15)).hasSize(1 + 10 * 4 + 5 * 5);
    assertThat(DocumentCodec.pack(map15)[0]).isEqualTo((byte) 0x8f);
    final Map<String, Object> map16 = new LinkedHashMap<>(map15);
    map16.put("k15", 1L);
    assertThat(DocumentCodec.pack(map16)[0]).isEqualTo((byte) 0xde);
  }

  @Test
  @DisplayName("浮点编码字节精确: 双精度 0.5")
  void floating() {
    assertThat(DocumentCodec.pack(0.5)).isEqualTo(bytes("cb 3f e0 00 00 00 00 00 00"));
    assertThat(DocumentCodec.pack(2.5f)).isEqualTo(bytes("cb 40 04 00 00 00 00 00 00"));
  }

  @Test
  @DisplayName("读侧全整数宽度: uint8-64/int8-64/fixint")
  void unpackIntegerWidths() {
    assertThat(DocumentCodec.unpack(bytes("7f"))).isEqualTo(127L);
    assertThat(DocumentCodec.unpack(bytes("e0"))).isEqualTo(-32L);
    assertThat(DocumentCodec.unpack(bytes("cc 80"))).isEqualTo(128L);
    assertThat(DocumentCodec.unpack(bytes("cd 01 00"))).isEqualTo(256L);
    assertThat(DocumentCodec.unpack(bytes("ce 00 01 00 00"))).isEqualTo(65536L);
    assertThat(DocumentCodec.unpack(bytes("cf 00 00 00 01 00 00 00 00")))
        .isEqualTo(4294967296L);
    assertThat(DocumentCodec.unpack(bytes("d0 df"))).isEqualTo(-33L);
    assertThat(DocumentCodec.unpack(bytes("d1 ff 7f"))).isEqualTo(-129L);
    assertThat(DocumentCodec.unpack(bytes("d2 ff ff 7f ff"))).isEqualTo(-32769L);
    assertThat(DocumentCodec.unpack(bytes("d3 80 00 00 00 00 00 00 00")))
        .isEqualTo(Long.MIN_VALUE);
    // uint64 最高位为 1 时按补码回读为负 long（与官方 unpackLong 一致）
    assertThat(DocumentCodec.unpack(bytes("cf ff ff ff ff ff ff ff ff"))).isEqualTo(-1L);
  }

  @Test
  @DisplayName("读侧 float32 与 bin 家族")
  void unpackFloatAndBinary() {
    assertThat((Double) DocumentCodec.unpack(bytes("ca 3f 80 00 00"))).isEqualTo(1.0);
    assertThat(DocumentCodec.unpack(bytes("ca bf 80 00 00"))).isEqualTo(-1.0);
    assertThat(DocumentCodec.unpack(bytes("c4 03 01 02 03"))).isEqualTo(new byte[] {1, 2, 3});
    assertThat(DocumentCodec.unpack(bytes("c5 00 03 04 05 06")))
        .isEqualTo(new byte[] {4, 5, 6});
  }

  @Test
  @DisplayName("嵌套树往返: 中文/emoji/深层嵌套/空容器")
  void roundTripTree() {
    final Map<String, Object> deep = new LinkedHashMap<>();
    deep.put("s", "中文🚀");
    deep.put("i", 42L);
    deep.put("big", 9007199254740993L);
    deep.put("d", 1.5);
    deep.put("b", Boolean.TRUE);
    deep.put("n", null);
    deep.put("arr", java.util.Arrays.asList(1L, "two", Boolean.FALSE, null));
    final Map<String, Object> nested = new LinkedHashMap<>();
    nested.put("x", 1.25);
    deep.put("nested", nested);
    deep.put("emptyMap", new LinkedHashMap<>());
    deep.put("emptyArr", List.of());

    assertThat(DocumentCodec.unpack(DocumentCodec.pack(deep))).isEqualTo(deep);
  }

  @Test
  @DisplayName("任意位置截断一律拒绝")
  void truncatedRejected() {
    final byte[] full = DocumentCodec.pack(Map.of("k", List.of(1L, 2.5, "v")));
    for (int len = 1; len < full.length; len++) {
      final int cut = len;
      assertThatThrownBy(() -> DocumentCodec.unpack(full, 0, cut))
          .as("截断至 %d 字节应失败", cut)
          .isInstanceOf(IllegalArgumentException.class);
    }
  }

  @Test
  @DisplayName("非法首字节/扩展类型/非字符串键拒绝")
  void invalidRejected() {
    assertThatThrownBy(() -> DocumentCodec.unpack(bytes("c1")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("非法");
    assertThatThrownBy(() -> DocumentCodec.unpack(bytes("d4 01 7f")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("不支持的 msgpack 类型");
    // fixmap1 + bin8 键(非字符串) → 拒绝
    assertThatThrownBy(() -> DocumentCodec.unpack(bytes("81 c4 01 61 c0")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("键");
  }

  @Test
  @DisplayName("根值后的尾部字节被忽略（与官方行为一致）")
  void trailingIgnored() {
    assertThat(DocumentCodec.unpack(bytes("01 ff ff"))).isEqualTo(1L);
  }

  @Test
  @DisplayName("不支持的非树类型打包直接拒绝")
  void packUnknownTypeRejected() {
    assertThatThrownBy(() -> DocumentCodec.pack(new Object()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("不支持的文档类型");
  }
}
