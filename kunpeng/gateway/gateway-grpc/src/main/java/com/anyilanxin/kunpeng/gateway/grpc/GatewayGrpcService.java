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
package com.anyilanxin.kunpeng.gateway.grpc;

import com.anyilanxin.kunpeng.broker.client.business.BrokerClient;
import com.anyilanxin.kunpeng.cluster.cluster.AtomixCluster;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.ClusterCommunicationService;
import com.anyilanxin.kunpeng.cluster.config.BrokerTopologyManager;
import com.anyilanxin.kunpeng.cluster.utils.TlsConfigUtil;
import com.anyilanxin.kunpeng.configuration.SecurityCfg;
import com.anyilanxin.kunpeng.configuration.gateway.GatewayCfg;
import com.anyilanxin.kunpeng.configuration.gateway.GatewayGrpcNetworkCfg;
import com.anyilanxin.kunpeng.configuration.gateway.GatewayThreadsCfg;
import com.anyilanxin.kunpeng.gateway.grpc.gatewaydiscover.GatewayTopologyManager;
import com.anyilanxin.kunpeng.gateway.grpc.gatewaydiscover.GatewayTopologyManagerImpl;
import com.anyilanxin.kunpeng.gateway.grpc.health.ActiveConnectionCounter;
import com.anyilanxin.kunpeng.gateway.grpc.health.GatewayHealthManager;
import com.anyilanxin.kunpeng.gateway.grpc.health.impl.GatewayHealthManagerImpl;
import com.anyilanxin.kunpeng.gateway.grpc.service.GrpcService;
import com.anyilanxin.kunpeng.gateway.grpc.service.impl.*;
import com.anyilanxin.kunpeng.gateway.job.GatewayJobHub;
import com.anyilanxin.kunpeng.protocol.gateway.GatewayLoggers;
import com.anyilanxin.kunpeng.scheduler.Actor;
import com.anyilanxin.kunpeng.utils.error.FatalErrorHandler;
import io.grpc.Server;
import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NettyServerBuilder;
import io.micrometer.core.instrument.MeterRegistry;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;

/**
 * 网关 gRPC 服务器：装配业务服务、执行器与 TLS，并跟随 {@link Actor} 生命周期启停。
 *
 * @author zxuanhong
 * @since 2026.9.0
 */
public class GatewayGrpcService extends Actor {

  private static final Logger LOG = GatewayLoggers.GATEWAY_LOGGER_GRPC;

  private final GatewayCfg gatewayCfg;
  private final MeterRegistry meterRegistry;
  private final BrokerClient brokerClient;
  private final BrokerTopologyManager topologyManager;
  private final GatewayTopologyManager gatewayTopologyManager;
  private final GatewayJobHub jobHub;
  private final ClusterCommunicationService communicationService;
  private final ActiveConnectionCounter connectionCounter = new ActiveConnectionCounter();
  private final GatewayHealthManager healthManager;
  private final List<GrpcService> grpcServices;
  private final Server grpcServer;
  private ExecutorService workerPool;

  public GatewayGrpcService(
      final GatewayCfg gatewayCfg,
      final MeterRegistry meterRegistry,
      final BrokerClient brokerClient,
      final BrokerTopologyManager topologyManager,
      final GatewayTopologyManager gatewayTopologyManager,
      final GatewayJobHub jobHub,
      final AtomixCluster atomixCluster) {
    this.gatewayCfg = gatewayCfg;
    this.meterRegistry = meterRegistry;
    this.brokerClient = brokerClient;
    this.topologyManager = topologyManager;
    this.gatewayTopologyManager = gatewayTopologyManager;
    this.jobHub = jobHub;
    communicationService = atomixCluster.getCommunicationService();
    // 把本地连接计数交给拓扑管理器，供其对外发布网关负载。
    if (gatewayTopologyManager instanceof final GatewayTopologyManagerImpl impl) {
      impl.setConnectionCounter(connectionCounter);
    }
    grpcServices = createGrpcServices();
    healthManager = new GatewayHealthManagerImpl(grpcServices);
    grpcServer = createServer();
  }

