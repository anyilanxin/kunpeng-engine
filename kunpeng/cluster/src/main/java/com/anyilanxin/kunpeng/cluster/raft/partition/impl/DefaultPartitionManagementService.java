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
package com.anyilanxin.kunpeng.cluster.primitive.partition.impl;

import com.anyilanxin.kunpeng.cluster.cluster.ClusterMembershipService;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.ClusterCommunicationService;
import com.anyilanxin.kunpeng.cluster.raft.partition.PartitionManagementService;

/** Default partition management service. */
public class DefaultPartitionManagementService implements PartitionManagementService {
  private final ClusterMembershipService membershipService;
  private final ClusterCommunicationService communicationService;

  public DefaultPartitionManagementService(
      final ClusterMembershipService membershipService,
      final ClusterCommunicationService communicationService) {
    this.membershipService = membershipService;
    this.communicationService = communicationService;
  }

  @Override
  public ClusterMembershipService getMembershipService() {
    return membershipService;
  }

  @Override
  public ClusterCommunicationService getMessagingService() {
    return communicationService;
  }
}
