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
package com.anyilanxin.kunpeng.rocksdb;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.anyilanxin.kunpeng.configuration.broker.rocksdb.RocksdbConfiguration;
import com.anyilanxin.kunpeng.kvstore.ColumnCopyType;
import com.anyilanxin.kunpeng.kvstore.ColumnFamily;
import com.anyilanxin.kunpeng.kvstore.types.StringType;
import com.anyilanxin.kunpeng.rocksdb.util.RocksdbUtil;
import java.nio.ByteOrder;
import java.nio.file.Path;
import java.util.Set;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * 拆分载荷写出器测试：写出的草稿库与合并镜像同构——close 后可被 merge 按虚拟列族摄入目标库（数据手术接收端路径）。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
class SplitPayloadWriterTest {
  @TempDir Path tempDir;

  @Test
  void shouldWritePayloadIngestibleByMerge() throws Exception {
    final Path payloadDir = tempDir.resolve("payload");
    try (final SplitPayloadWriter<TestColumnFamilies> writer =
        new SplitPayloadWriter<>(payloadDir, TestColumnFamilies.TRANSFERABLE)) {
      writer.write(prefixedKey("r1-key-a"), typedBytes("value-a"));
      writer.write(prefixedKey("r1-key-b"), typedBytes("value-b"));
    }

    try (final RocksdbTransactionDb<TestColumnFamilies> target =
        new DefaultRocksdbFactory<TestColumnFamilies>()
            .createDb(
                tempDir.resolve("target").toString(),
                null,
                0,
                new RocksdbConfiguration(),
                TestColumnFamilies.DEFAULT)) {
      RocksdbUtil.merge(
          payloadDir,
          target,
          Set.of(TestColumnFamilies.TRANSFERABLE),
          ColumnCopyType.VIRTUAL_FAMILY);

      final ColumnFamily<StringType, StringType> family =
          target.createColumnFamily(
              TestColumnFamilies.TRANSFERABLE,
              target.createTransactionContext(),
              new StringType(),
              new StringType());
      assertEquals("value-a", get(family, "r1-key-a"));
      assertEquals("value-b", get(family, "r1-key-b"));
      assertNull(get(family, "r1-key-absent"));
    }
  }

  /** 与拆分访问器 writeKey/StringType 序列化同构的物理 key：4 字节虚拟列族前缀 + 4 字节长度 + key 本体 */
  private static byte[] prefixedKey(final String key) {
    final byte[] keyBytes = key.getBytes(UTF_8);
    final byte[] bytes = new byte[2 * Integer.BYTES + keyBytes.length];
    final MutableDirectBuffer buffer = new UnsafeBuffer(bytes);
    buffer.putInt(0, TestColumnFamilies.TRANSFERABLE.virtualFamily(), ByteOrder.BIG_ENDIAN);
    buffer.putInt(Integer.BYTES, keyBytes.length, ByteOrder.BIG_ENDIAN);
    buffer.putBytes(2 * Integer.BYTES, keyBytes);
    return bytes;
  }

  /** 与 writeValue/StringType 序列化同构的物理值：4 字节长度 + 值本体 */
  private static byte[] typedBytes(final String value) {
    final byte[] valueBytes = value.getBytes(UTF_8);
    final byte[] bytes = new byte[Integer.BYTES + valueBytes.length];
    final MutableDirectBuffer buffer = new UnsafeBuffer(bytes);
    buffer.putInt(0, valueBytes.length, ByteOrder.BIG_ENDIAN);
    buffer.putBytes(Integer.BYTES, valueBytes);
    return bytes;
  }

  private static String get(
      final ColumnFamily<StringType, StringType> family, final String key) {
    final var stringKey = new StringType();
    stringKey.wrapString(key);
    final StringType value = family.get(stringKey);
    return value == null ? null : value.toString();
  }
}
