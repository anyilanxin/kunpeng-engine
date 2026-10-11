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
package com.anyilanxin.kunpeng.broker.client.admin.impl;

import com.anyilanxin.kunpeng.broker.client.admin.AdminBrokerPlane;
import com.anyilanxin.kunpeng.broker.client.admin.ClusterDispatchClient;
import com.anyilanxin.kunpeng.broker.client.admin.commandapi.admin.AdminChangeReplicationRequest;
import com.anyilanxin.kunpeng.broker.client.admin.commandapi.admin.AdminQueryRequest;
import com.anyilanxin.kunpeng.broker.client.admin.commandapi.business.BusinessChangePartitionRequest;
import com.anyilanxin.kunpeng.broker.client.admin.commandapi.business.BusinessChangeReplicationRequest;
import com.anyilanxin.kunpeng.broker.client.admin.commandapi.business.BusinessClusterBalanceRequest;
import com.anyilanxin.kunpeng.broker.client.admin.commandapi.business.BusinessQueryRequest;
import com.anyilanxin.kunpeng.broker.client.core.BrokerResponse;
import com.anyilanxin.kunpeng.broker.client.core.impl.BrokerClientImpl;
import com.anyilanxin.kunpeng.cluster.cluster.leaderfound.ClusterLeaderFoundService;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.ClusterEventService;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.MessagingService;
import com.anyilanxin.kunpeng.cluster.config.topology.cluster.ClusterTopologyService;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.commandapi.admin.dispatch.AdminChangeResponseRecord;
import com.anyilanxin.kunpeng.protocol.admin.impl.record.commandapi.business.dispatch.BusinessChangeResponseRecord;
import com.anyilanxin.kunpeng.scheduler.ActorSchedulingService;
import com.anyilanxin.kunpeng.scheduler.future.ActorFuture;
import java.util.concurrent.CompletableFuture;

/**
 * 集群调度客户端默认实现：管理面/业务面调度请求经统一传输核心下发。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class DefaultClusterDispatchClient implements ClusterDispatchClient {
  private final BrokerClientImpl brokerClient;
  private final ActorSchedulingService actorSchedulingService;

  public DefaultClusterDispatchClient(
      final ActorSchedulingService actorSchedulingService,
      final ClusterLeaderFoundService leaderFoundService,
      final MessagingService messagingService,
      final ClusterEventService eventService,
      final ClusterTopologyService topologyService) {
    this.actorSchedulingService = actorSchedulingService;
    brokerClient =
        new BrokerClientImpl(
            messagingService,
            eventService,
            new AdminBrokerPlane(leaderFoundService),
            topologyService);
  }

  @Override
  public CompletableFuture<BrokerResponse<AdminChangeResponseRecord>> adminChangeReplication(
      final int expectReplicationFactor, final boolean apply) {
    final AdminChangeReplicationRequest request =
        new AdminChangeReplicationRequest()
            .expectReplicationFactor(expectReplicationFactor)
            .apply(apply);
    return brokerClient.sendRequest(request);
  }

  @Override
  public CompletableFuture<BrokerResponse<AdminChangeResponseRecord>> adminDispatchQuery() {
    final AdminQueryRequest request = new AdminQueryRequest();
    return brokerClient.sendRequest(request);
  }

  @Override
  public CompletableFuture<BrokerResponse<BusinessChangeResponseRecord>> businessChangePartition(
      final int expectPartitionsCount, final boolean apply) {
    final BusinessChangePartitionRequest request =
        new BusinessChangePartitionRequest()
            .expectPartitionsCount(expectPartitionsCount)
            .apply(apply);
    return brokerClient.sendRequest(request);
  }

  @Override
  public CompletableFuture<BrokerResponse<BusinessChangeResponseRecord>> businessChangeReplication(
      final int expectReplicationFactor, final boolean apply) {
    final BusinessChangeReplicationRequest request =
        new BusinessChangeReplicationRequest()
            .expectReplicationFactor(expectReplicationFactor)
            .apply(apply);
    return brokerClient.sendRequest(request);
  }

  @Override
  public CompletableFuture<BrokerResponse<BusinessChangeResponseRecord>> businessClusterBalance(
      final boolean apply) {
    final BusinessClusterBalanceRequest request = new BusinessClusterBalanceRequest().apply(apply);
    return brokerClient.sendRequest(request);
  }

  @Override
  public CompletableFuture<BrokerResponse<BusinessChangeResponseRecord>> businessDispatchQuery() {
    final BusinessQueryRequest request = new BusinessQueryRequest();
    return brokerClient.sendRequest(request);
  }

  @Override
  public ActorFuture<Void> start() {
    return actorSchedulingService.submitActor(brokerClient);
  }

  @Override
  public ActorFuture<Void> stop() {
    return brokerClient.closeAsync();
  }
}
