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
package com.anyilanxin.kunpeng.broker;

import com.anyilanxin.kunpeng.cluster.cluster.AtomixCluster;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.MessagingService;
import com.anyilanxin.kunpeng.configuration.broker.BrokerCfg;
import com.anyilanxin.kunpeng.configuration.cluster.ClusterCfg;
import com.anyilanxin.kunpeng.scheduler.ActorSchedulingService;
import io.micrometer.core.instrument.MeterRegistry;

/** 由 Broker 直接管理的组件/actor 的上下文。 */
public interface BrokerContext {

  AtomixCluster getAtomixCluster();

  MessagingService getApiMessagingService();

  BrokerCfg getBrokerCfg();

  ClusterCfg getClusterCfg();

  ActorSchedulingService getActorSchedulingService();

  MeterRegistry getMeterRegistry();

  String getNodeId();
}
