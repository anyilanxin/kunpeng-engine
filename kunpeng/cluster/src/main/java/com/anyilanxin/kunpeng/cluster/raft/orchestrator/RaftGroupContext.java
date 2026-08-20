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

import com.anyilanxin.kunpeng.cluster.cluster.messaging.ClusterCommunicationService;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.MessagingService;
import com.anyilanxin.kunpeng.cluster.raft.RaftServer.Role;
import com.anyilanxin.kunpeng.cluster.raft.partition.RaftPartition;
import com.anyilanxin.kunpeng.cluster.raft.partition.RaftPartitionGroup;
import io.micrometer.core.instrument.MeterRegistry;
import java.nio.file.Path;

/**
 * Raft 分区组上下文基类。
 *
 * <p>子类继承后可访问全部 raft 基础能力：RaftPartition、通信服务、指标等。
 * {@link #partitionGroup()} 在 {@code RaftPartitionGroupStartupStep} 执行后可用
 * （启动链中该步骤之前的 {@link PartitionStartup} 不应访问分区组）。
 */
public abstract class RaftGroupContext {

  private final String groupName;
  private final NodePartitionMetadata metadata;
  private final Path groupDataDirectory;
  private final MessagingService messagingService;
  private final ClusterCommunicationService communicationService;
  private final MeterRegistry meterRegistry;
  private volatile RaftPartitionGroup partitionGroup;

  protected RaftGroupContext(
      final String groupName,
      final NodePartitionMetadata metadata,
      final Path groupDataDirectory,
      final MessagingService messagingService,
      final ClusterCommunicationService communicationService,
      final MeterRegistry meterRegistry) {
    this.groupName = groupName;
    this.metadata = metadata;
    this.groupDataDirectory = groupDataDirectory;
    this.messagingService = messagingService;
    this.communicationService = communicationService;
    this.meterRegistry = meterRegistry;
  }

  /** 分区组名 */
  public String groupName() {
    return groupName;
  }

  /** 分区组元数据 */
  public NodePartitionMetadata metadata() {
    return metadata;
  }

  /** 分区组数据目录（data/&lt;groupName&gt;/） */
  public Path groupDataDirectory() {
    return groupDataDirectory;
  }

  /** raft 通信框架 */
  public MessagingService messagingService() {
    return messagingService;
  }

  /** 集群通信服务 */
  public ClusterCommunicationService communicationService() {
    return communicationService;
  }

  /** 指标注册中心 */
  public MeterRegistry meterRegistry() {
    return meterRegistry;
  }

  /** Raft 分区组（启动链中的 RaftPartitionGroupStartupStep 执行后可用） */
  public RaftPartitionGroup partitionGroup() {
    final var group = partitionGroup;
    if (group == null) {
      throw new IllegalStateException(
          "Raft 分区组尚未启动（partitionGroup 为 null）；仅在 RaftPartitionGroupStartupStep 之后可用");
    }
    return group;
  }

  /** 分区组是否已启动 */
  public boolean isPartitionGroupStarted() {
    return partitionGroup != null;
  }

  /** 获取指定分区 */
  public RaftPartition partition(final int partitionId) {
    return partitionGroup().getPartition(partitionId);
  }

  /** 获取指定分区的当前角色 */
  public Role role(final int partitionId) {
    final var server = partition(partitionId).getServer();
    return server != null ? server.getRole() : Role.INACTIVE;
  }

  /** 本节点是否为指定分区的 leader */
  public boolean isLeader(final int partitionId) {
    return role(partitionId) == Role.LEADER;
  }

  /** 内部注入（由 RaftPartitionGroupStartupStep 调用） */
  void attachPartitionGroup(final RaftPartitionGroup group) {
    this.partitionGroup = group;
  }

  /** 内部清除（由 RaftPartitionGroupStartupStep.shutdown 调用） */
  void detachPartitionGroup() {
    this.partitionGroup = null;
  }
}
