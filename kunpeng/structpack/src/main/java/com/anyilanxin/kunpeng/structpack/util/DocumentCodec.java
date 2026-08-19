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

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 标准 msgpack 文档编解码（自研实现, 零外部依赖）。
 *
 * <p>仅覆盖文档(流程变量)所需的动态树子集：nil/boolean/integer/float/string/binary/array/map； ext 扩展类型与保留字节 0xC1
 * 拒绝。写侧采用最小编码, 与官方 msgpack-java 默认 packer 字节完全一致——存量数据、gateway/job API 零迁移。
 *
 * <p>类型映射：null/Boolean/Long/Double/String/byte[]/List/Map（读侧 Integer 一律回读为 {@link Long}, Float
 * 一律回读为 {@link Double}, 键序用 {@link LinkedHashMap} 保持）。
 */
public final class DocumentCodec {

  private DocumentCodec() {}

  /** Object 树 → 标准 msgpack 字节（最小编码） */
  public static byte[] pack(final Object value) {
    final Writer writer = new Writer(64);
    writer.write(value);
    return writer.copy();
  }

  /** msgpack 字节 → Object 树 */
  public static Object unpack(final byte[] bytes) {
    return unpack(bytes, 0, bytes.length);
  }

  /** msgpack 字节区间 → Object 树；根值后的尾部字节忽略 */
  public static Object unpack(final byte[] bytes, final int offset, final int length) {
    if (length == 0) {
      return null;
    }
    return new Reader(bytes, offset, offset + length).readValue();
  }

  // ===== 写侧 =====

  private static final class Writer {
    private byte[] buffer;
    private int position;

    Writer(final int initialCapacity) {
      buffer = new byte[initialCapacity];
    }

    byte[] copy() {
      return Arrays.copyOf(buffer, position);
    }

    void write(final Object value) {
      switch (value) {
        case null -> code(0xC0);
        case final Boolean b -> code(b ? 0xC3 : 0xC2);
        case final String s -> writeString(s);
        case final Double d -> writeDouble(d);
        case final Float f -> writeDouble(f.doubleValue());
        case final Number n -> writeLong(n.longValue());
        case final Map<?, ?> map -> {
          writeContainerHeader(map.size(), 0x80, 0xde); // fixmap / map16 / map32

          for (final Map.Entry<?, ?> entry : map.entrySet()) {
            if (!(entry.getKey() instanceof final String key)) {
              throw new IllegalArgumentException(
                  "不支持的文档类型: map 键必须为 String, 实际 " + entry.getKey().getClass().getName());
            }
            writeString(key);
            write(entry.getValue());
          }
        }
        case final Iterable<?> iterable -> {
          int size = 0;
          for (final Object ignored : iterable) {
            size++;
          }
          writeContainerHeader(size, 0x90, 0xdc); // fixarray / array16 / array32

          for (final Object item : iterable) {
            write(item);
          }
        }
        default -> throw new IllegalArgumentException("不支持的文档类型: " + value.getClass().getName());
      }
    }

    /** fix(15)/16 位/32 位容器头（宽形态需附加长度字段） */
    void writeContainerHeader(final int size, final int fixBase, final int wide16) {
      if (size < 16) {
        code(fixBase | size);
      } else if (size < 65536) {
        code(wide16);
        w16(size);
      } else {
        code(wide16 + 1);
        w32(size);
      }
    }

    void writeString(final String value) {
      final byte[] utf8 = value.getBytes(StandardCharsets.UTF_8);
      if (utf8.length < 32) {
        code(0xa0 | utf8.length);
      } else if (utf8.length < 256) {
        code(0xd9);
        code(utf8.length);
      } else if (utf8.length < 65536) {
        code(0xda);
        w16(utf8.length);
      } else {
        code(0xdb);
        w32(utf8.length);
      }
      raw(utf8, 0, utf8.length);
    }

    void writeLong(final long value) {
      // 与官方 packer 一致: 负数走有符号族, 正数超 fixint 走无符号族
      if (value < -32) {
        if (value < Short.MIN_VALUE) {
          if (value < Integer.MIN_VALUE) {
            code(0xd3);
            w64(value); // int64
          } else {
            code(0xd2);
            w32(value); // int32
          }
        } else if (value < Byte.MIN_VALUE) {
          code(0xd1);
          w16(value); // int16
        } else {
          code(0xd0);
          code((int) value); // int8
        }
      } else if (value < 128) {
        code((int) value); // fixint
      } else if (value < 256) {
        code(0xcc);
        code((int) value); // uint8
      } else if (value < 65536) {
        code(0xcd);
        w16(value); // uint16
      } else if (value < 4294967296L) {
        code(0xce);
        w32(value); // uint32
      } else {
        code(0xcf);
        w64(value); // uint64
      }
    }

    void writeDouble(final double value) {
      code(0xcb);
      w64(Double.doubleToRawLongBits(value)); // float64
    }

    void code(final int value) {
      ensure(1);
      buffer[position++] = (byte) value;
    }

    void w16(final long value) {
      ensure(2);
      buffer[position++] = (byte) (value >>> 8);
      buffer[position++] = (byte) value;
    }

    void w32(final long value) {
      ensure(4);
      buffer[position++] = (byte) (value >>> 24);
      buffer[position++] = (byte) (value >>> 16);
      buffer[position++] = (byte) (value >>> 8);
      buffer[position++] = (byte) value;
    }

