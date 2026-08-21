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
package io.atomix.cluster.discovery;

import com.google.common.net.HostAndPort;
import io.atomix.cluster.BootstrapService;
import io.atomix.cluster.Node;
import io.atomix.cluster.NodeConfig;
import io.atomix.cluster.NodeId;
import io.atomix.utils.event.AbstractListenerManager;
import io.atomix.utils.net.Address;
import io.atomix.utils.VisibleForTesting;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 动态节点发现提供者。
 *
 * <p>核心思路是把"配置里的一串主机名"翻译成"当前存活的节点集合"：每次扫描时逐个解析配置
 * 地址，把解析到的每个 IP（一个主机名可能对应多个 IP）都映射为一个 {@link Node}，再与上一轮
 * 的结果做差集，新增的节点广播 {@link NodeDiscoveryEvent.Type#JOIN}，消失的节点广播 {@link
 * NodeDiscoveryEvent.Type#LEAVE}。
 *
 * <p>扫描由单线程调度器按 {@link DynamicDiscoveryConfig#getRefreshInterval()} 周期驱动，
 * 地址中未写端口时按 {@link DynamicDiscoveryConfig#getDefaultPort()} 补齐。
 */
public final class DynamicDiscoveryProvider
    extends AbstractListenerManager<NodeDiscoveryEvent, NodeDiscoveryEventListener>
    implements NodeDiscoveryProvider {

  private static final Logger LOGGER = LoggerFactory.getLogger(DynamicDiscoveryProvider.class);

  /** 调度线程名，便于日志与线程 dump 中定位。 */
  private static final String REFRESH_THREAD_NAME = "dynamic-discovery-refresh";

  private final Function<String, List<InetAddress>> hostnameResolver;
  private final DynamicDiscoveryConfig config;
  private final int fallbackPort;
  private final Set<Node> knownNodes = ConcurrentHashMap.newKeySet();
  private final AtomicBoolean running = new AtomicBoolean(false);
  private final ScheduledExecutorService refreshExecutor;

  private ScheduledFuture<?> pendingRefresh;

  public static final Type TYPE = new Type();

  /**
   * 以系统默认的 DNS 解析能力构造提供者。
   *
   * @param config 发现配置
   */
  public DynamicDiscoveryProvider(final DynamicDiscoveryConfig config) {
    this(config, DynamicDiscoveryProvider::lookupAllIps);
  }

  /** 测试用构造器：允许注入自定义的主机名解析函数。 */
  @VisibleForTesting
  DynamicDiscoveryProvider(
      final DynamicDiscoveryConfig config,
      final Function<String, List<InetAddress>> hostnameResolver) {
    this.config = Objects.requireNonNull(config, "config cannot be null");
    this.hostnameResolver = hostnameResolver;
    this.fallbackPort = config.getDefaultPort();
    ensureValidRefreshInterval(config);
    this.refreshExecutor =
        Executors.newSingleThreadScheduledExecutor(
            task -> {
              final Thread worker = new Thread(task, REFRESH_THREAD_NAME);
              worker.setDaemon(true);
              return worker;
            });
  }

  @Override
  public DynamicDiscoveryConfig config() {
    return config;
  }

  @Override
  public Set<Node> getNodes() {
    return Set.copyOf(knownNodes);
  }

  @Override
  public CompletableFuture<Void> join(final BootstrapService bootstrap, final Node localNode) {
    if (running.compareAndSet(false, true)) {
      // 首轮扫描放在调度线程里执行，避免在调用方线程上做阻塞式 DNS 查询
      refreshExecutor.execute(
          () -> {
            runRefreshCycle();
            LOGGER.debug("Dynamic discovery started, {} nodes known", knownNodes.size());
          });
    }
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public CompletableFuture<Void> leave(final Node localNode) {
    if (running.compareAndSet(true, false)) {
      LOGGER.info("Stopping Dynamic discovery");
      cancelPendingRefresh();
      knownNodes.clear();
      refreshExecutor.shutdownNow();
      LOGGER.info("Dynamic discovery stopped");
    }
    return CompletableFuture.completedFuture(null);
  }

  /** 取消尚未执行的下一轮扫描。 */
  private void cancelPendingRefresh() {
    if (pendingRefresh != null) {
      pendingRefresh.cancel(false);
      pendingRefresh = null;
    }
  }

  /**
   * 单个扫描周期：先执行一次扫描，然后自排队下一轮。
   *
   * <p>采用"执行后再调度"而非固定速率，保证上一轮解析耗时较长时不会堆积扫描任务。
   */
  private void runRefreshCycle() {
    rescanEndpoints();
    final var period = config.getRefreshInterval();
    LOGGER.trace("Next node refresh scheduled in {}", period);
    pendingRefresh =
        refreshExecutor.schedule(
            this::runRefreshCycle, period.toMillis(), TimeUnit.MILLISECONDS);
  }

  /** 重新解析全部配置地址，并把节点集合的增删差异以事件形式广播出去。 */
  private void rescanEndpoints() {
    try {
      final var previousSnapshot = new HashSet<>(knownNodes);
      final var latestNodes = resolveAllEndpoints();
      applyJoins(latestNodes, previousSnapshot);
      applyLeaves(latestNodes, previousSnapshot);
    } catch (final Exception failure) {
      LOGGER.error("Error refreshing discovery nodes", failure);
    }
  }

  /** 对新增节点补录集合并广播 JOIN 事件。 */
  private void applyJoins(final Set<Node> latestNodes, final HashSet<Node> previousSnapshot) {
    final var joined = new ArrayList<>(latestNodes);
    joined.removeAll(previousSnapshot);
    for (final Node newcomer : joined) {
      knownNodes.add(newcomer);
      LOGGER.debug("Node joined: {}", newcomer);
      post(new NodeDiscoveryEvent(NodeDiscoveryEvent.Type.JOIN, newcomer));
    }
    if (!joined.isEmpty()) {
      LOGGER.info("{} nodes joined, {} total nodes", joined.size(), knownNodes.size());
    }
  }

  /** 对消失节点移出集合并广播 LEAVE 事件。 */
  private void applyLeaves(final Set<Node> latestNodes, final HashSet<Node> previousSnapshot) {
    final var left = new ArrayList<>(previousSnapshot);
    left.removeAll(latestNodes);
    for (final Node departed : left) {
      knownNodes.remove(departed);
      LOGGER.debug("Node left: {}", departed);
      post(new NodeDiscoveryEvent(NodeDiscoveryEvent.Type.LEAVE, departed));
    }
    if (!left.isEmpty()) {
      LOGGER.info("{} nodes left, {} total nodes", left.size(), knownNodes.size());
    }
  }

  /** 解析全部配置地址；单个地址失败只记警告，不影响其余地址。 */
  private Set<Node> resolveAllEndpoints() {
    final var resolved = ConcurrentHashMap.<Node>newKeySet();
    for (final String endpoint : config.getAddresses()) {
      try {
        resolved.addAll(resolveEndpoint(endpoint));
      } catch (final Exception failure) {
        LOGGER.warn("Failed to resolve address: {}", endpoint, failure);
      }
    }
    return resolved;
  }

  /**
   * 解析单个地址：主机名展开出的每个 IP 各生成一个节点。
   *
   * @param endpoint 形如 {@code "host:port"} 或 {@code "host"} 的地址
   * @return 该地址对应的节点集合，解析失败时为空集合
   */
  private Set<Node> resolveEndpoint(final String endpoint) {
    final var nodes = ConcurrentHashMap.<Node>newKeySet();
    try {
      final var hostPort = HostAndPort.fromString(endpoint).withDefaultPort(fallbackPort);
      final var ips = hostnameResolver.apply(hostPort.getHost());
      for (final InetAddress ip : ips) {
        nodes.add(toNode(ip, hostPort.getPort()));
        LOGGER.debug(
            "Resolved {} to {} ({}:{})", endpoint, ip, ip.getHostAddress(), hostPort.getPort());
      }
      if (nodes.isEmpty()) {
        LOGGER.warn("No addresses resolved for: {}", endpoint);
      }
    } catch (final Exception failure) {
      LOGGER.debug("Error resolving address: {}", endpoint, failure);
    }
    return nodes;
  }

  /** 由 IP 与端口构造节点，节点 ID 直接取 {@code ip:port} 文本。 */
  private static Node toNode(final InetAddress ip, final int port) {
    final var address = new Address(ip.getHostAddress(), port, ip);
    final var id = NodeId.from(ip.getHostAddress() + ":" + port);
    return new Node(new NodeConfig().setId(id).setAddress(address));
  }

  /** 系统默认解析：返回主机名对应的全部 IP，解析失败时返回空列表。 */
  private static List<InetAddress> lookupAllIps(final String host) {
    try {
      return Arrays.asList(InetAddress.getAllByName(host));
    } catch (final UnknownHostException failure) {
      LOGGER.warn("Failed to resolve DNS for address: {}", host, failure);
      return List.of();
    }
  }

  /** 校验刷新间隔必须为正值，否则启动即失败。 */
  private static void ensureValidRefreshInterval(final DynamicDiscoveryConfig config) {
    final var interval = config.getRefreshInterval();
    if (interval == null || interval.isZero() || interval.isNegative()) {
      throw new IllegalArgumentException(
          "Refresh interval must be a positive duration, but given: " + interval);
    }
  }

  /** 提供者类型元信息，名称 {@code "dynamic"}。 */
  public static class Type implements NodeDiscoveryProvider.Type<DynamicDiscoveryConfig> {
    private static final String NAME = "dynamic";

    @Override
    public String name() {
      return NAME;
    }

    @Override
    public NodeDiscoveryProvider newProvider(final DynamicDiscoveryConfig config) {
      return new DynamicDiscoveryProvider(config);
    }
  }
}
