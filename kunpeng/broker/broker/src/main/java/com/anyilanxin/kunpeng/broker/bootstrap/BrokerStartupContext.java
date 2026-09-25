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
package com.anyilanxin.kunpeng.broker.bootstrap;

import com.anyilanxin.kunpeng.broker.admin.ClusterAdminService;
import com.anyilanxin.kunpeng.broker.bootstrap.step.adminapi.CommandApiServiceImpl;
import com.anyilanxin.kunpeng.broker.bootstrap.step.idgenerator.NodeIdGeneratorService;
import com.anyilanxin.kunpeng.broker.bootstrap.step.idgenerator.NodeIdGeneratorServiceImpl;
import com.anyilanxin.kunpeng.broker.business.ClusterBusinessService;
import com.anyilanxin.kunpeng.cluster.cluster.AtomixCluster;
import com.anyilanxin.kunpeng.cluster.config.ClusterMetaStore;
import com.anyilanxin.kunpeng.cluster.config.topology.broker.DefaultClusterSwimTopologyService;
import com.anyilanxin.kunpeng.cluster.config.topology.cluster.ClusterTopologyService;
import com.anyilanxin.kunpeng.cluster.dispatch.api.ClusterDispatchClient;
import com.anyilanxin.kunpeng.cluster.dispatch.scheduling.TimerClock;
import com.anyilanxin.kunpeng.configuration.broker.BrokerCfg;
import com.anyilanxin.kunpeng.configuration.cluster.ClusterCfg;
import com.anyilanxin.kunpeng.scheduler.ActorSchedulingService;
import com.anyilanxin.kunpeng.scheduler.ConcurrencyControl;
import com.anyilanxin.kunpeng.sink.config.SinksConfig;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.BeanFactory;

/**
 * broker 启动与关闭过程中使用的上下文，包含启动/关闭所需的依赖。它是可修改的上下文， 会在启动或关闭过程中被更新。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public interface BrokerStartupContext {
  BrokerCfg getBrokerConfiguration();

  BeanFactory getBeanFactory();

  ClusterCfg getClusterCft();

  SinksConfig getSinksConfig();

  ActorSchedulingService getActorSchedulingService();

  AtomixCluster getAtomixCluster();

  MeterRegistry getMeterRegistry();

  ConcurrencyControl getConcurrencyControl();

  ClusterAdminService getClusterManagerService();

  void setClusterManagerService(ClusterAdminService clusterManagerService);

  ClusterBusinessService getClusterBusinessService();

  void setClusterBusinessService(ClusterBusinessService clusterBusinessService);

  CommandApiServiceImpl getCommandApiService();

  void setCommandApiService(CommandApiServiceImpl commandApiService);

  ClusterMetaStore getClusterMetaStore();

  void setClusterMetaStore(ClusterMetaStore clusterMetaStore);

  NodeIdGeneratorService getRequestIdGenerator();

  void setRequestIdGenerator(NodeIdGeneratorServiceImpl idGenerator);

  ClusterDispatchClient getClusterDispatchClient();

  void setClusterDispatchClient(ClusterDispatchClient dispatchClient);

  /** broker 层 job 流推送服务件（协调器 + 引擎适配器 + 失败回退）；raft 业务层 transition 消费 */
  com.anyilanxin.kunpeng.broker.jobstream.JobStreamDispatcher getJobStreamDispatcher();

  void setJobStreamDispatcher(
      com.anyilanxin.kunpeng.broker.jobstream.JobStreamDispatcher jobStreamDispatcher);

  /** 业务命令 API 服务（business 链 client 命令入口，分区 leader 时经 transition 绑定日志写入器） */
  com.anyilanxin.kunpeng.broker.commandapi.CommandApiServiceImpl getBusinessCommandApiService();

  void setBusinessCommandApiService(
      com.anyilanxin.kunpeng.broker.commandapi.CommandApiServiceImpl businessCommandApiService);

  DefaultClusterSwimTopologyService getClusterPartitionTopology();

  void setClusterPartitionTopology(DefaultClusterSwimTopologyService clusterPartitionTopology);

  /** 集群分区拓扑只读视图：汇聚各成员经 SWIM 广播的分区状态 */
  ClusterTopologyService getClusterTopologyService();

  void setClusterTopologyService(ClusterTopologyService clusterTopologyService);

  TimerClock getTimerClock();

  void setTimerClock(TimerClock timerClock);
}
