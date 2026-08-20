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
import io.micrometer.core.instrument.MeterRegistry;
import java.nio.file.Path;
import java.util.List;

/**
 * 按分区组类型注册的工厂。
 *
 * <p>每种分区组类型（如 "engine"、"metadata"）注册一个工厂实例；
 * {@link #startupSteps(RaftGroupContext)} 返回的有序步骤列表中，
 * {@code RaftPartitionGroupStartupStep} 之前的是前置启动，之后的是后置启动。
 *
 * @param <T> 该类型分区组的上下文类型
 */
public interface PartitionGroupTypeFactory<T extends RaftGroupContext> {

  /**
   * 创建该类型分区组的上下文实例（从元数据恢复或首次创建时调用）
   *
   * @param metadata 分区组元数据
   * @param groupDataDirectory 分区组数据目录
   * @param messagingService raft 通信框架
   * @param communicationService 集群通信服务
   * @param meterRegistry 指标注册中心
   */
  T createContext(
      NodePartitionMetadata metadata,
      Path groupDataDirectory,
      MessagingService messagingService,
      ClusterCommunicationService communicationService,
      MeterRegistry meterRegistry);

  /**
   * 有序启动步骤列表。
   *
   * <p>StartupProcess 顺序执行 startup、倒序执行 shutdown。
   * RaftPartitionGroupStartupStep 之前的是前置启动，之后的是后置启动。
   */
  List<PartitionStartup<T>> startupSteps(T context);

  /** 该分区组的全部监听器（Raft 分区组启动完成后注册） */
  List<PartitionEventListener> listeners(T context);
}