    void w64(final long value) {
      ensure(8);
      buffer[position++] = (byte) (value >>> 56);
      buffer[position++] = (byte) (value >>> 48);
      buffer[position++] = (byte) (value >>> 40);
      buffer[position++] = (byte) (value >>> 32);
      buffer[position++] = (byte) (value >>> 24);
      buffer[position++] = (byte) (value >>> 16);
      buffer[position++] = (byte) (value >>> 8);
      buffer[position++] = (byte) value;
    }

    void raw(final byte[] source, final int offset, final int length) {
      ensure(length);
      System.arraycopy(source, offset, buffer, position, length);
      position += length;
    }

    void ensure(final int extra) {
      if (position + extra > buffer.length) {
        buffer = Arrays.copyOf(buffer, Math.max(buffer.length * 2, position + extra));
      }
    }
  }

  // ===== 读侧 =====

  private static final class Reader {
    private final byte[] buffer;
    private int position;
    private final int end;

    Reader(final byte[] buffer, final int offset, final int end) {
      this.buffer = buffer;
      position = offset;
      this.end = end;
    }

    Object readValue() {
      final int code = readByte() & 0xFF;
      if (code <= 0x7f) {
        return (long) code; // positive fixint
      }
      if (code >= 0xe0) {
        return (long) (byte) code; // negative fixint
      }
      return switch (code) {
        case 0xc0 -> null; // nil
        case 0xc2 -> Boolean.FALSE;
        case 0xc3 -> Boolean.TRUE;
        case 0xcc -> (long) (readByte() & 0xFF); // uint8
        case 0xcd -> (long) readU16(); // uint16
        case 0xce -> readU32(); // uint32
        case 0xcf -> readU64(); // uint64（补码回读）
        case 0xd0 -> (long) readByte(); // int8
        case 0xd1 -> (long) readS16(); // int16
        case 0xd2 -> (long) readS32(); // int32
        case 0xd3 -> readS64(); // int64
        case 0xca -> (double) Float.intBitsToFloat(readS32()); // float32
        case 0xcb -> Double.longBitsToDouble(readS64()); // float64
        case 0xc4 -> readBinary(readU8()); // bin8
        case 0xc5 -> readBinary(readU16()); // bin16
        case 0xc6 -> readBinary(readS32()); // bin32
        case 0xd9 -> readString(readU8()); // str8
        case 0xda -> readString(readU16()); // str16
        case 0xdb -> readString(readS32()); // str32
        case 0xdc -> readArray(readU16()); // array16
        case 0xdd -> readArray(readS32()); // array32
        case 0xde -> readMap(readU16()); // map16
        case 0xdf -> readMap(readS32()); // map32
        case 0xc1 -> throw new IllegalArgumentException("非法 msgpack 字节: 0xc1 为保留值");
        default -> {
          if (code >= 0xa0 && code <= 0xbf) {
            yield readString(code & 0x1f); // fixstr
          }
          if (code >= 0x90 && code <= 0x9f) {
            yield readArray(code & 0x0f); // fixarray
          }
          if (code >= 0x80 && code <= 0x8f) {
            yield readMap(code & 0x0f); // fixmap
          }
          // 0xd4-0xd8 fixext / 0xc7-0xc9 ext
          throw new IllegalArgumentException("不支持的 msgpack 类型: 0x" + Integer.toHexString(code));
        }
      };
    }

    String readString(final int byteLength) {
      return new String(readBinary(byteLength), StandardCharsets.UTF_8);
    }

    byte[] readBinary(final int byteLength) {
      require(byteLength);
      final byte[] out = Arrays.copyOfRange(buffer, position, position + byteLength);
      position += byteLength;
      return out;
    }

    List<Object> readArray(final int size) {
      // 头部声明的大小可能来自损坏数据, 只按需扩容, 截断在逐元素读取时报错
      final List<Object> list = new ArrayList<>(Math.min(size, 16));
      for (int i = 0; i < size; i++) {
        list.add(readValue());
      }
      return list;
    }

    Map<String, Object> readMap(final int size) {
      final Map<String, Object> map = new LinkedHashMap<>(Math.min(size, 16));
      for (int i = 0; i < size; i++) {
        final Object key = readValue();
        if (!(key instanceof final String stringKey)) {
          throw new IllegalArgumentException("msgpack map 键必须为字符串, 实际: " + typeName(key));
        }
        map.put(stringKey, readValue());
      }
      return map;
    }

    int readByte() {
      require(1);
      return buffer[position++];
    }

    int readU8() {
      return readByte() & 0xFF;
    }

    int readU16() {
      require(2);
      final int value = ((buffer[position] & 0xFF) << 8) | (buffer[position + 1] & 0xFF);
      position += 2;
      return value;
    }

    int readS16() {
      return (short) readU16();
    }

    int readS32() {
      require(4);
      int value = 0;
      for (int i = 0; i < 4; i++) {
        value = (value << 8) | (buffer[position + i] & 0xFF);
      }
      position += 4;
      return value;
    }

    long readU32() {
      return readS32() & 0xFFFFFFFFL;
    }

    long readS64() {
      require(8);
      long value = 0;
      for (int i = 0; i < 8; i++) {
        value = (value << 8) | (buffer[position + i] & 0xFF);
      }
      position += 8;
      return value;
    }

    long readU64() {
      return readS64();
    }

    void require(final int needed) {
      if (position + needed > end) {
        throw new IllegalArgumentException(
            "msgpack 数据截断: 需要 " + (position + needed - end) + " 字节, 位置 " + position);
      }
    }

    static String typeName(final Object value) {
      return value == null ? "null" : value.getClass().getName();
    }
  }
}
