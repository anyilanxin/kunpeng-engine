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
package com.anyilanxin.kunpeng.cluster.raft.orchestrator;

import com.anyilanxin.kunpeng.cluster.raft.partition.PartitionMetadata;
import com.anyilanxin.kunpeng.scheduler.ConcurrencyControl;

import java.nio.file.Path;

/**
 * Raft 分区组上下文基类。
 *
 * <p>子类继承后可访问全部 raft 基础能力：RaftPartition 集合、通信服务、指标等。
 * 分区在 {@code RaftPartitionGroupStartupStep} 执行后可用
 * （启动链中该步骤之前的 {@link PartitionStartup} 不应访问分区）。
 */
public abstract class RaftGroupContext {
  private PartitionMetadata partitionMetadata;
  private Path groupDataDirectory;
  private ConcurrencyControl concurrencyControl;
  boolean initialized = false;

  /**
   * 由 raft 内部完成初始化
   */
  protected void init(final PartitionMetadata partitionMetadata,
                      final Path groupDataDirectory,
                      final ConcurrencyControl concurrencyControl) {
    this.partitionMetadata = partitionMetadata;
    this.groupDataDirectory = groupDataDirectory;
    this.concurrencyControl = concurrencyControl;
  }

  public ConcurrencyControl concurrencyControl() {
    if (!initialized) {
      throw new IllegalStateException("RaftGroupContext has not been initialized");
    }
    return concurrencyControl;
  }

  /** 分区组名 */
  public String groupName() {
    if (!initialized) {
      throw new IllegalStateException("RaftGroupContext has not been initialized");
    }
    return partitionMetadata.id().group();
  }

  /** 分区组元数据 */
  public PartitionMetadata metadata() {
    if (!initialized) {
      throw new IllegalStateException("RaftGroupContext has not been initialized");
    }
    return partitionMetadata;
  }

  /** 分区组数据目录（data/&lt;groupName&gt;/） */
  public Path groupDataDirectory() {
    if (!initialized) {
      throw new IllegalStateException("RaftGroupContext has not been initialized");
    }
    return groupDataDirectory;
  }
}
