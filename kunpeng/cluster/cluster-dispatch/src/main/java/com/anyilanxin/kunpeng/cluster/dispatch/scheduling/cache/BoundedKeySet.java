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
package com.anyilanxin.kunpeng.cluster.dispatch.scheduling.cache;

import java.util.concurrent.locks.StampedLock;
import java.util.function.IntConsumer;
import org.agrona.collections.LongHashSet;

/**
 * 线程安全、容量有界的 {@code long} key 集合。
 *
 * <p>读取（{@link #contains} 与 {@link #size}）使用 {@link StampedLock} 乐观读，竞争时回落
 * 到真实读锁；所有变更独占写锁。写锁不可重入：{@link #addAll} 与 {@link #removeAll} 直接 变更底层集合，而不委托给 {@link #add} 与 {@link
 * #remove}。
 *
 * <p>插入将超出容量时，先淘汰任意牺牲者；唯一保证是 {@link #size()} 不超过配置容量。
 */
public final class BoundedKeySet {

  private static final int DEFAULT_CAPACITY = 100_000;

  private static final float LOAD_FACTOR = 0.9f;

  private final StampedLock lock = new StampedLock();
  private final int capacity;
  private final LongHashSet keys;
  private final IntConsumer sizeReporter;

  /** 以给定容量创建集合，不做大小上报。 */
  public BoundedKeySet(final int capacity) {
    this(capacity, ignored -> {});
  }

  /** 以默认容量创建集合，并在每次变更时上报大小。 */
  public BoundedKeySet(final IntConsumer sizeReporter) {
    this(DEFAULT_CAPACITY, sizeReporter);
  }

  /** 以给定容量创建集合，每次变更后（以及创建时以 {@code 0}）向 {@code sizeReporter} 上报 大小。 */
  public BoundedKeySet(final int capacity, final IntConsumer sizeReporter) {
    this.capacity = capacity;
    this.sizeReporter = sizeReporter;

    // 超量预分配底层集合，使其在容量范围内永不触发扩容。
    final var resizeThreshold = (int) Math.ceil(capacity * LOAD_FACTOR);
    final var capacityToPreventResize = 2 * capacity - resizeThreshold;
    keys = new LongHashSet(capacityToPreventResize, LOAD_FACTOR, true);
    sizeReporter.accept(0);
  }

  /** 存储 {@code key}；若已达容量则先淘汰任意牺牲者。 */
  public void add(final long key) {
    final long stamp = lock.writeLock();
    try {
      evictExcess(keys.size() + 1);
      keys.add(key);
      reportSize();
    } finally {
      lock.unlockWrite(stamp);
    }
  }

  /** 存储 {@code incoming} 中的所有 key，必要时先淘汰任意牺牲者。 */
  public void addAll(final LongHashSet incoming) {
    final long stamp = lock.writeLock();
    try {
      evictExcess(keys.size() + incoming.size());
      keys.addAll(incoming);
      reportSize();
    } finally {
      lock.unlockWrite(stamp);
    }
  }

  /** 若已存储则遗忘 {@code key}。 */
  public void remove(final long key) {
    final long stamp = lock.writeLock();
    try {
      keys.remove(key);
      reportSize();
    } finally {
      lock.unlockWrite(stamp);
    }
  }

  /** 遗忘 {@code gone} 中的所有 key。 */
  public void removeAll(final LongHashSet gone) {
    final long stamp = lock.writeLock();
    try {
      keys.removeAll(gone);
      reportSize();
    } finally {
      lock.unlockWrite(stamp);
    }
  }

  /** 丢弃所有已存储的 key。 */
  public void clear() {
    final long stamp = lock.writeLock();
    try {
      keys.clear();
      reportSize();
    } finally {
      lock.unlockWrite(stamp);
    }
  }

  /** 返回当前是否已存储 {@code key}。 */
  public boolean contains(final long key) {
    final long stamp = lock.tryOptimisticRead();
    final boolean result = keys.contains(key);
    if (lock.validate(stamp)) {
      return result;
    }
    final long readStamp = lock.readLock();
    try {
      return keys.contains(key);
    } finally {
      lock.unlockRead(readStamp);
    }
  }

  /** 返回当前存储的 key 数量。 */
  public int size() {
    final long stamp = lock.tryOptimisticRead();
    final int result = keys.size();
    if (lock.validate(stamp)) {
      return result;
    }
    final long readStamp = lock.readLock();
    try {
      return keys.size();
    } finally {
      lock.unlockRead(readStamp);
    }
  }

  private void evictExcess(final int projectedSize) {
    final int excess = projectedSize - capacity;
    if (excess <= 0) {
      return;
    }
    final var iterator = keys.iterator();
    for (int removed = 0; removed < excess && iterator.hasNext(); removed++) {
      iterator.nextValue();
      iterator.remove();
    }
  }

  private void reportSize() {
    sizeReporter.accept(keys.size());
  }
}
