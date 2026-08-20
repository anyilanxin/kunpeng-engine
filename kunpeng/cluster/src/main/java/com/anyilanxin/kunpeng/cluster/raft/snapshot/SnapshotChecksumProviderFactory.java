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
package com.anyilanxin.kunpeng.cluster.raft.snapshot;

import java.nio.file.Path;

/**
 * {@link SnapshotChecksumProvider} 工厂：每个分区创建独立的校验和提供方实例。
 *
 * <p>由 {@code RaftPartition} 持有并传入各个 {@code RaftPartition}，
 * 后者在创建 {@code RaftPartitionServer} 时调用 {@link #create(int, Path)}
 * 生成该分区的专属实例。
 */
@FunctionalInterface
public interface SnapshotChecksumProviderFactory {

  /**
   * 为指定分区创建校验和提供方。
   *
   * @param partitionId 分区 ID
   * @param partitionDataDirectory 分区数据目录
   * @return 该分区的校验和提供方实例
   */
  SnapshotChecksumProvider create(int partitionId, Path partitionDataDirectory);
}
