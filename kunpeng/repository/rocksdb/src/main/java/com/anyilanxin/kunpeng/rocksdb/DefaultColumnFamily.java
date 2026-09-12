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

import static com.anyilanxin.kunpeng.structpack.util.BufferUtil.startsWith;

import com.anyilanxin.kunpeng.kvstore.ColumnFamilies;
import com.anyilanxin.kunpeng.kvstore.ColumnFamily;
import com.anyilanxin.kunpeng.kvstore.KeyValuePairVisitor;
import com.anyilanxin.kunpeng.kvstore.TransactionContext;
import com.anyilanxin.kunpeng.kvstore.types.KeyType;
import com.anyilanxin.kunpeng.kvstore.types.NullKeyType;
import com.anyilanxin.kunpeng.kvstore.types.ValueType;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayDeque;
import java.util.Objects;
import java.util.Queue;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.ObjIntConsumer;
import org.agrona.DirectBuffer;
import org.agrona.ExpandableArrayBuffer;
import org.agrona.LangUtil;
import org.agrona.concurrent.UnsafeBuffer;
import org.rocksdb.ColumnFamilyHandle;
import org.rocksdb.ReadOptions;
import org.rocksdb.RocksIterator;
import org.rocksdb.Transaction;

/**
 * {@link ColumnFamily} 的默认实现，基于 RocksDB Transaction 通过 native handle 直接读写指定列族中的 key-value 对
 *
 * @author zxuanhong
 */
