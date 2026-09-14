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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.anyilanxin.kunpeng.configuration.broker.rocksdb.RocksdbConfiguration;
import com.anyilanxin.kunpeng.kvstore.ColumnFamily;
import com.anyilanxin.kunpeng.kvstore.TransactionContext;
import com.anyilanxin.kunpeng.kvstore.types.CompositeKeyType;
import com.anyilanxin.kunpeng.kvstore.types.LongType;
import com.anyilanxin.kunpeng.kvstore.types.NilType;
import com.anyilanxin.kunpeng.kvstore.types.StringType;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** 基于 rocksdbjni 的 KvStore 功能测试：CRUD、事务语义、迭代与虚拟列族隔离 */
class RocksdbTransactionDbTest {

  @TempDir Path tempDir;

  private RocksdbTransactionDb<TestColumnFamilies> db;

  @BeforeEach
  void setUp() {
    db =
        new DefaultRocksdbFactory<TestColumnFamilies>()
            .createDb(tempDir.toString(), null, 0, new RocksdbConfiguration(), TestColumnFamilies.DEFAULT);
  }

  @AfterEach
  void tearDown() throws Exception {
    // 重开类用例已提前关闭实例，这里容忍重复关闭
    try {
      db.close();
    } catch (final Exception ignored) {
      // 已关闭
    }
  }

  private ColumnFamily<StringType, StringType> strings(
      final TestColumnFamilies family, final TransactionContext context) {
    return db.createColumnFamily(family, context, new StringType(), new StringType());
  }

  @Test
  void shouldPersistAndReadKeyValueAcrossTransactions() {
    final var context = db.createTransactionContext();
    final var columnFamily = strings(TestColumnFamilies.FIRST, context);

    final var key = new StringType();
    key.wrapString("persistent-key");
    final var value = new StringType();
    value.wrapString("persistent-value");

    // put 内部自动包事务并在最外层提交
    columnFamily.put(key, value);

    // 新的事务上下文中读取仍然可见
    final var reader = strings(TestColumnFamilies.FIRST, db.createTransactionContext());
    final var readKey = new StringType();
    readKey.wrapString("persistent-key");
    final var readValue = reader.get(readKey);
    assertThat(readValue).isNotNull();
    assertThat(readValue.toString()).isEqualTo("persistent-value");
    assertThat(reader.exists(readKey)).isTrue();
    assertThat(reader.isEmpty()).isFalse();
  }

  @Test
  void shouldDistinguishMissingKeyFromEmptyValue() {
    final var context = db.createTransactionContext();
    final var nilFamily =
        db.createColumnFamily(
            TestColumnFamilies.SECOND, context, new StringType(), NilType.INSTANCE);

    final var key = new StringType();
    key.wrapString("empty-value-key");
    nilFamily.put(key, NilType.INSTANCE);

    final var readValue = nilFamily.get(key);
    assertThat(readValue).isNotNull(); // 空 value 是合法值，get 不应返回 null

    final var missingKey = new StringType();
    missingKey.wrapString("no-such-key");
    assertThat(nilFamily.get(missingKey)).isNull();
    assertThat(nilFamily.exists(missingKey)).isFalse();
  }

  @Test
  void shouldDeleteKey() {
    final var columnFamily =
        db.createColumnFamily(
            TestColumnFamilies.FIRST, db.createTransactionContext(),
            new LongType(), new LongType());

    final var key = new LongType();
    key.wrapLong(100L);
    final var value = new LongType();
    value.wrapLong(200L);
    columnFamily.put(key, value);
    assertThat(columnFamily.exists(key)).isTrue();

    columnFamily.delete(key);
    assertThat(columnFamily.exists(key)).isFalse();
    assertThat(columnFamily.get(key)).isNull();
    assertThat(columnFamily.isEmpty()).isTrue();
  }

  @Test
  void shouldIterateInKeyOrder() {
    final var columnFamily =
        db.createColumnFamily(
            TestColumnFamilies.FIRST, db.createTransactionContext(),
            new LongType(), new StringType());

    for (final var entry : new long[] {5, 1, 3, 9, 7}) {
      final var key = new LongType();
      key.wrapLong(entry);
      final var value = new StringType();
      value.wrapString("v-" + entry);
      columnFamily.put(key, value);
    }

    final List<Long> observedKeys = new ArrayList<>();
    columnFamily.forEach(
        (key, value) -> {
          observedKeys.add(key.getValue());
          assertThat(value.toString()).isEqualTo("v-" + key.getValue());
        });

    assertThat(observedKeys).containsExactly(1L, 3L, 5L, 7L, 9L);
    assertThat(columnFamily.count()).isEqualTo(5);
  }

