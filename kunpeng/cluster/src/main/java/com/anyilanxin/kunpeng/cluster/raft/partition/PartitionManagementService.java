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
package com.anyilanxin.kunpeng.cluster.raft.partition;

import com.anyilanxin.kunpeng.cluster.cluster.ClusterMembershipService;
import com.anyilanxin.kunpeng.cluster.cluster.messaging.ClusterCommunicationService;

/** Partition management service. */
public interface PartitionManagementService {

  /**
   * Returns the cluster service.
   *
   * @return the cluster service
   */
  ClusterMembershipService getMembershipService();

  /**
   * Returns the cluster messaging service.
   *
   * @return the cluster messaging service
   */
  ClusterCommunicationService getMessagingService();
}
