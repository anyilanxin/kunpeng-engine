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
package com.anyilanxin.kunpeng.repository.business;

import static com.anyilanxin.kunpeng.protocol.common.Protocol.decodeResourceId;

import com.anyilanxin.kunpeng.kvstore.ColumnFamilies;
import com.anyilanxin.kunpeng.kvstore.types.StoreKey;
import com.anyilanxin.kunpeng.kvstore.types.StoreValue;
import java.nio.ByteOrder;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;

/**
 * RocksDB 数据分片接口：直接操作字节缓冲的分片扩展。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public interface RockResourceDataSplit extends ResourceDataSplit {

  default boolean check(final long key, final int resourceId) {
    return decodeResourceId(key) == resourceId;
  }

  default byte[] writeKey(final StoreKey key, final ColumnFamilies columnFamilies) {
    final byte[] bytes = new byte[key.getLength() + Integer.BYTES];
    final MutableDirectBuffer directBuffer = new UnsafeBuffer(bytes);
    directBuffer.putInt(0, columnFamilies.virtualFamily(), ByteOrder.BIG_ENDIAN);
    key.write(directBuffer, Integer.BYTES);
    return bytes;
  }

  default byte[] writeValue(final StoreValue value) {
    final byte[] bytes = new byte[value.getLength()];
    final MutableDirectBuffer directBuffer = new UnsafeBuffer(bytes);
    value.write(directBuffer, 0);
    return bytes;
  }
}