  @Test
  void shouldStopIterationWhenVisitorReturnsFalse() {
    final var columnFamily =
        db.createColumnFamily(
            TestColumnFamilies.FIRST, db.createTransactionContext(),
            new LongType(), new LongType());

    for (long i = 0; i < 10; i++) {
      final var key = new LongType();
      key.wrapLong(i);
      final var value = new LongType();
      value.wrapLong(i * 10);
      columnFamily.put(key, value);
    }

    final List<Long> visited = new ArrayList<>();
    columnFamily.whileTrue(
        (key, value) -> {
          visited.add(key.getValue());
          return key.getValue() < 3;
        });

    // 访问完 key=3（返回 false 的那条）后停止
    assertThat(visited).containsExactly(0L, 1L, 2L, 3L);
  }

  @Test
  void shouldIterateOnlyMatchingPrefix() {
    final var context = db.createTransactionContext();
    final var key = new CompositeKeyType<>(new StringType(), new LongType());
    final var value = new StringType();
    final var columnFamily =
        db.createColumnFamily(TestColumnFamilies.FIRST, context, key, value);

    // twoTenants-1 / twoTenants-2 / other-1
    putComposite(columnFamily, "tenant-A", 1);
    putComposite(columnFamily, "tenant-A", 2);
    putComposite(columnFamily, "tenant-B", 1);

    // 前缀即组合 key 的 first 段（裸 StringType，不含未初始化的 second）
    final var prefix = new StringType();
    prefix.wrapString("tenant-A");

    final List<String> observed = new ArrayList<>();
    columnFamily.whileEqualPrefix(
        prefix,
        (iterKey, iterValue) -> {
          observed.add(iterKey.getFirst() + ":" + iterKey.getSecond().getValue());
          return true;
        });

    assertThat(observed).containsExactly("tenant-A:1", "tenant-A:2");
    assertThat(columnFamily.countEqualPrefix(prefix)).isEqualTo(2);
  }

  @Test
  void shouldIterateFromGivenStartKey() {
    final var columnFamily =
        db.createColumnFamily(
            TestColumnFamilies.FIRST, db.createTransactionContext(),
            new LongType(), new LongType());

    for (long i = 0; i < 5; i++) {
      final var key = new LongType();
      key.wrapLong(i);
      final var value = new LongType();
      value.wrapLong(i);
      columnFamily.put(key, value);
    }

    final var startAt = new LongType();
    startAt.wrapLong(3);
    final List<Long> visited = new ArrayList<>();
    columnFamily.whileTrue(
        startAt,
        (key, value) -> {
          visited.add(key.getValue());
          return true;
        });

    assertThat(visited).containsExactly(3L, 4L);
  }

  @Test
  void shouldIsolateVirtualColumnFamilies() {
    final var context = db.createTransactionContext();
    final var first = strings(TestColumnFamilies.FIRST, context);
    final var second = strings(TestColumnFamilies.SECOND, context);
    final var transferable = strings(TestColumnFamilies.TRANSFERABLE, context);

    final var key = new StringType();
    key.wrapString("shared-key-name");

    final var firstValue = new StringType();
    firstValue.wrapString("in-first");
    first.put(key, firstValue);

    final var transferableValue = new StringType();
    transferableValue.wrapString("in-transferable");
    transferable.put(key, transferableValue);

    assertThat(first.get(key).toString()).isEqualTo("in-first");
    assertThat(transferable.get(key).toString()).isEqualTo("in-transferable");
    // SECOND 列族没有写过该 key
    assertThat(second.get(key)).isNull();
    assertThat(first.count()).isEqualTo(1);
    assertThat(second.count()).isZero();
    assertThat(transferable.count()).isEqualTo(1);
  }

  @Test
  void shouldCommitWhenTransactionOperationSucceeds() throws Exception {
    final var context = db.createTransactionContext();
    final var columnFamily = strings(TestColumnFamilies.FIRST, context);

    context.runInTransaction(
        () -> {
          final var key = new StringType();
          key.wrapString("committed");
          final var value = new StringType();
          value.wrapString("yes");
          columnFamily.put(key, value);
        });

    final var key = new StringType();
    key.wrapString("committed");
    assertThat(columnFamily.get(key).toString()).isEqualTo("yes");
  }

  @Test
  void shouldRollbackWhenTransactionOperationFails() throws Exception {
    final var context = db.createTransactionContext();
    final var columnFamily = strings(TestColumnFamilies.FIRST, context);

    // 事务操作抛出的异常应原样传播，同时写入被回滚
    assertThatThrownBy(
            () ->
                context.runInTransaction(
                    () -> {
                      final var key = new StringType();
                      key.wrapString("rolled-back");
                      final var value = new StringType();
                      value.wrapString("should-not-persist");
                      columnFamily.put(key, value);
                      throw new IllegalStateException("boom");
                    }))
        .isInstanceOf(IllegalStateException.class);

    final var key = new StringType();
    key.wrapString("rolled-back");
    assertThat(columnFamily.get(key)).isNull();
    assertThat(columnFamily.isEmpty()).isTrue();
  }

