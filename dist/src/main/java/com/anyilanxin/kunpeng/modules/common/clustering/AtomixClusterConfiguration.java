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
package com.anyilanxin.kunpeng.modules.common.clustering;

import com.anyilanxin.kunpeng.broker.client.admin.ClusterDispatchClient;
import com.anyilanxin.kunpeng.broker.client.admin.impl.DefaultClusterDispatchClient;
import com.anyilanxin.kunpeng.cluster.cluster.AtomixCluster;
import com.anyilanxin.kunpeng.cluster.cluster.ClusterConfig;
import com.anyilanxin.kunpeng.cluster.config.topology.cluster.ClusterTopologyService;
import com.anyilanxin.kunpeng.cluster.config.topology.cluster.DefaultClusterTopologyService;
import com.anyilanxin.kunpeng.cluster.utils.Version;
import com.anyilanxin.kunpeng.modules.common.endpoints.*;
import com.anyilanxin.kunpeng.scheduler.ActorSchedulingService;
import com.anyilanxin.kunpeng.utils.VersionUtil;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public final class AtomixClusterConfiguration {
  private final ClusterConfig config;

  @Autowired
  public AtomixClusterConfiguration(final ClusterConfig config) {
    this.config = config;
  }

  @Bean(destroyMethod = "stop")
  public AtomixCluster atomixCluster() {
    final SimpleMeterRegistry simpleMeterRegistry = new SimpleMeterRegistry();
    final var atomixCluster =
        new AtomixCluster(config, Version.from(VersionUtil.getVersion()), simpleMeterRegistry);
    atomixCluster.start().join();
    return atomixCluster;
  }

  @Bean(destroyMethod = "stop")
  public ClusterDispatchClient dispatchClient(
      final ActorSchedulingService actorSchedulingService, final AtomixCluster atomixCluster) {
    final DefaultClusterDispatchClient dispatchClient =
        new DefaultClusterDispatchClient(
            actorSchedulingService,
            atomixCluster.getLeaderFoundService(),
            atomixCluster.getMessagingService(),
            Duration.ofSeconds(30));
    dispatchClient.start();
    return dispatchClient;
  }

  @Bean(destroyMethod = "close")
  public DefaultClusterTopologyService clusterTopologyService(
      final ActorSchedulingService actorSchedulingService, final AtomixCluster atomixCluster) {
    final DefaultClusterTopologyService clusterTopologyService =
        new DefaultClusterTopologyService(atomixCluster.getMembershipService());
    actorSchedulingService.submitActor(clusterTopologyService);
    return clusterTopologyService;
  }

  @Bean
  public ClusterTopologyEndpoint clusterTopologyEndpoint(
      final ClusterTopologyService clusterTopologyService, final AtomixCluster atomixCluster) {
    return new ClusterTopologyEndpoint(
        clusterTopologyService, atomixCluster.getMembershipService());
  }

  @Bean
  public AdminDispatchEndpoint adminDispatchEndpoint(final ClusterDispatchClient dispatchClient) {
    return new AdminDispatchEndpoint(dispatchClient);
  }

  @Bean
  public BusinessDispatchEndpoint businessDispatchEndpoint(
      final ClusterDispatchClient dispatchClient) {
    return new BusinessDispatchEndpoint(dispatchClient);
  }

  @Bean
  public BusinessClusterBalanceEndpoint businessClusterBalanceEndpoint(
      final ClusterDispatchClient dispatchClient) {
    return new BusinessClusterBalanceEndpoint(dispatchClient);
  }

  @Bean
  public BusinessChangePartitionEndpoint businessChangePartitionEndpoint(
      final ClusterDispatchClient dispatchClient) {
    return new BusinessChangePartitionEndpoint(dispatchClient);
  }

  @Bean
  public BusinessChangeReplicationEndpoint businessChangeReplicationEndpoint(
      final ClusterDispatchClient dispatchClient) {
    return new BusinessChangeReplicationEndpoint(dispatchClient);
  }
}
