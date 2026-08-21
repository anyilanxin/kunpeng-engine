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
package com.anyilanxin.kunpeng.cluster.raft.journal.util;

import java.nio.ByteBuffer;
import java.util.zip.CRC32C;
import org.agrona.DirectBuffer;

/**
 * CRC32C 校验和计算器。
 *
 * <p>内部复用同一个 {@link CRC32C} 实例，每次计算前重置状态，避免频繁分配。此类非线程
 * 安全，各使用点应持有独立实例。
 */
public final class ChecksumGenerator {

  private final CRC32C crc = new CRC32C();

  /**
   * 计算 {@link DirectBuffer} 中指定区间的校验和。
   *
   * <p>按缓冲底层存储类型（堆数组或 ByteBuffer）分派到对应重载。
   *
   * @param buffer 数据缓冲
   * @param offset 区间起始偏移
   * @param length 区间字节长度
   * @return CRC32C 校验和
   */
  public long compute(final DirectBuffer buffer, final int offset, final int length) {
    final byte[] heap = buffer.byteArray();
    if (heap != null) {
      return compute(heap, offset, length);
    }
    final ByteBuffer wrapped = buffer.byteBuffer();
    if (wrapped != null) {
      // compute(ByteBuffer, ...) 内部本就会对缓冲做切分复制
      return compute(wrapped, offset, length);
    }
    throw new IllegalStateException(
        "Provided DirectBuffer does not have either a byteArray or a byteBuffer");
  }

  /**
   * 计算 {@link ByteBuffer} 中指定区间的校验和。
   *
   * @param buffer 数据缓冲
   * @param offset 区间起始偏移
   * @param length 区间字节长度
   * @return CRC32C 校验和
   */
  public long compute(final ByteBuffer buffer, final int offset, final int length) {
    final var region = buffer.asReadOnlyBuffer().position(offset).slice().limit(length);
    crc.reset();
    crc.update(region);
    return crc.getValue();
  }

  /**
   * 计算堆数组中指定区间的校验和。
   *
   * @param bytes 数据数组
   * @param offset 区间起始偏移
   * @param length 区间字节长度
   * @return CRC32C 校验和
   */
  public long compute(final byte[] bytes, final int offset, final int length) {
    crc.reset();
    crc.update(bytes, offset, length);
    return crc.getValue();
  }
}
