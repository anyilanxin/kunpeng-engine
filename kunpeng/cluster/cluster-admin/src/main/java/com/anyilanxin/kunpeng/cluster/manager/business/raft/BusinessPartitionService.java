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
package com.anyilanxin.kunpeng.cluster.manager.business.raft;

import com.anyilanxin.kunpeng.cluster.business.PartitionService;
import com.anyilanxin.kunpeng.cluster.manager.business.raft.step.*;
import com.anyilanxin.kunpeng.cluster.manager.business.raft.step.transition.BusinessPartitionTransitionStep;
import com.anyilanxin.kunpeng.cluster.raft.RaftBusinessMetaListener;
import com.anyilanxin.kunpeng.cluster.raft.RaftRoleChangeListener;
import com.anyilanxin.kunpeng.cluster.utils.health.FailureListener;
import com.anyilanxin.kunpeng.scheduler.startup.StartupProcess;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 表示 Raft 分区与业务分区的组合。目前构建分区的唯一方式是通过 {@link #bootstrapping(BusinessPartitionStartupContext)} 方法。 */
public final class BusinessPartitionService
    extends PartitionService<BusinessPartitionStartupContext> {
  private static final Logger LOGGER = LoggerFactory.getLogger(BusinessPartitionService.class);

  private BusinessPartitionService(
      final BusinessPartitionStartupContext context,
      final StartupProcess<BusinessPartitionStartupContext> startupProcess) {
    super(context, startupProcess);
  }

  @Override
  public boolean equals(final Object obj) {
    return super.equals(obj);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }

  /**
   * 创建一个启动时采用引导（bootstrap）流程的分区。引导流程假定当前节点已是该分区复制组的成员； 若节点本地未存储分区配置，则从静态节点配置推导初始配置。
   *
   * @param context 已填充完成、供分区使用的上下文
   * @return 可启动的分区实例
   */
  public static BusinessPartitionService bootstrapping(
      final BusinessPartitionStartupContext context) {
    return new BusinessPartitionService(
        context,
        new StartupProcess<>(
            LOGGER,
            List.of(
                new BusinessRaftBootstrapStep(),
                createRaftRoleChangeListenerStep(context),
                createFailureListenerStep(context),
                createRaftBusinessMetaListenerStep(context),
                new BusinessPartitionTransitionStep())));
  }

  public static BusinessPartitionService joining(final BusinessPartitionStartupContext context) {
    return new BusinessPartitionService(
        context,
        new StartupProcess<>(
            LOGGER,
            List.of(
                new BusinessRaftJoinStep(),
                createRaftRoleChangeListenerStep(context),
                createFailureListenerStep(context),
                createRaftBusinessMetaListenerStep(context),
                new BusinessPartitionTransitionStep())));
  }

  private static BusinessFailureListenerStep createFailureListenerStep(
      final BusinessPartitionStartupContext context) {
    final List<FailureListener> failureListeners = new ArrayList<>();
    failureListeners.add(context.getBrokerTopologyService());
    return new BusinessFailureListenerStep(failureListeners);
  }

  private static BusinessRaftRoleChangeListenerStep createRaftRoleChangeListenerStep(
      final BusinessPartitionStartupContext context) {
    final List<RaftRoleChangeListener> raftRoleChangeListeners = new CopyOnWriteArrayList<>();
    raftRoleChangeListeners.add(context.getBrokerTopologyService());
    return new BusinessRaftRoleChangeListenerStep(raftRoleChangeListeners);
  }

  private static BusinessBusinessMetaListenerStep createRaftBusinessMetaListenerStep(
      final BusinessPartitionStartupContext context) {
    final List<RaftBusinessMetaListener> businessMetaListeners = new CopyOnWriteArrayList<>();
    businessMetaListeners.add(context.getBrokerTopologyService());
    return new BusinessBusinessMetaListenerStep(businessMetaListeners);
  }
}
