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

import java.util.concurrent.CompletableFuture;

/** Managed partition group. */
public interface ManagedPartitionGroup extends PartitionGroup {

  /**
   * Joins the partition group.
   *
   * @param managementService the partition management service
   * @return a future to be completed once the partition group has been joined
   */
  CompletableFuture<ManagedPartitionGroup> join(PartitionManagementService managementService);

  /**
   * Connects to the partition group.
   *
   * @param managementService the partition management service
   * @return a future to be completed once the partition group has been connected
   */
  CompletableFuture<ManagedPartitionGroup> connect(PartitionManagementService managementService);

  /**
   * Closes the partition group.
   *
   * @return a future to be completed once the partition group has been closed
   */
  CompletableFuture<Void> close();
}
