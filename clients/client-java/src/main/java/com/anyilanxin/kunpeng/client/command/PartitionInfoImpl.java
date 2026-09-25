/*
 * Copyright © 2017 camunda services GmbH (info@camunda.com)
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
package com.anyilanxin.kunpeng.client.command;

import com.anyilanxin.kunpeng.client.util.EnumUtil;
import com.anyilanxin.kunpeng.gateway.grpc.service.ClusterManageServiceOuterClass;
import java.util.Objects;

public class PartitionInfoImpl implements PartitionInfo {

  private final int partitionId;
  private final PartitionBrokerRole role;
  private final PartitionBrokerHealth partitionBrokerHealth;

  public PartitionInfoImpl(final ClusterManageServiceOuterClass.Partition partition) {
    partitionId = partition.getPartitionId();

    if (partition.getRole()
        == ClusterManageServiceOuterClass.Partition.PartitionBrokerRole.LEADER) {
      role = PartitionBrokerRole.LEADER;
    } else if (partition.getRole()
        == ClusterManageServiceOuterClass.Partition.PartitionBrokerRole.FOLLOWER) {
      role = PartitionBrokerRole.FOLLOWER;
    } else if (partition.getRole()
        == ClusterManageServiceOuterClass.Partition.PartitionBrokerRole.INACTIVE) {
      role = PartitionBrokerRole.INACTIVE;
    } else {
      EnumUtil.logUnknownEnumValue(
          partition.getRole(), "partition broker role", PartitionBrokerRole.values());
      role = PartitionBrokerRole.UNKNOWN_ENUM_VALUE;
    }

    if (partition.getHealth()
        == ClusterManageServiceOuterClass.Partition.PartitionBrokerHealth.HEALTHY) {
      partitionBrokerHealth = PartitionBrokerHealth.HEALTHY;
    } else if (partition.getHealth()
        == ClusterManageServiceOuterClass.Partition.PartitionBrokerHealth.UNHEALTHY) {
      partitionBrokerHealth = PartitionBrokerHealth.UNHEALTHY;
    } else if (partition.getHealth()
        == ClusterManageServiceOuterClass.Partition.PartitionBrokerHealth.DEAD) {
      partitionBrokerHealth = PartitionBrokerHealth.DEAD;
    } else {
      EnumUtil.logUnknownEnumValue(
          partition.getHealth(), "partition broker health", PartitionBrokerHealth.values());
      partitionBrokerHealth = PartitionBrokerHealth.UNKNOWN_ENUM_VALUE;
    }
  }

  @Override
  public int getPartitionId() {
    return partitionId;
  }

  @Override
  public PartitionBrokerRole getRole() {
    return role;
  }

  @Override
  public boolean isLeader() {
    return role == PartitionBrokerRole.LEADER;
  }

  @Override
  public PartitionBrokerHealth getHealth() {
    return partitionBrokerHealth;
  }

  @Override
  public int hashCode() {
    return Objects.hash(partitionId, role, partitionBrokerHealth);
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    final PartitionInfoImpl that = (PartitionInfoImpl) o;
    return partitionId == that.partitionId
        && role == that.role
        && partitionBrokerHealth == that.partitionBrokerHealth;
  }

  @Override
  public String toString() {
    return "PartitionInfoImpl{"
        + "partitionId="
        + partitionId
        + ", role="
        + role
        + ", health="
        + partitionBrokerHealth
        + '}';
  }
}
