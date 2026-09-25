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
package com.anyilanxin.kunpeng.gateway.grpc.service.impl;

import static com.anyilanxin.kunpeng.cluster.config.ClusterResourceInfo.to;

import com.anyilanxin.kunpeng.broker.client.business.BrokerClient;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.ClusterCommunicationService;
import com.anyilanxin.kunpeng.cluster.config.BrokerClusterState;
import com.anyilanxin.kunpeng.cluster.config.BrokerTopologyManager;
import com.anyilanxin.kunpeng.cluster.config.ClusterResourceInfo;
import com.anyilanxin.kunpeng.gateway.grpc.GrpcErrorHandle;
import com.anyilanxin.kunpeng.gateway.grpc.gatewaydiscover.GatewayTopologyManager;
import com.anyilanxin.kunpeng.gateway.grpc.health.ActiveConnectionCounter;
import com.anyilanxin.kunpeng.gateway.grpc.service.ClusterManageServiceGrpc;
import com.anyilanxin.kunpeng.gateway.grpc.service.ClusterManageServiceOuterClass;
import com.anyilanxin.kunpeng.gateway.grpc.service.GrpcService;
import com.anyilanxin.kunpeng.protocol.gateway.GatewayConnectorRecord;
import com.anyilanxin.kunpeng.protocol.gateway.GatewayInfo;
import com.anyilanxin.kunpeng.utils.VersionUtil;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * 集群管理 gRPC 服务实现。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class GrpcClusterManageServiceImpl
    extends ClusterManageServiceGrpc.ClusterManageServiceImplBase implements GrpcService {
  /** Forward budget must be strictly less than the client's RPC deadline. */
  private static final Duration FORWARD_TIMEOUT = Duration.ofSeconds(8);

  private final BrokerClient brokerClient;
  private final BrokerTopologyManager topologyManager;
  private final GrpcErrorHandle handle;
  final GatewayTopologyManager gatewayTopologyManager;
  final ActiveConnectionCounter connectionCounter;
  final ClusterCommunicationService communicationService;

  public GrpcClusterManageServiceImpl(
      final BrokerClient brokerClient,
      final GrpcErrorHandle handle,
      final BrokerTopologyManager topologyManager,
      final GatewayTopologyManager gatewayTopologyManager,
      final ActiveConnectionCounter connectionCounter,
      final ClusterCommunicationService communicationService) {
    this.brokerClient = brokerClient;
    this.topologyManager = topologyManager;
    this.handle = handle;
    this.gatewayTopologyManager = gatewayTopologyManager;
    this.connectionCounter = connectionCounter;
    this.communicationService = communicationService;
  }

  @Override
  public String getServiceName() {
    return "Cluster Manage Service";
  }

  @Override
  public void clusterTopology(
      final ClusterManageServiceOuterClass.ClusterTopologyRequest request,
      final StreamObserver<ClusterManageServiceOuterClass.ClusterTopologyResponse>
          responseObserver) {
    final BrokerClusterState topology = topologyManager.getTopology();
    final ClusterResourceInfo clusterResourceInfo = to(topologyManager.getTopology());
    final Set<ClusterResourceInfo.MemberInfo> brokers = clusterResourceInfo.brokers();
    final ClusterManageServiceOuterClass.ClusterTopologyResponse.Builder builder =
        ClusterManageServiceOuterClass.ClusterTopologyResponse.newBuilder();
    builder.setClusterSize(brokers.size());
    builder.setGatewayVersion(VersionUtil.getVersion());
    builder.setPartitionsCount(topology.getPartitions().size());
    builder.setReplicationFactor(1);
    for (final ClusterResourceInfo.MemberInfo memberInfo : brokers) {
      final ClusterManageServiceOuterClass.BrokerInfo.Builder brokerBuilder =
          ClusterManageServiceOuterClass.BrokerInfo.newBuilder();
      for (final ClusterResourceInfo.PartitionInfo partitionInfo : memberInfo.partitions()) {
        final ClusterManageServiceOuterClass.Partition partition =
            ClusterManageServiceOuterClass.Partition.newBuilder()
                .setPartitionId(partitionInfo.partitionId())
                .setRoleValue(partitionInfo.role().value())
                .setHealthValue(partitionInfo.state().value())
                .build();
        brokerBuilder.addPartitions(partition);
      }
      builder.addBrokers(brokerBuilder.build());
    }
    final ClusterManageServiceOuterClass.ClusterTopologyResponse response = builder.build();
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void queryGateway(
      final ClusterManageServiceOuterClass.QueryGatewayRequest request,
      final StreamObserver<ClusterManageServiceOuterClass.QueryGatewayResponse> responseObserver) {
    final ClusterManageServiceOuterClass.QueryGatewayResponse.Builder queryGatewayResponseBuilder =
        ClusterManageServiceOuterClass.QueryGatewayResponse.newBuilder();
    // 处理获取 gateway 信息
    final List<GatewayInfo> gateways = gatewayTopologyManager.getGateways();
    gateways.forEach(
        v -> {
          if (!v.isAllowClientDiscovery()) {
            return;
          }
          final ClusterManageServiceOuterClass.GatewayInfo.Builder builder =
              ClusterManageServiceOuterClass.GatewayInfo.newBuilder();
          builder
              .setVersion(v.getVersion())
              .setNodeId(v.getNodeId())
              .setGrpcAddress(v.getGrpcAddress())
              .setGatewayAddress(v.getGrpcAddress())
              .setAllowClientDiscovery(v.isAllowClientDiscovery());
          queryGatewayResponseBuilder.addGateways(builder.build());
        });
    final ClusterManageServiceOuterClass.QueryGatewayResponse response =
        queryGatewayResponseBuilder.build();
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void queryGatewayLoad(
      final ClusterManageServiceOuterClass.QueryGatewayLoadRequest request,
      final StreamObserver<ClusterManageServiceOuterClass.QueryGatewayLoadResponse>
          responseObserver) {
    final CompletableFuture<GatewayConnectorRecord> gatewayLoad =
        gatewayTopologyManager.getGatewayLoad(request.getTargetGrpcAddress());
    gatewayLoad.whenComplete(
        (record, ex) -> {
          if (ex != null) {
            responseObserver.onError(
                Status.UNAVAILABLE.withDescription(ex.getMessage()).asException());
            return;
          }
          responseObserver.onNext(
              ClusterManageServiceOuterClass.QueryGatewayLoadResponse.newBuilder()
                  .setNodeId(record.nodeId())
                  .setActiveConnections(record.connectorNum())
                  .build());
          responseObserver.onCompleted();
        });
  }
}