  @Test
  void nestedTransactionsShouldRollbackTogether() throws Exception {
    final var context = db.createTransactionContext();
    final var columnFamily = strings(TestColumnFamilies.FIRST, context);

    assertThatThrownBy(
            () ->
                context.runInTransaction(
                    () -> {
                      putString(columnFamily, "outer", "outer-value");
                      // 内层成功，但外层稍后失败，二者应一起回滚
                      context.runInTransaction(() -> putString(columnFamily, "inner", "inner-value"));
                      throw new IllegalStateException("outer failed");
                    }))
        .isInstanceOf(IllegalStateException.class);

    final var probe = new StringType();
    probe.wrapString("outer");
    assertThat(columnFamily.get(probe)).isNull();
    probe.wrapString("inner");
    assertThat(columnFamily.get(probe)).isNull();
  }

  @Test
  void shouldHideUncommittedWritesFromOtherTransactionContext() throws Exception {
    final var writerContext = db.createTransactionContext();
    final var readerContext = db.createTransactionContext();
    final var writerFamily = strings(TestColumnFamilies.FIRST, writerContext);
    final var readerFamily = strings(TestColumnFamilies.FIRST, readerContext);

    final var transaction = writerContext.getCurrentTransaction();
    transaction.run(() -> putString(writerFamily, "isolation-key", "uncommitted"));
    // 未提交前，另一个事务上下文不应看到该写入
    final var probe = new StringType();
    probe.wrapString("isolation-key");
    assertThat(readerFamily.get(probe)).isNull();

    transaction.commit();

    assertThat(readerFamily.get(probe)).isNotNull();
    assertThat(readerFamily.get(probe).toString()).isEqualTo("uncommitted");
  }

  @Test
  void shouldPersistDataToDiskAfterFlush() throws Exception {
    // 使用独立目录，避免与 @BeforeEach 实例持有的目录锁冲突。
    // 默认配置禁用 WAL，持久化依赖显式 flush（与快照/checkpoint 恢复路径一致）
    final var dbPath = tempDir.resolve("reopen-db");
    final var db1 =
        new DefaultRocksdbFactory<TestColumnFamilies>()
            .createDb(dbPath.toString(), null, 0, new RocksdbConfiguration(), TestColumnFamilies.DEFAULT);
    putString(
        strings(TestColumnFamilies.FIRST, db1.createTransactionContext()),
        "reopen-key",
        "reopen-value");
    try (final var flushOptions = new org.rocksdb.FlushOptions()) {
      flushOptions.setWaitForFlush(true);
      db1.getRocksdb().flush(flushOptions);
    }
    db1.close();

    // 以原生只读方式重开：flush 产生的 SST 中应能找到虚拟列族 FIRST(编号1) 前缀编码的 key
    try (final var rawDb =
        org.rocksdb.RocksDB.openReadOnly(
            new org.rocksdb.Options().setCreateIfMissing(false), dbPath.toString())) {
      final var expectedKey = new java.io.ByteArrayOutputStream();
      expectedKey.write(java.nio.ByteBuffer.allocate(4).putInt(1).array()); // 虚拟列族前缀
      final var keyBytes = "reopen-key".getBytes(java.nio.charset.StandardCharsets.UTF_8);
      expectedKey.write(java.nio.ByteBuffer.allocate(4).putInt(keyBytes.length).array());
      expectedKey.write(keyBytes);

      assertThat(rawDb.get(expectedKey.toByteArray())).isNotNull();
    }
  }

  private static StringType newKey(final String key) {
    final var stringKey = new StringType();
    stringKey.wrapString(key);
    return stringKey;
  }

  private static void putString(
      final ColumnFamily<StringType, StringType> columnFamily,
      final String key,
      final String value) {
    final var stringKey = new StringType();
    stringKey.wrapString(key);
    final var stringValue = new StringType();
    stringValue.wrapString(value);
    columnFamily.put(stringKey, stringValue);
  }

  private static void putComposite(
      final ColumnFamily<CompositeKeyType<StringType, LongType>, StringType> columnFamily,
      final String tenant,
      final long id) {
    final var key = new CompositeKeyType<>(new StringType(), new LongType());
    key.getFirst().wrapString(tenant);
    key.getSecond().wrapLong(id);
    final var value = new StringType();
    value.wrapString(tenant + "-" + id);
    columnFamily.put(key, value);
  }
}
