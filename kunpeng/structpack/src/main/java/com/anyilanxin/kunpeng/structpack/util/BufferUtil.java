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

import com.anyilanxin.kunpeng.structpack.PackerReader;
import com.anyilanxin.kunpeng.structpack.UnpackedObject;
import com.anyilanxin.kunpeng.structpack.buffer.BufferWriter;
import com.anyilanxin.kunpeng.structpack.property.BaseProperty;
import java.nio.charset.StandardCharsets;
import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;

/** buffer 便利工具（UTF-8 语义） */
public final class BufferUtil {

  private BufferUtil() {}

  /** 零拷贝包装字符串（UTF-8 字节） */
  public static DirectBuffer wrapString(final String string) {
    return string == null
        ? new UnsafeBuffer(0, 0)
        : new UnsafeBuffer(string.getBytes(StandardCharsets.UTF_8));
  }

  /** buffer → String（此时才发生 UTF-8 解码分配） */
  public static String bufferAsString(final DirectBuffer buffer) {
    return buffer.getStringWithoutLengthUtf8(0, buffer.capacity());
  }

  /** buffer → byte[] 拷贝 */
  public static byte[] bufferAsArray(final DirectBuffer buffer) {
    final byte[] bytes = new byte[buffer.capacity()];
    buffer.getBytes(0, bytes);
    return bytes;
  }

  /** 可写对象 → byte[] 编码拷贝 */
  public static byte[] bufferAsArray(final BufferWriter writer) {
    final var bytes = new byte[writer.getLength()];
    final var writeBuffer = new UnsafeBuffer();
    writeBuffer.wrap(bytes);
    writer.write(writeBuffer, 0);
    return bytes;
  }

  public static MutableDirectBuffer wrapArray(final byte[] array) {
    return new UnsafeBuffer(array);
  }

  public static boolean equals(final DirectBuffer buffer1, final DirectBuffer buffer2) {
    return buffer1.equals(buffer2);
  }

  public static boolean contentsEqual(final DirectBuffer buffer1, final DirectBuffer buffer2) {
    return buffer1.compareTo(buffer2) == 0;
  }

  public static DirectBuffer cloneBuffer(final DirectBuffer src) {
    final byte[] copy = bufferAsArray(src);
    return new UnsafeBuffer(copy);
  }

  public static String bufferAsHexString(final BufferWriter writer) {
    return bufferAsHexString(writer, DEFAULT_WRAP);
  }

  public static DirectBuffer createCopy(final BufferWriter writer) {
    final var buffer = new UnsafeBuffer(new byte[writer.getLength()]);
    writer.write(buffer, 0);
    return buffer;
  }

  public static String bufferAsHexString(final BufferWriter writer, final int wrap) {
    final UnsafeBuffer buffer = new UnsafeBuffer(new byte[writer.getLength()]);
    writer.write(buffer, 0);
    return bufferAsHexString(buffer);
  }

  public static String bufferAsHexString(final DirectBuffer buffer) {
    return bufferAsHexString(buffer, DEFAULT_WRAP);
  }

  public static String bufferAsHexString(final DirectBuffer buffer, final int wrap) {
    final StringBuilder builder = new StringBuilder();
    for (int i = 0; i < buffer.capacity(); i++) {
      builder.append(String.format("%02x", buffer.getByte(i)));
      if (i % wrap == wrap - 1 && i < buffer.capacity() - 1) {
        builder.append('\n');
      }
    }
    return builder.toString();
  }

  public static final int DEFAULT_WRAP = 16;

  /** content 是否以 prefix 指定区间开头（RocksDB 前缀扫描用） */
  public static boolean startsWith(
      final byte[] prefix,
      final int prefixOffset,
      final int prefixLength,
      final byte[] content,
      int contentOffset,
      final int contentLength) {
    if (contentLength < prefixLength) {
      return false;
    }
    for (int i = prefixOffset; i < prefixOffset + prefixLength; i++, contentOffset++) {
      if (content[contentOffset] != prefix[i]) {
        return false;
      }
    }
    return true;
  }

  /**
   * 将 source 编码后立即解码进 target 持有的实例（桥接拷贝，值语义），source 为 null 时清空 target。
   *
   * <p>嵌套对象属性（{@code ObjectProperty}）子对象实例复用且底层为 DirectBuffer 视图，若直接传递 引用会与源对象共享
   * buffer，源对象复用重写后已赋值数据会被静默污染，必须走本方法的编码桥接。
   */
  public static void copyInto(final UnpackedObject source, final BaseProperty<?> target) {
    if (source == null) {
      target.reset();
      return;
    }
    final var reader = new PackerReader();
    final var valueBuffer = new UnsafeBuffer(new byte[source.getLength()]);
    source.write(valueBuffer, 0);
    target.read(reader.wrap(valueBuffer, 0, valueBuffer.capacity()));
  }

  /**
   * 同类型对象赋值：把 source 的内容赋值给 target（值语义），返回 target 便于链式使用；source 为 null 时清空 target。
   *
   * <p>与 {@link #copyInto(UnpackedObject, BaseProperty)} 同理必须走编码桥接：嵌套对象属性子对象 实例复用且底层为 DirectBuffer
   * 视图，直接传引用会与 source 共享 buffer，source 复用重写后 target 已赋值数据会被静默污染。两个参数经泛型约束为同一类型。
   */
  public static <T extends UnpackedObject> void copyInto(final T source, final T target) {
    if (source == null) {
      target.reset();
      return;
    }
    if (!source.getClass().equals(target.getClass())) {
      throw new RuntimeException("类型不一致，无法复制");
    }
    final var valueBuffer = new UnsafeBuffer(new byte[source.getLength()]);
    source.write(valueBuffer, 0);
    target.wrap(valueBuffer, 0, valueBuffer.capacity());
  }

  /**
   * 将 source 编码为独立的 byte[]（structpack 帧）。
   *
   * <p>返回的是新分配数组，与 source 无共享，可安全跨线程 / 持久化传递；与 {@link #fromBytes} 配对使用。
   */
  public static <T extends UnpackedObject> byte[] toBytes(final T source) {
    return bufferAsArray(source);
  }

  /**
   * 将 byte[] 解码并填充进调用方提供的 target 实例（值语义复用），返回该实例便于链式使用。
   *
   * @param data 由 {@link #toBytes} 产出的完整 structpack 帧
   * @param target 具体的 {@link UnpackedObject} 实例，解码结果写入其中
   */
  public static <T extends UnpackedObject> T fromBytes(final byte[] data, final T target) {
    target.wrap(new UnsafeBuffer(data));
    return target;
  }
}
