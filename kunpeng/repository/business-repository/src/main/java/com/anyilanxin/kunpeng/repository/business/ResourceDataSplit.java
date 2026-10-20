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

/**
 * 数据分片接口：仓储声明的 RocksDB column family 划分。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public interface ResourceDataSplit {

  void splitLocalFamilyData(int resourceId, DataVisitor visitor);

  /**
   * 删除变体：移除本域中归属于指定资源的全部数据。默认实现按拆分访问器产出的物理 key 逐个删除—— 与拆分载荷同一迭代、同一过滤，保证删的正是拆走的。
   *
   * @param resourceId 资源 id
   * @param remover 物理删除执行器（key 为 {@code writeKey} 产出的带虚拟列族前缀完整 key）
   */
  default void removeLocalFamilyData(final int resourceId, final DataRemover remover) {
    splitLocalFamilyData(
        resourceId,
        (key, value) -> {
          remover.remove(key);
          return true;
        });
  }

  @FunctionalInterface
  interface DataVisitor {
    boolean visit(final byte[] key, final byte[] value);
  }

  @FunctionalInterface
  interface DataRemover {
    void remove(final byte[] key);
  }
}
