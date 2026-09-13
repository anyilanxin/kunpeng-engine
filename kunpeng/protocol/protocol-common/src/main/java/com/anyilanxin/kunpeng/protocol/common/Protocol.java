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
package com.anyilanxin.kunpeng.protocol.common;

import java.nio.ByteOrder;

public final class Protocol {

  public static final int PROTOCOL_VERSION = 6;

  /** 协议中多字节值编码所使用的字节序。必须与 SBE XML schema 中默认的字节序保持一致。 */
  public static final ByteOrder ENDIANNESS = ByteOrder.LITTLE_ENDIAN;

  /** 按约定，部署命令写入的目标分区 */
  public static final int DEPLOYMENT_PARTITION = 1;

  /** 首个分区的 id。分区 id 的范围为 (START_PARTITION_ID, START_PARTITION_ID + partitionCount) */
  public static final int START_PARTITION_ID = 1;

  /**
   * 分区空间由键空间（keyspace）和 long 的最大值推导而来。
   *
   * <p>partitionSpace = 2^64 - KEYSPACE
   */
  public static final int RESOURCE_BITS = 14;

  /** 集群中可创建的最大分区数，即 2^13，也就是 8192。 */
  public static final long MAXIMUM_RESOURCES = 1L << RESOURCE_BITS;

  /**
   * 每个分区定义一个键空间（keyspace）。要确定键空间大小，需要先计算 dispatcher 实现最多可写入的事件数。
   *
   * <p><b>如果我们变更或替换 dispatcher 实现，应检查当前定义的键空间大小是否仍然有效。</b>
   *
   * <p>计算方式如下：
   *
   * <p>每个 segment 可写入 2^32 字节，最多可有 2^32 个 segment，即可写入的总字节数为 2*32 * 2^32 =
   * 18446744073709551616。假设事件平均大小为 15_000 字节（由于变量等开销）， 可计算 dispatcher 最多可写入的事件数：`maximumEvents =
   * maximumBytes / eventAvgSize = 1229782938247303.5`。再计算达到该值所需的 2 的最小幂：log(2, 1229782938247303.5)。
   * 即需要一个 2^51 的键空间，才能保证键的数量多于可能写入的事件数。
   */
  public static final int KEY_BITS = 50;

  public static long encodeResourceId(final int resourceId, final long key) {
    return ((long) resourceId << KEY_BITS) + key;
  }

  public static int decodeResourceId(final long key) {
    return (int) (key >> KEY_BITS);
  }

  public static long decodeKey(final long key) {
    // 为了便于理解，计算过程为：key - ((long) partitionId << KEY_BITS);

    // 为了效率，实际以位运算实现
    return key & 0x0007FFFFFFFFFFFFL;
  }
}