  @Override
  protected void onActorStarting() {
    try {
      grpcServer.start();
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public void close() {
    grpcServer.shutdown();
  }

  private List<GrpcService> createGrpcServices() {
    final GrpcErrorHandle errorHandle = new GrpcErrorHandle();
    return List.of(
        new GrpcDeploymentServiceImpl(brokerClient, errorHandle),
        new GrpcIncidentServiceImpl(brokerClient, errorHandle),
        new GrpcMessageServiceImpl(brokerClient, errorHandle),
        new GrpcSignalServiceImpl(brokerClient, errorHandle),
        new GrpcJobServiceImpl(brokerClient, errorHandle, jobHub),
        new GrpcProcessInstanceServiceImpl(brokerClient, errorHandle),
        new GrpcUserTaskServiceImpl(brokerClient, errorHandle),
        new GrpcVariableServiceImpl(brokerClient, errorHandle),
        new GrpcClusterManageServiceImpl(
            brokerClient,
            errorHandle,
            topologyManager,
            gatewayTopologyManager,
            connectionCounter,
            communicationService));
  }

  private Server createServer() {
    final NettyServerBuilder builder = configureNetwork(gatewayCfg.getNetwork());
    configureExecutor(builder);
    configureTls(builder);
    for (final GrpcService service : grpcServices) {
      builder.addService(service);
    }
    return builder.addService(healthManager.getHealthService()).build();
  }

  private NettyServerBuilder configureNetwork(final GatewayGrpcNetworkCfg cfg) {
    requirePositive(cfg.getMinKeepAliveInterval(), "客户端最小保活间隔");
    final int maxMessageSize = positiveSize(cfg.getMaxMessageSize().toBytes(), "最大消息体积");
    final int flowControlWindow = positiveSize(cfg.getFlowControlWindow().toBytes(), "流控窗口");
    final int maxInboundMetadataSize =
        positiveSize(cfg.getMaxInboundMetadataSize().toBytes(), "请求元数据体积上限");
    requirePositive(cfg.getKeepAliveTime(), "服务端保活探测间隔");
    requirePositive(cfg.getKeepAliveTimeout(), "服务端保活超时");

    return NettyServerBuilder.forAddress(new InetSocketAddress(cfg.getHost(), cfg.getPort()))
        .maxInboundMessageSize(maxMessageSize)
        .maxInboundMetadataSize(maxInboundMetadataSize)
        .flowControlWindow(flowControlWindow)
        .permitKeepAliveTime(cfg.getMinKeepAliveInterval().toMillis(), TimeUnit.MILLISECONDS)
        .permitKeepAliveWithoutCalls(false)
        // 服务端主动保活：探测 NAT/负载均衡之后的半开连接，避免孤儿流在执行器里持续堆积。
        .keepAliveTime(cfg.getKeepAliveTime().toNanos(), TimeUnit.NANOSECONDS)
        .keepAliveTimeout(cfg.getKeepAliveTimeout().toNanos(), TimeUnit.NANOSECONDS);
  }

  private void configureExecutor(final NettyServerBuilder builder) {
    final GatewayThreadsCfg threads = gatewayCfg.getThreads();
    // 异步模式允许 FJP 在请求处理阻塞时弹性扩线程（上限约两倍核数），但用 maxThreads 封顶，防止雪崩。
    workerPool =
        new ForkJoinPool(
            threads.getGrpcMinThreads(),
            new GrpcPoolThreadFactory(),
            FatalErrorHandler.uncaughtExceptionHandler(LOG),
            true,
            0,
            threads.getGrpcMaxThreads(),
            1,
            pool -> false,
            1,
            TimeUnit.MINUTES);
    builder.executor(workerPool);
  }

  private void configureTls(final NettyServerBuilder builder) {
    final SecurityCfg security = gatewayCfg.getNetwork().getSecurity();
    if (!security.isEnabled()) {
      return;
    }

    try {
      builder.sslContext(loadSslContext(security));
    } catch (final Exception e) {
      throw new IllegalArgumentException("gRPC 服务端 TLS 配置无法加载", e);
    }
  }

  private SslContext loadSslContext(final SecurityCfg security) throws Exception {
    final SslContextBuilder builder;
    final var keyStorePath = security.getKeyStore().getFilePath();
    if (keyStorePath != null) {
      final var identity =
          TlsConfigUtil.loadServerIdentity(
              keyStorePath.toPath(), security.getKeyStore().getPassword().toCharArray());
      builder = SslContextBuilder.forServer(identity.privateKey(), identity.certificateChain());
    } else {
      builder =
          SslContextBuilder.forServer(
              security.getCertificateChainPath(), security.getPrivateKeyPath());
    }
    return GrpcSslContexts.configure(builder).build();
  }

  private static void requirePositive(final Duration value, final String label) {
    if (value.isNegative() || value.isZero()) {
      throw new IllegalArgumentException(label + "必须为正数");
    }
  }

  private static int positiveSize(final long bytes, final String label) {
    if (bytes <= 0 || bytes > Integer.MAX_VALUE) {
      throw new IllegalArgumentException(label + "必须为正数");
    }
    return (int) bytes;
  }

  private static final class GrpcPoolThreadFactory
      implements ForkJoinPool.ForkJoinWorkerThreadFactory {

    @Override
    public ForkJoinWorkerThread newThread(final ForkJoinPool pool) {
      final var worker = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool);
      worker.setName("kp-grpc-worker-" + worker.getPoolIndex());
      return worker;
    }
  }
}
