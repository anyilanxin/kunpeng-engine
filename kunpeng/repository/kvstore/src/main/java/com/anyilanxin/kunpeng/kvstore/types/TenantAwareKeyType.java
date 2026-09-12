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
package com.anyilanxin.kunpeng.kvstore.types;

import java.util.Collection;
import java.util.Collections;
import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;

/**
 * TenantAwareKeyType 包装任意给定的 key，并根据 PlacementType 将租户信息前置于或追加到该 key。
 *
 * <p>根据使用场景选择 PREFIX 或 SUFFIX：需要按租户检索 value 时适合用 PREFIX；需要与租户无关地检索 value 时用 SUFFIX。
 *
 * <p>使用 PREFIX 时需要注意可能产生优先级差异：RocksDB 中的 key 是有序的。以 Jobs 为例，激活时需要 遍历所有可激活的 job 返回给 worker，我们不希望租户
 * AAA 的 job 比租户 ZZZ 的 job 更优先，这种场景 更适合使用 SUFFIX。
 */
public record TenantAwareKeyType<WrappedKey extends KeyType>(
    StringType tenantKey, WrappedKey wrappedKey, PlacementType placementType)
    implements ContainsForeignKeys, KeyType {

  /** 返回租户 key */
  @Override
  public StringType tenantKey() {
    return tenantKey;
  }

  /** 返回被包装的 key */
  @Override
  public WrappedKey wrappedKey() {
    return wrappedKey;
  }

  /** 按 placementType 的位置关系依次从缓冲区读取租户 key 与被包装 key */
  @Override
  public void wrap(final DirectBuffer buffer, final int offset, final int length) {
    switch (placementType) {
      case PREFIX -> {
        tenantKey.wrap(buffer, offset, length);
        final var tenantKeyLength = tenantKey.getLength();
        wrappedKey.wrap(buffer, offset + tenantKeyLength, length - tenantKeyLength);
      }
      case SUFFIX -> {
        wrappedKey.wrap(buffer, offset, length);
        final var wrappedKeyLength = wrappedKey.getLength();
        tenantKey.wrap(buffer, offset + wrappedKeyLength, length - wrappedKeyLength);
      }
      default -> throw new IllegalStateException("Unexpected value: " + placementType);
    }
  }

  /** 返回租户 key 与被包装 key 的序列化长度之和 */
  @Override
  public int getLength() {
    return wrappedKey.getLength() + tenantKey.getLength();
  }

  /** 按 placementType 的位置关系依次写入租户 key 与被包装 key */
  @Override
  public void write(final MutableDirectBuffer buffer, final int offset) {
    switch (placementType) {
      case PREFIX -> {
        tenantKey.write(buffer, offset);
        final var tenantKeyLength = tenantKey.getLength();
        wrappedKey.write(buffer, offset + tenantKeyLength);
      }
      case SUFFIX -> {
        wrappedKey.write(buffer, offset);
        final var wrappedKeyLength = wrappedKey.getLength();
        tenantKey.write(buffer, offset + wrappedKeyLength);
      }
      default -> throw new IllegalStateException("Unexpected value: " + placementType);
    }
  }

  /**
   * 返回被包装 key 包含的外键集合；被包装 key 不具备外键能力时返回空集合
   *
   * @return 外键集合
   */
  @Override
  public Collection<ForeignKeyType<KeyType>> containedForeignKeys() {
    if (wrappedKey instanceof ContainsForeignKeys) {
      return ((ContainsForeignKeys) wrappedKey).containedForeignKeys();
    }

    return Collections.emptyList();
  }

  /** 租户 key 在组合 key 中的放置位置 */
  public enum PlacementType {
    /** 置于被包装 key 之前 */
    PREFIX,
    /** 置于被包装 key 之后 */
    SUFFIX
  }
}