@SuppressWarnings("rawtypes")
public final class DefaultColumnFamily<Key extends KeyType, Value extends ValueType>
    implements ColumnFamily<Key, Value> {
  private final ColumnFamilyHandle familyHandle;
  private final long familyNativeHandle;
  private final Value valueInstance;
  private final Key keyInstance;
  private static final byte[] ZERO_SIZE_ARRAY = new byte[0];
  private final ReadOptions prefixReadOptions;
  private final ReadOptions defaultReadOptions;
  private int keyLength = 0;
  private final ExpandableArrayBuffer keyWriteBuffer = new ExpandableArrayBuffer(500);
  private final DirectBuffer keyViewBuffer = new UnsafeBuffer(0, 0);

  private int valueLength = 0;
  private final ExpandableArrayBuffer valueWriteBuffer = new ExpandableArrayBuffer(2048);
  private final DirectBuffer valueViewBuffer = new UnsafeBuffer(0, 0);
  private final TransactionContext transactionContext;
  private final ColumnFamilies columnFamilies;
  private final Queue<ExpandableArrayBuffer> prefixKeyBuffers;
  private final ExpandableArrayBuffer seekKeyBuffer = new ExpandableArrayBuffer(200);

  private long transactionNativeHandle;
  private final long defaultReadOptionNativeHandle;
  private final long prefixOptionNativeHandle;
  private static MethodHandle putWithHandle;
  private static MethodHandle getWithHandle;
  private static MethodHandle removeWithHandle;

  static {
    try {
      putWithHandle();
      getWithHandle();
      removeWithHandle();
    } catch (final Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  /** 构造列族访问实例，持有列族句柄、读写选项、key/value 类型实例与事务上下文。 */
  public DefaultColumnFamily(
      final RocksdbOptions rocksdbOptions,
      final ColumnFamilyHandle familyHandle,
      final ColumnFamilies columnFamilies,
      final Key keyInstance,
      final Value valueInstance,
      final TransactionContext transactionContext) {
    this.transactionContext = transactionContext;
    prefixReadOptions = rocksdbOptions.prefixReadOptions();
    defaultReadOptions = rocksdbOptions.defaultReadOptions();
    this.familyHandle = familyHandle;
    this.valueInstance = valueInstance;
    this.keyInstance = keyInstance;
    this.columnFamilies = columnFamilies;
    prefixKeyBuffers = new ArrayDeque<>();
    prefixKeyBuffers.add(new ExpandableArrayBuffer());
    prefixKeyBuffers.add(new ExpandableArrayBuffer());
    familyNativeHandle = familyHandle.getNativeHandle();
    defaultReadOptionNativeHandle = defaultReadOptions.getNativeHandle();
    prefixOptionNativeHandle = prefixReadOptions.getNativeHandle();
  }

  private static void putWithHandle() throws NoSuchMethodException {
    final var method =
        Transaction.class.getDeclaredMethod(
            "put",
            Long.TYPE,
            byte[].class,
            Integer.TYPE,
            Integer.TYPE,
            byte[].class,
            Integer.TYPE,
            Integer.TYPE,
            Long.TYPE,
            Boolean.TYPE);
    method.setAccessible(true);
    try {
      putWithHandle = MethodHandles.lookup().unreflect(method);
    } catch (final IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  private static void getWithHandle() throws NoSuchMethodException {
    final var method =
        Transaction.class.getDeclaredMethod(
            "get", Long.TYPE, Long.TYPE, byte[].class, Integer.TYPE, Integer.TYPE, Long.TYPE);
    method.setAccessible(true);
    try {
      getWithHandle = MethodHandles.lookup().unreflect(method);
    } catch (final IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  private static void removeWithHandle() throws NoSuchMethodException {
    final var method =
        Transaction.class.getDeclaredMethod(
            "delete", Long.TYPE, byte[].class, Integer.TYPE, Long.TYPE, Boolean.TYPE);
    method.setAccessible(true);
    try {
      removeWithHandle = MethodHandles.lookup().unreflect(method);
    } catch (final IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  void writeKey(final KeyType key) {
    keyWriteBuffer.putInt(0, columnFamilies.virtualFamily(), ByteOrder.BIG_ENDIAN);
    key.write(keyWriteBuffer, Integer.BYTES);
    keyLength = key.getLength() + Integer.BYTES;
  }

  void writeValue(final ValueType value) {
    value.write(valueWriteBuffer, 0);
    valueLength = value.getLength();
  }

  private boolean toInstance(
      final RocksIterator iterator, final KeyValuePairVisitor<Key, Value> visitor) {
    final byte[] key = iterator.key();
    if (key != null) {
      // 包装时去除 key 中的列族前缀部分
      keyViewBuffer.wrap(key, Integer.BYTES, key.length - Integer.BYTES);
    } else {
      keyViewBuffer.wrap(ZERO_SIZE_ARRAY);
    }
    keyInstance.wrap(keyViewBuffer, 0, keyViewBuffer.capacity());

    valueViewBuffer.wrap(iterator.value());
    valueInstance.wrap(valueViewBuffer, 0, valueViewBuffer.capacity());
    return visitor.visit(keyInstance, valueInstance);
  }

  @Override
  public void put(final Key key, final Value value) {
    ensureInOpenTransaction(
        _ -> {
          try {
            writeKey(key);
            writeValue(value);
            putWithHandle.invokeExact(
                transactionNativeHandle,
                keyWriteBuffer.byteArray(),
                0,
                keyLength,
                valueWriteBuffer.byteArray(),
                0,
                valueLength,
                familyNativeHandle,
                false);
          } catch (final Throwable e) {
            LangUtil.rethrowUnchecked(e);
          }
        });
  }

  @Override
  public Value get(final Key key) {
    // 以返回 null 与否判断存在性：空字节数组 value 也是有效值，不能以长度判断
    final boolean[] found = {false};
    ensureInOpenTransaction(
        _ -> {
          try {
            writeKey(key);
            final byte[] bytes =
                (byte[])
                    getWithHandle.invokeExact(
                        transactionNativeHandle,
                        defaultReadOptionNativeHandle,
                        keyWriteBuffer.byteArray(),
                        0,
                        keyLength,
                        familyNativeHandle);
            found[0] = bytes != null;
            valueViewBuffer.wrap(Objects.requireNonNullElse(bytes, ZERO_SIZE_ARRAY));
          } catch (final Throwable e) {
            LangUtil.rethrowUnchecked(e);
          }
        });
    if (!found[0]) {
      return null;
    }
    valueInstance.wrap(valueViewBuffer, 0, valueViewBuffer.capacity());
    return valueInstance;
  }

  @Override
  public void forEach(final Consumer<Value> consumer) {
    ensureInOpenTransaction(
        transaction -> {
          try (final RocksIterator iterator = getIterator(prefixReadOptions, transaction)) {
            withSeekKeyBuffer(
                NullKeyType.INSTANCE,
                buffer -> {
                  for (iterator.seek(buffer); iterator.isValid(); iterator.next()) {
                    toInstance(
                        iterator,
                        (key, value) -> {
                          consumer.accept(value);
                          return false;
                        });
                  }
                });
          }
        });
  }

  @Override
  public void forEach(final BiConsumer<Key, Value> consumer) {
    ensureInOpenTransaction(
        transaction -> {
          try (final RocksIterator iterator = getIterator(prefixReadOptions, transaction)) {
            withSeekKeyBuffer(
                NullKeyType.INSTANCE,
                buffer -> {
                  for (iterator.seek(buffer); iterator.isValid(); iterator.next()) {
                    toInstance(
                        iterator,
                        (key, value) -> {
                          consumer.accept(key, value);
                          return false;
                        });
                  }
                });
          }
        });
  }

  @Override
  public void whileTrue(final KeyValuePairVisitor<Key, Value> visitor) {
    ensureInOpenTransaction(
        transaction -> {
          try (final RocksIterator iterator = getIterator(prefixReadOptions, transaction)) {
            withSeekKeyBuffer(
                NullKeyType.INSTANCE,
                buffer -> {
                  for (iterator.seek(buffer); iterator.isValid(); iterator.next()) {
                    final boolean result = toInstance(iterator, visitor);
                    if (!result) {
                      break;
                    }
                  }
                });
          }
        });
  }

  @Override
  public void whileTrue(final Key startAtKey, final KeyValuePairVisitor<Key, Value> visitor) {
    ensureInOpenTransaction(
        transaction -> {
          forEachInPrefix(startAtKey, NullKeyType.INSTANCE, visitor, transaction);
        });
  }

  private void forEachInPrefix(
      final KeyType startAt,
      final KeyType prefix,
      final KeyValuePairVisitor<Key, Value> visitor,
      final Transaction transaction) {
    final var seekTarget = Objects.requireNonNullElse(startAt, prefix);
    Objects.requireNonNull(prefix);
    Objects.requireNonNull(visitor);
    /*
     * 注意：目前 Java 版 RocksDB 似乎无法为 iterator 设置灵活的 prefix extractor，因此使用前缀主要
     * 用于跳过不包含该前缀 key 的文件（这本身也有用），但 iterator 仍会遍历这些文件中的所有 key，
     * 所以仍需自行校验 key 是否真正匹配该前缀。
     *
     * <p>在迭代后续 key 时必须进行该校验。
     */
    withPrefixKey(
        prefix,
        (prefixKey, prefixLength) -> {
          try (final RocksIterator iterator = getIterator(prefixReadOptions, transaction)) {
            withSeekKeyBuffer(
                seekTarget,
                buffer -> {
                  boolean shouldVisitNext = true;
                  for (iterator.seek(buffer);
                      iterator.isValid() && shouldVisitNext;
                      iterator.next()) {
                    final byte[] keyBytes = iterator.key();
                    if (!startsWith(prefixKey, 0, prefixLength, keyBytes, 0, keyBytes.length)) {
                      break;
                    }
                    shouldVisitNext = toInstance(iterator, visitor);
                  }
                });
          }
        });
  }

  @Override
  public void whileEqualPrefix(final KeyType keyPrefix, final BiConsumer<Key, Value> visitor) {
    whileEqualPrefix(
        keyPrefix,
        (key, value) -> {
          visitor.accept(key, value);
          return true;
        });
  }

  /** 本类的所有公有方法都应通过该方法执行操作，以保证对列族的所有操作都发生在事务内。 在私有方法中可以假定事务已经打开。 */
  private void ensureInOpenTransaction(final TransactionConsumer operation) {
    transactionContext.runInTransaction(
        () -> {
          final RocksdbRepositoryTransaction currentTransaction =
              (RocksdbRepositoryTransaction) transactionContext.getCurrentTransaction();
          final Transaction transaction = currentTransaction.getTransaction();
          transactionNativeHandle = currentTransaction.getNativeHandle();
          operation.run(transaction);
        });
  }

  @Override
  public void whileEqualPrefix(
      final KeyType keyPrefix, final KeyValuePairVisitor<Key, Value> visitor) {
    ensureInOpenTransaction(transaction -> forEachInPrefix(keyPrefix, visitor, transaction));
  }

  @Override
  public void whileEqualPrefix(
      final KeyType keyPrefix,
      final Key startAtKey,
      final KeyValuePairVisitor<Key, Value> visitor) {
    ensureInOpenTransaction(
        transaction -> {
          forEachInPrefix(startAtKey, keyPrefix, visitor, transaction);
        });
  }

  /** 将列族前缀与给定 key 写入前缀缓冲区，包装成字节数组及其长度交给 consumer 使用，使用完毕后归还缓冲区。 */
  public void withPrefixKey(final KeyType key, final ObjIntConsumer<byte[]> prefixKeyConsumer) {
    if (prefixKeyBuffers.peek() == null) {
      throw new IllegalStateException(
          "Currently nested prefix iterations are not supported! This will cause unexpected behavior.");
    }
    final ExpandableArrayBuffer prefixKeyBuffer = prefixKeyBuffers.remove();
    try {
      prefixKeyBuffer.putInt(0, columnFamilies.virtualFamily(), ByteOrder.BIG_ENDIAN);
      key.write(prefixKeyBuffer, Integer.BYTES);
      final int prefixLength = Integer.BYTES + key.getLength();

      prefixKeyConsumer.accept(prefixKeyBuffer.byteArray(), prefixLength);
    } finally {
      prefixKeyBuffers.add(prefixKeyBuffer);
    }
  }

  /** 将列族前缀与给定 key 写入 seek 缓冲区，包装成 ByteBuffer 交给 consumer 使用。 */
  public void withSeekKeyBuffer(final KeyType key, final Consumer<ByteBuffer> keyConsumer) {
    seekKeyBuffer.putInt(0, columnFamilies.virtualFamily(), ByteOrder.BIG_ENDIAN);
    key.write(seekKeyBuffer, Integer.BYTES);
    final int len = Integer.BYTES + key.getLength();
    keyConsumer.accept(ByteBuffer.wrap(seekKeyBuffer.byteArray(), 0, len));
  }

  @Override
  public void delete(final Key key) {
    ensureInOpenTransaction(
        _ -> {
          try {
            writeKey(key);
            removeWithHandle.invokeExact(
                transactionNativeHandle,
                keyWriteBuffer.byteArray(),
                keyLength,
                familyNativeHandle,
                false);
          } catch (final Throwable e) {
            LangUtil.rethrowUnchecked(e);
          }
        });
  }

  @Override
  public boolean exists(final Key key) {
    // 同 get()：空字节数组 value 也视为存在
    final boolean[] found = {false};
    ensureInOpenTransaction(
        _ -> {
          try {
            writeKey(key);
            final byte[] bytes =
                (byte[])
                    getWithHandle.invokeExact(
                        transactionNativeHandle,
                        defaultReadOptionNativeHandle,
                        keyWriteBuffer.byteArray(),
                        0,
                        keyLength,
                        familyNativeHandle);
            found[0] = bytes != null;
          } catch (final Throwable e) {
            LangUtil.rethrowUnchecked(e);
          }
        });
    return found[0];
  }

  private RocksIterator getIterator(final ReadOptions readOptions, final Transaction transaction) {
    return transaction.getIterator(readOptions, familyHandle);
  }

  private void forEachInPrefix(
      final KeyType prefix,
      final KeyValuePairVisitor<Key, Value> visitor,
      final Transaction transaction) {
    forEachInPrefix(prefix, prefix, visitor, transaction);
  }

  @Override
  public boolean isEmpty() {
    final boolean[] isEmpty = {true};
    ensureInOpenTransaction(
        transaction ->
            forEachInPrefix(
                NullKeyType.INSTANCE,
                (key, value) -> {
                  isEmpty[0] = false;
                  return false;
                },
                transaction));

    return isEmpty[0];
  }

  /**
   * 实现列族条目统计方法的推荐方式。
   *
   * <p>迭代每个条目时不反序列化 value。若需要 value，可考虑使用 {@link #forEachInPrefix}。
   *
   * @param prefix 所有被迭代的 key 的公共前缀
   * @return 列族中具有给定前缀的条目数量
   */
  private long countEachInPrefix(final KeyType prefix, final Transaction transaction) {
    final var seekTarget = Objects.requireNonNull(prefix);
    final long[] count = {0};
    withPrefixKey(
        seekTarget,
        (prefixKey, prefixLength) -> {
          try (final RocksIterator iterator = getIterator(prefixReadOptions, transaction)) {
            withSeekKeyBuffer(
                prefix,
                buffer -> {
                  for (iterator.seek(buffer); iterator.isValid(); iterator.next()) {
                    final byte[] keyBytes = iterator.key();
                    if (!startsWith(prefixKey, 0, prefixLength, keyBytes, 0, keyBytes.length)) {
                      break;
                    }
                    count[0]++;
                  }
                });
          }
        });
    return count[0];
  }

  @Override
  public long count() {
    return countEqualPrefix(NullKeyType.INSTANCE);
  }

  @Override
  public long countEqualPrefix(final KeyType prefix) {
    final long[] count = {0};
    ensureInOpenTransaction(
        transaction -> {
          count[0] = countEachInPrefix(prefix, transaction);
        });
    return count[0];
  }
}
