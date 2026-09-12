/*
 * Copyright 2018-present Open Networking Foundation
 * Copyright © 2020 camunda services GmbH (info@camunda.com)
 * Copyright © 2026 anyilanxin zxh (anyilanxin@aliyun.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.anyilanxin.kunpeng.cluster.cluster.discovery;

import static com.google.common.base.Preconditions.checkNotNull;

import com.anyilanxin.kunpeng.cluster.cluster.BootstrapService;
import com.anyilanxin.kunpeng.cluster.cluster.Node;
import com.anyilanxin.kunpeng.cluster.cluster.NodeConfig;
import com.anyilanxin.kunpeng.cluster.utils.event.AbstractListenerManager;
import com.google.common.collect.ImmutableSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cluster discovery provider that bootstraps the cluster from a pre-defined set of seed nodes.
 *
 * <p>The bootstrap discovery provider takes a set of peer {@link
 * BootstrapDiscoveryConfig#setNodes(Collection) addresses} and simply returns them from {@link
 * #getNodes()}. {@link #join(BootstrapService, Node)} and {@link #leave(Node)} are no-ops: this
 * provider does not exchange any messages with peers. Membership propagation and failure detection
 * are delegated to the configured group membership protocol (e.g. SWIM).
 */
public final class BootstrapDiscoveryProvider
    extends AbstractListenerManager<NodeDiscoveryEvent, NodeDiscoveryEventListener>
    implements NodeDiscoveryProvider {

  public static final Type TYPE = new Type();
  private static final Logger LOGGER = LoggerFactory.getLogger(BootstrapDiscoveryProvider.class);
  private final ImmutableSet<Node> bootstrapNodes;
  private final BootstrapDiscoveryConfig config;

  public BootstrapDiscoveryProvider(final Node... bootstrapNodes) {
    this(Arrays.asList(bootstrapNodes));
  }

  public BootstrapDiscoveryProvider(final Collection<Node> bootstrapNodes) {
    this(
        new BootstrapDiscoveryConfig()
            .setNodes(
                bootstrapNodes.stream()
                    .map(node -> new NodeConfig().setId(node.id()).setAddress(node.address()))
                    .collect(Collectors.toList())));
  }

  BootstrapDiscoveryProvider(final BootstrapDiscoveryConfig config) {
    this.config = checkNotNull(config);
    bootstrapNodes =
        ImmutableSet.copyOf(config.getNodes().stream().map(Node::new).collect(Collectors.toList()));
  }

  /**
   * Creates a new bootstrap discovery provider builder.
   *
   * @return a new bootstrap discovery provider builder
   */
  public static BootstrapDiscoveryBuilder builder() {
    return new BootstrapDiscoveryBuilder();
  }

  @Override
  public BootstrapDiscoveryConfig config() {
    return config;
  }

  @Override
  public Set<Node> getNodes() {
    return bootstrapNodes;
  }

  @Override
  public CompletableFuture<Void> join(final BootstrapService bootstrap, final Node localNode) {
    LOGGER.debug("Local node {} joined the bootstrap service", localNode);
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public CompletableFuture<Void> leave(final Node localNode) {
    LOGGER.debug("Local node {} left the bootstrap servide", localNode);
    return CompletableFuture.completedFuture(null);
  }

  /** Bootstrap discovery provider type. */
  public static class Type implements NodeDiscoveryProvider.Type<BootstrapDiscoveryConfig> {
    private static final String NAME = "bootstrap";

    @Override
    public String name() {
      return NAME;
    }

    @Override
    public NodeDiscoveryProvider newProvider(final BootstrapDiscoveryConfig config) {
      return new BootstrapDiscoveryProvider(config);
    }
  }
}
