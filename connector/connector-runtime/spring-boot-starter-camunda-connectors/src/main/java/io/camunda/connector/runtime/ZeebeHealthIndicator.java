/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * Copyright © 2026 anyilanxin zxh (anyilanxin@aliyun.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.camunda.connector.runtime;

import com.anyilanxin.kunpeng.client.KunpengClient;
import com.anyilanxin.kunpeng.client.command.BrokerInfo;
import com.anyilanxin.kunpeng.client.command.PartitionBrokerHealth;
import com.anyilanxin.kunpeng.client.command.PartitionInfo;
import com.anyilanxin.kunpeng.client.command.topology.TopologyCommandResponse;
import java.util.Collection;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.health.contributor.AbstractHealthIndicator;
import org.springframework.boot.health.contributor.Health.Builder;

public class ZeebeHealthIndicator extends AbstractHealthIndicator {

  private static final Logger LOG = LoggerFactory.getLogger(ZeebeHealthIndicator.class);

  private final KunpengClient client;

  public ZeebeHealthIndicator(KunpengClient client) {
    this.client = client;
  }

  @Override
  protected void doHealthCheck(Builder builder) {
    final TopologyCommandResponse topology;

    try {
      topology = client.newTopologyCommand().send().join();
    } catch (Exception e) {
      LOG.warn("Kunpeng health check failed: could not retrieve topology", e);
      builder.down(e);
      return;
    }
    var numBrokers = topology.getBrokers().size();
    boolean anyPartitionHealthy =
        topology.getBrokers().stream()
            .map(BrokerInfo::getPartitions)
            .flatMap(Collection::stream)
            .map(PartitionInfo::getHealth)
            .anyMatch(health -> health == PartitionBrokerHealth.HEALTHY);
    var details = Map.of("numBrokers", numBrokers, "anyPartitionHealthy", anyPartitionHealthy);
    if (numBrokers > 0 && anyPartitionHealthy) {
      builder.up().withDetails(details);
    } else {
      LOG.warn(
          "Kunpeng health check failed: numBrokers={}, anyPartitionHealthy={}",
          numBrokers,
          anyPartitionHealthy);
      builder.down().withDetails(details);
    }
  }
}
