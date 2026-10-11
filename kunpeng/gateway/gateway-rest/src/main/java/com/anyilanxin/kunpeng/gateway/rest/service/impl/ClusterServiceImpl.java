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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.anyilanxin.kunpeng.gateway.rest.service.impl;

import static com.anyilanxin.kunpeng.cluster.config.ClusterResourceInfo.to;

import com.anyilanxin.kunpeng.cluster.config.BrokerTopologyManager;
import com.anyilanxin.kunpeng.cluster.config.ClusterResourceInfo;
import com.anyilanxin.kunpeng.cluster.config.topology.cluster.ClusterTopologyService;
import com.anyilanxin.kunpeng.gateway.grpc.gatewaydiscover.GatewayTopologyManager;
import com.anyilanxin.kunpeng.gateway.rest.service.ClusterService;
import com.anyilanxin.kunpeng.gateway.rest.service.vo.ClusterTopologyVo;
import com.anyilanxin.kunpeng.gateway.rest.service.vo.GatewayDiscoveryVo;
import com.anyilanxin.kunpeng.gateway.rest.service.vo.GatewayLoadVo;
import com.anyilanxin.kunpeng.utils.VersionUtil;
import java.util.stream.StreamSupport;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * 集群管理查询业务实现：拓扑读本地快照；网关负载非本机网关经集群通信转发。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
@Service
public class ClusterServiceImpl implements ClusterService {
  private final BrokerTopologyManager brokerTopologyManager;
  private final GatewayTopologyManager gatewayTopologyManager;

  public ClusterServiceImpl(
      final ClusterTopologyService topologyService,
      final GatewayTopologyManager gatewayTopologyManager) {
    this.brokerTopologyManager = new BrokerTopologyManager(topologyService);
    this.gatewayTopologyManager = gatewayTopologyManager;
  }

  @Override
  public Mono<ClusterTopologyVo> topology() {
    final var topology = brokerTopologyManager.getTopology();
    final ClusterResourceInfo clusterResourceInfo = to(topology);
    final var brokers =
        clusterResourceInfo.brokers().stream()
            .map(
                member ->
                    new ClusterTopologyVo.BrokerVo(
                        StreamSupport.stream(member.partitions().spliterator(), false)
                            .map(
                                partition ->
                                    new ClusterTopologyVo.PartitionVo(
                                        partition.partitionId(),
                                        partition.role().value(),
                                        partition.state().value()))
                            .toList()))
            .toList();
    return Mono.just(
        new ClusterTopologyVo(
            brokers.size(), VersionUtil.getVersion(), topology.getPartitions().size(), 1, brokers));
  }

  @Override
  public Mono<GatewayDiscoveryVo> gateways() {
    final var gateways =
        gatewayTopologyManager.getGateways().stream()
            .filter(info -> info.isAllowClientDiscovery())
            .map(GatewayDiscoveryVo::of)
            .toList();
    return Mono.just(new GatewayDiscoveryVo(gateways));
  }

  @Override
  public Mono<GatewayLoadVo> gatewayLoad(final String grpcAddress) {
    return Mono.fromFuture(gatewayTopologyManager.getGatewayLoad(grpcAddress))
        .map(GatewayLoadVo::of);
  }
}
