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

import java.util.zip.CRC32C;
import org.agrona.DirectBuffer;
import org.agrona.checksum.Crc32c;

/**
 * CRC32C 校验和计算器。
 *
 * <p>堆外缓冲（mmap/直接内存）经 agrona {@link Crc32c} 以内存地址直调 JDK 的 updateDirectByteBuffer 内建函数，
 * 单次计算零对象分配；堆数组走 JDK 的数组内建函数重载。两条路径同为标准 CRC-32C，校验值一致，落盘格式不受 影响。此类非线程安全，各使用点应持有独立实例。 使用 agrona
 * {@link Crc32c} 的前提是 JVM 带 {@code --add-opens=java.base/java.util.zip=ALL-UNNAMED}（agrona 反射调用 JDK
 * 私有方法），构建注入与发行版启动脚本已统一携带。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public final class ChecksumGenerator {

  /** 地址式 CRC-32C 计算器（无状态），用于堆外缓冲。 */
  private static final Crc32c ADDRESS_CRC = Crc32c.INSTANCE;

  private final CRC32C crc = new CRC32C();

  /**
   * 计算 {@link DirectBuffer} 中指定区间的校验和。
   *
   * <p>按缓冲底层存储类型分派：堆数组走数组重载，堆外地址直调内建函数。两条路径下 offset 均相对缓冲自身起点。
   *
   * @param buffer 数据缓冲
   * @param offset 区间起始偏移（相对缓冲起点）
   * @param length 区间字节长度
   * @return CRC32C 校验和
   */
  public long compute(final DirectBuffer buffer, final int offset, final int length) {
    final byte[] heap = buffer.byteArray();
    if (heap != null) {
      return compute(heap, offset, length);
    }
    return ADDRESS_CRC.compute(buffer.addressOffset(), offset, length) & 0xFFFF_FFFFL;
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
