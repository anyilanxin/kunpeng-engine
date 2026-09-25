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
package com.anyilanxin.kunpeng.gateway;

import com.anyilanxin.kunpeng.broker.client.business.BrokerClient;
import com.anyilanxin.kunpeng.cluster.cluster.AtomixCluster;
import com.anyilanxin.kunpeng.cluster.cluster.leaderfound.ClusterLeaderFoundService;
import com.anyilanxin.kunpeng.cluster.config.BrokerTopologyManager;
import com.anyilanxin.kunpeng.configuration.gateway.GatewayCfg;
import com.anyilanxin.kunpeng.configuration.gateway.GatewayGrpcNetworkCfg;
import com.anyilanxin.kunpeng.gateway.grpc.GatewayGrpcService;
import com.anyilanxin.kunpeng.gateway.grpc.gatewaydiscover.GatewayTopologyManager;
import com.anyilanxin.kunpeng.gateway.grpc.gatewaydiscover.GatewayTopologyManagerImpl;
import com.anyilanxin.kunpeng.gateway.job.GatewayJobHub;
import com.anyilanxin.kunpeng.gateway.job.GatewayJobStreamClient;
import com.anyilanxin.kunpeng.protocol.gateway.GatewayInfo;
import com.anyilanxin.kunpeng.scheduler.ActorSchedulingService;
import com.anyilanxin.kunpeng.scheduler.future.ActorFuture;
import com.anyilanxin.kunpeng.scheduler.future.CompletableActorFuture;
import com.anyilanxin.kunpeng.utils.CloseableSilently;
import com.anyilanxin.kunpeng.utils.VersionUtil;
import io.micrometer.core.instrument.MeterRegistry;
import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ScheduledExecutorService;

/**
 * gateway：无状态接入层。终结 client 的 gRPC 连接，聚合本地 worker 注册广播到 broker 全节点， 维护挂起的 long-poll 表并转发 push
 * 流（workjob 设计 §2）。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class Gateway implements CloseableSilently {
  /** 默认 long-poll 挂起时长：到期返回空批，client 立即重挂，兼作双向存活探活（设计 §5） */
  public static final long DEFAULT_LONG_POLL_MILLIS = 30_000;

  private final ActorSchedulingService schedulingService;
  private final GatewayCfg gatewayCfg;
  private final AtomixCluster atomixCluster;
  private final MeterRegistry meterRegistry;
  private final GatewayInfo localGateway;
  private final BrokerClient brokerClient;
  private final GatewayGrpcService gatewayGrpcService;
  private final BrokerTopologyManager topologyManager;
  private final GatewayTopologyManagerImpl gatewayTopologyManager;
  private final GatewayJobHub jobHub;
  private final GatewayJobStreamClient jobStreamClient;
  private final ScheduledExecutorService jobHubScheduler;

  public Gateway(
      final ActorSchedulingService schedulingService,
      final GatewayCfg gatewayCfg,
      final AtomixCluster atomixCluster,
      final MeterRegistry meterRegistry,
      final ClusterLeaderFoundService leaderService,
      final BrokerClient brokerClient,
      final BrokerTopologyManager topologyManager) {
    this.schedulingService = schedulingService;
    this.gatewayCfg = gatewayCfg;
    this.atomixCluster = atomixCluster;
    localGateway = createGatewayInfo(gatewayCfg);
    this.meterRegistry = meterRegistry;
    this.topologyManager = topologyManager;
    this.brokerClient = brokerClient;
    gatewayTopologyManager =
        new GatewayTopologyManagerImpl(
            atomixCluster.getMembershipService(),
            atomixCluster.getCommunicationService(),
            localGateway);
    jobHubScheduler = newDaemonScheduler("gateway-job-hub");
    jobHub = new GatewayJobHub(jobHubScheduler, DEFAULT_LONG_POLL_MILLIS);
    jobHub.setBrokerClient(brokerClient);
    jobStreamClient =
        new GatewayJobStreamClient(
            atomixCluster.getCommunicationService(), atomixCluster.getMembershipService(), jobHub);
    gatewayGrpcService = buildGrpcService();
  }

  private static ScheduledExecutorService newDaemonScheduler(final String name) {
    return java.util.concurrent.Executors.newSingleThreadScheduledExecutor(
        r -> {
          final Thread thread = new Thread(r, name);
          thread.setDaemon(true);
          return thread;
        });
  }

  private GatewayInfo createGatewayInfo(final GatewayCfg gatewayCfg) {
    final GatewayGrpcNetworkCfg network = gatewayCfg.getNetwork();
    String grpcHost = network.getHost();
    if (network.userCustomGrpcHost()) {
      grpcHost = network.getGrpcHost();
    }
    if (grpcHost == null || grpcHost.isBlank()) {
      throw new IllegalStateException("Gateway gRPC host must not be empty");
    }
    int grpcPort = network.getPort();
    if (network.userCustomGrpcPort()) {
      grpcPort = network.getGrpcPort();
    }
    final GatewayInfo result =
        new GatewayInfo(
                atomixCluster.getMembershipService().getLocalMember().id().id(),
                URI.create("http://" + grpcHost + ":" + grpcPort).toString())
            .setAllowClientDiscovery(gatewayCfg.isAllowClientDiscovery());
    final String version = VersionUtil.getVersion();
    if (version != null && !version.isBlank()) {
      result.setVersion(version);
    }
    return result;
  }

  GatewayGrpcService buildGrpcService() {
    return new GatewayGrpcService(
        gatewayCfg,
        meterRegistry,
        brokerClient,
        topologyManager,
        gatewayTopologyManager,
        jobHub,
        atomixCluster);
  }

  public GatewayJobHub jobHub() {
    return jobHub;
  }

  private CompletionStage<GatewayTopologyManager> startGatewayTopology() {
    final var future = new CompletableFuture<GatewayTopologyManager>();
    schedulingService
        .submitActor(gatewayTopologyManager)
        .onComplete(
            (ok, error) -> {
              if (error != null) {
                future.completeExceptionally(error);
                return;
              }
              future.complete(gatewayTopologyManager);
            },
            ForkJoinPool.commonPool());
    return future;
  }

  public ActorFuture<Gateway> start() {
    final var resultFuture = new CompletableActorFuture<Gateway>();
    // job-ready 广播唤醒长轮询（标准 BrokerClient 订阅面）
    brokerClient.subscribeJobAvailableNotification(
        com.anyilanxin.kunpeng.broker.client.jobstream.JobStreamSubjects.READY,
        jobHub::onJobsAvailable);
    schedulingService
        .submitActor(jobStreamClient)
        .onComplete(
            (unused, error) -> {
              if (error != null) {
                resultFuture.completeExceptionally(error);
                return;
              }
              completeGrpcStart(resultFuture);
            });
    return resultFuture;
  }

  private void completeGrpcStart(final CompletableActorFuture<Gateway> resultFuture) {
    schedulingService
        .submitActor(gatewayGrpcService)
        .thenApply(v -> startGatewayTopology())
        .onComplete(
            (unused, throwable) -> {
              if (throwable != null) {
                resultFuture.completeExceptionally(throwable);
              } else {
                resultFuture.complete(this);
              }
            });
  }

  @Override
  public void close() {
    jobStreamClient.close();
    gatewayGrpcService.close();
    jobHubScheduler.shutdownNow();
  }
}
