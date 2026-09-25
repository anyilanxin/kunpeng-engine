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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.rocksdb;

import static org.assertj.core.api.Assertions.assertThat;

import com.anyilanxin.kunpeng.configuration.broker.rocksdb.RocksdbConfiguration;
import com.anyilanxin.kunpeng.kvstore.ColumnCopyType;
import com.anyilanxin.kunpeng.kvstore.ColumnFamily;
import com.anyilanxin.kunpeng.kvstore.types.StringType;
import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * 快照、复制与合并功能测试：对应集群变更时的数据迁移路径。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
class RocksdbSnapshotAndCopyTest {

  @TempDir Path tempDir;

  private RocksdbTransactionDb<TestColumnFamilies> db;

  @BeforeEach
  void setUp() {
    db =
        new DefaultRocksdbFactory<TestColumnFamilies>()
            .createDb(
                tempDir.resolve("source").toString(),
                null,
                0,
                new RocksdbConfiguration(),
                TestColumnFamilies.DEFAULT);
  }

  @AfterEach
  void tearDown() throws Exception {
    try {
      db.close();
    } catch (final Exception ignored) {
      // 已关闭
    }
  }

  private static void put(
      final ColumnFamily<StringType, StringType> columnFamily, final String key, final String value) {
    final var stringKey = new StringType();
    stringKey.wrapString(key);
    final var stringValue = new StringType();
    stringValue.wrapString(value);
    columnFamily.put(stringKey, stringValue);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.ValueSource(strings = {"short", "a-much-longer-key-for-bloom-path"})
  void shouldCreateSnapshotWithChecksums(final String keySeed) throws Exception {
    put(
        db.createColumnFamily(
            TestColumnFamilies.FIRST, db.createTransactionContext(),
            new StringType(), new StringType()),
        "key-" + keySeed,
        "value-" + keySeed);

    final var snapshotDir = tempDir.resolve("snapshot");
    db.createSnapshot(snapshotDir.toFile());

    // 快照目录可以用只读方式打开并产出校验和（供集群迁移校验）
    final var readOnlyDb =
        new DefaultRocksdbFactory<TestColumnFamilies>()
            .createReadOnlyDb(snapshotDir.toString(), TestColumnFamilies.DEFAULT);
    try {
      final var checksums = readOnlyDb.getChecksums();
      assertThat(checksums).isNotEmpty();
      assertThat(db.getChecksums().keySet()).containsAll(checksums.keySet());
    } finally {
      readOnlyDb.close();
    }
  }

  @Test
  void shouldCopyOnlyRequestedVirtualColumnFamilies() throws Exception {
    final var context = db.createTransactionContext();
    put(
        db.createColumnFamily(TestColumnFamilies.FIRST, context, new StringType(), new StringType()),
        "first-key",
        "first-value");
    // 只有映射到可迁移(local)列族的虚拟列族允许 copy/merge
    put(
        db.createColumnFamily(
            TestColumnFamilies.TRANSFERABLE, context, new StringType(), new StringType()),
        "transferable-key",
        "transferable-value");
    db.close();

    // 仅复制 TRANSFERABLE 虚拟列族
    final var copyPath = tempDir.resolve("copy");
    final var copier =
        new DefaultRocksdbFactory<TestColumnFamilies>()
            .createDb(
                tempDir.resolve("copier-workspace").toString(),
                null,
                0,
                new RocksdbConfiguration(),
                TestColumnFamilies.DEFAULT);
    try {
      copier.createCopy(
          tempDir.resolve("source"),
          copyPath,
          Set.of(TestColumnFamilies.TRANSFERABLE),
          ColumnCopyType.VIRTUAL_FAMILY);
    } finally {
      copier.close();
    }

    // 打开副本，验证 TRANSFERABLE 数据在、不可迁移的 FIRST 数据不在
    final var copyDb =
        new DefaultRocksdbFactory<TestColumnFamilies>()
            .createDb(copyPath.toString(), null, 0, new RocksdbConfiguration(), TestColumnFamilies.DEFAULT);
    try {
      final var transferable =
          copyDb.createColumnFamily(
              TestColumnFamilies.TRANSFERABLE, copyDb.createTransactionContext(),
              new StringType(), new StringType());
      final var first =
          copyDb.createColumnFamily(
              TestColumnFamilies.FIRST, copyDb.createTransactionContext(),
              new StringType(), new StringType());

      assertThat(transferable.count()).isEqualTo(1);
      assertThat(first.count()).isZero();

      final var probe = new StringType();
      probe.wrapString("transferable-key");
      assertThat(transferable.get(probe)).isNotNull();
      assertThat(transferable.get(probe).toString()).isEqualTo("transferable-value");
    } finally {
      copyDb.close();
    }
  }

  @Test
  void shouldMergeDataFromClosedDb() throws Exception {
    // 源库写入后关闭
    final var sourcePath = tempDir.resolve("merge-source");
    final var sourceDb =
        new DefaultRocksdbFactory<TestColumnFamilies>()
            .createDb(sourcePath.toString(), null, 0, new RocksdbConfiguration(), TestColumnFamilies.DEFAULT);
    put(
        sourceDb.createColumnFamily(
            TestColumnFamilies.TRANSFERABLE, sourceDb.createTransactionContext(),
            new StringType(), new StringType()),
        "merged-key",
        "merged-value");
    sourceDb.close();

    // 目标库（当前实例）合并源库数据
    final var transferable =
        db.createColumnFamily(
            TestColumnFamilies.TRANSFERABLE, db.createTransactionContext(),
            new StringType(), new StringType());
    put(transferable, "local-key", "local-value");
    db.merge(sourcePath, Set.of(TestColumnFamilies.TRANSFERABLE), ColumnCopyType.VIRTUAL_FAMILY);

    assertThat(transferable.count()).isEqualTo(2);

    final var probe = new StringType();
    probe.wrapString("merged-key");
    assertThat(transferable.get(probe)).isNotNull();
    assertThat(transferable.get(probe).toString()).isEqualTo("merged-value");
    probe.wrapString("local-key");
    assertThat(transferable.get(probe).toString()).isEqualTo("local-value");
  }
}
